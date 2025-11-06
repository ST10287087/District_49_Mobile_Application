package vcmsa.projects.district49_android

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.LruCache
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

class MeetTheTeam : AppCompatActivity() {

    // ---------- Data ----------
    data class TeamMember(
        val docId: String,
        val name: String,
        val role: String?,
        val imagePath: String?,   // "meet_the_team/uuid.jpg"
        val photoUrl: String?,    // https download URL
        val order: Long?
    )

    // ---------- Firebase ----------
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance("gs://district49-db.firebasestorage.app") }

    // ---------- Views ----------
    private lateinit var root: ConstraintLayout
    private lateinit var orbitCenter: View

    private val orbitItemViews = arrayListOf<View>()
    private val members = mutableListOf<TeamMember>()

    // ---------- Orbit/anim ----------
    private var offsetAngle = 0f
    private var dragJob: Job? = null

    private val SPOTLIGHT_CENTER_DEG = 0f
    private val SPOTLIGHT_HALF_WIDTH_DEG = 60f
    private val MIN_SCALE = 0.82f
    private val MAX_SCALE = 1.00f
    private val MIN_ALPHA = 0.70f
    private val MAX_ALPHA = 1.00f
    private val Z_ELEVATION_RANGE = 120f
    private val DRAG_TO_ANGLE_FACTOR = 0.35f
    private val SNAP_BASE_MS = 180L
    private val SNAP_MS_PER_DEG = 10L
    private val SNAP_MAX_MS = 1000L

    // We scale the container (photoCard), not the ImageView
    private val BASE_PHOTO_SCALE = 1.4f

    // ---------- State ----------
    private var isAdmin = false
    private var hasInitializedOrbit = false
    private val uiScope = CoroutineScope(Dispatchers.Main)

    // ---------- Simple in-memory image cache ----------
    companion object {
        private const val TAG = "MeetTheTeam"
        // ~8MB cache (adjust as needed)
        private val imageCache = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount
            }
        }
    }

    // ---------- AddMember launcher ----------
    private val addMemberLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val name = result.data?.getStringExtra("name")?.trim().orEmpty()
                val uriStr = result.data?.getStringExtra("photoUri")
                val photoUri = uriStr?.let { Uri.parse(it) }

                if (name.isEmpty()) {
                    Toast.makeText(this, "Name missing", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }

                uiScope.launch {
                    val ok = addMemberToFirebase(name, photoUri)
                    Toast.makeText(this@MeetTheTeam, if (ok) "Member added" else "Failed to add member", Toast.LENGTH_SHORT).show()
                    if (ok) loadTeamMembersAndBuild()
                }
            }
        }

    // ---------- Lifecycle ----------
    private lateinit var bottomNav: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.meet_the_team)

        root = findViewById(R.id.clMeetTheTeam)
        orbitCenter = findViewById(R.id.orbitCenter)
        bottomNav = findViewById(R.id.include_bottom_nav)

        // 🔹 Always show navbar on top
        bottomNav.bringToFront()
        bottomNav.elevation = 24f
        bottomNav.translationZ = 1000f

        setupDragRotate()
        wireOptionalNavBar()

        uiScope.launch {
            isAdmin = fetchIsAdmin()
            loadTeamMembersAndBuild()
        }
    }

    private fun wireOptionalNavBar() {
        // Back
        findViewById<ImageButton?>(R.id.nav_btn_arrow_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Menu
        findViewById<ImageButton?>(R.id.nav_btn_plus_circle)?.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        // Notifications
        findViewById<ImageButton?>(R.id.nav_btn_notifications)?.setOnClickListener {
            val intent = Intent(this, NotificationSettingsActivity::class.java)
            startActivity(intent)
        }

        // Profile
        findViewById<ImageButton?>(R.id.nav_btn_profile)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Home
        findViewById<ImageButton?>(R.id.nav_btn_home)?.setOnClickListener {
            val intent = Intent(this, Homepage::class.java)
            startActivity(intent)
        }
    }

    // ---------- Admin detection ----------
    private suspend fun fetchIsAdmin(): Boolean = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid ?: return@withContext false
        return@withContext try {
            val doc = db.collection("users").document(uid).get().await()
            val role = doc.getString("userRole") ?: ""
            val adminBool = doc.getBoolean("admin") ?: false
            role.equals("admin", ignoreCase = true) || adminBool
        } catch (e: Exception) {
            Log.w(TAG, "fetchIsAdmin failed", e)
            false
        }
    }

    // ---------- Firestore add ----------
    private suspend fun addMemberToFirebase(name: String, localImage: Uri?): Boolean = withContext(Dispatchers.IO) {
        try {
            val nextOrder = try {
                val q = db.collection("meet_the_team")
                    .orderBy("order", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .await()
                val top = q.documents.firstOrNull()?.getLong("order") ?: 0L
                top + 1L
            } catch (_: Exception) {
                (members.maxOfOrNull { it.order ?: 0L } ?: 0L) + 1L
            }

            var imagePath: String? = null
            var httpsUrl: String? = null

            if (localImage != null) {
                val filename = "${UUID.randomUUID()}.jpg"
                imagePath = "meet_the_team/$filename"
                val ref = storage.reference.child(imagePath)
                contentResolver.openInputStream(localImage).use { stream ->
                    requireNotNull(stream) { "Cannot open image stream" }
                    ref.putStream(stream).await()
                }
                httpsUrl = ref.downloadUrl.await().toString()
            }

            val payload = hashMapOf(
                "name" to name,
                "order" to nextOrder,
                "imagePath" to imagePath,
                "photoUrl" to httpsUrl,
                "createdAt" to FieldValue.serverTimestamp()
            )

            db.collection("meet_the_team").document().set(payload).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "addMemberToFirebase failed", e)
            false
        }
    }

    // ---------- Firestore load ----------
    private suspend fun loadTeamMembers(): List<TeamMember> = withContext(Dispatchers.IO) {
        val col = db.collection("meet_the_team")
        val out = mutableListOf<TeamMember>()
        try {
            val snap = col.orderBy("order", Query.Direction.ASCENDING)
                .orderBy("name", Query.Direction.ASCENDING)
                .get()
                .await()
            for (d in snap.documents) {
                out += TeamMember(
                    docId = d.id,
                    name = d.getString("name") ?: "(no name)",
                    role = d.getString("role"),
                    imagePath = d.getString("imagePath"),
                    photoUrl = d.getString("photoUrl"),
                    order = d.getLong("order")
                )
            }
        } catch (e1: Exception) {
            Log.w(TAG, "Primary query failed, trying fallback 1", e1)
            try {
                val s2 = col.orderBy("name", Query.Direction.ASCENDING).get().await()
                for (d in s2.documents) {
                    out += TeamMember(
                        docId = d.id,
                        name = d.getString("name") ?: "(no name)",
                        role = d.getString("role"),
                        imagePath = d.getString("imagePath"),
                        photoUrl = d.getString("photoUrl"),
                        order = d.getLong("order")
                    )
                }
            } catch (e2: Exception) {
                Log.w(TAG, "Fallback 1 failed, trying fallback 2", e2)
                val s3 = col.get().await()
                for (d in s3.documents) {
                    out += TeamMember(
                        docId = d.id,
                        name = d.getString("name") ?: "(no name)",
                        role = d.getString("role"),
                        imagePath = d.getString("imagePath"),
                        photoUrl = d.getString("photoUrl"),
                        order = d.getLong("order")
                    )
                }
            }
        }
        return@withContext out
    }

    private suspend fun loadTeamMembersAndBuild() {
        members.clear()
        members += loadTeamMembers()
        rebuildOrbitViews()
    }

    // ---------- Build orbit ----------
    private fun rebuildOrbitViews() {
        orbitItemViews.forEach { root.removeView(it) }
        orbitItemViews.clear()

        val baseAngles = computeBaseAngles(members.size)

        // initial lock to first
        if (!hasInitializedOrbit && baseAngles.isNotEmpty()) {
            offsetAngle = normalizeAngle(SPOTLIGHT_CENTER_DEG - baseAngles[0])
            hasInitializedOrbit = true
        }

        members.forEachIndexed { idx, m ->
            val item = layoutInflater.inflate(R.layout.orbit_item, root, false)
            val tvName = item.findViewById<TextView>(R.id.tvName)
            val tvRole = item.findViewById<TextView>(R.id.tvRole)
            val ivPhoto = item.findViewById<ImageView>(R.id.ivPhoto)
            val photoCard = item.findViewById<View>(R.id.photoCard)
            val adminBar = item.findViewById<LinearLayout>(R.id.adminBar)
            val btnAdd = item.findViewById<ImageButton>(R.id.btnAdd)
            val btnRemove = item.findViewById<ImageButton>(R.id.btnRemove)

            // rounded container clips; ImageView uses fitCenter (no crop)
            photoCard.outlineProvider = ViewOutlineProvider.BACKGROUND
            photoCard.clipToOutline = true

            // labels
            tvName.text = m.name
            val roleTxt = m.role?.trim().orEmpty()
            if (roleTxt.isEmpty()) {
                tvRole.visibility = View.GONE
            } else {
                tvRole.text = roleTxt
                tvRole.visibility = View.VISIBLE
            }

            // admin UI
            adminBar.visibility = if (isAdmin) View.VISIBLE else View.GONE
            if (isAdmin) {
                btnAdd.setOnClickListener {
                    val intent = Intent(this, AddMember::class.java)
                    addMemberLauncher.launch(intent)
                }
                btnRemove.setOnClickListener {
                    uiScope.launch {
                        val ok = deleteMember(m)
                        Toast.makeText(this@MeetTheTeam, if (ok) "Member removed" else "Failed to remove member", Toast.LENGTH_SHORT).show()
                        if (ok) loadTeamMembersAndBuild()
                    }
                }
            }

            // set placeholder BEFORE async load so it's never blank
            ivPhoto.setImageResource(R.drawable.meettheteam_person)

            // async image load
            uiScope.launch {
                val bmp = resolveBitmapForMember(m)
                if (bmp != null) ivPhoto.setImageBitmap(bmp)
                else {
                    // keep placeholder and log
                    Log.w(TAG, "Image load failed for member=${m.name} url=${m.photoUrl ?: m.imagePath}")
                }
            }

            // place on orbit
            val lp = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            lp.circleConstraint = orbitCenter.id
            lp.circleRadius = dp(140f)
            lp.circleAngle = normalizeAngle(baseAngles[idx] + offsetAngle)
            item.layoutParams = lp

            root.addView(item)
            orbitItemViews += item
        }

        updateOrbit()
    }

    private suspend fun deleteMember(m: TeamMember): Boolean = withContext(Dispatchers.IO) {
        try {
            m.imagePath?.let { p ->
                if (p.startsWith("meet_the_team/")) {
                    try { storage.reference.child(p).delete().await() } catch (_: Exception) {}
                }
            }
            db.collection("meet_the_team").document(m.docId).delete().await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteMember failed", e)
            false
        }
    }

    // ---------- Orbit visuals ----------
    private fun updateOrbit() {
        val baseAngles = computeBaseAngles(members.size)
        orbitItemViews.forEachIndexed { idx, item ->
            val lp = item.layoutParams as ConstraintLayout.LayoutParams
            val angle = normalizeAngle(baseAngles[idx] + offsetAngle)
            lp.circleAngle = angle
            item.layoutParams = lp

            val photoCard = item.findViewById<View>(R.id.photoCard)
            val dist = angularDistanceDeg(angle, SPOTLIGHT_CENTER_DEG)
            val w = spotlightWeight(dist, SPOTLIGHT_HALF_WIDTH_DEG)

            val scale = lerp(MIN_SCALE, MAX_SCALE, w) * BASE_PHOTO_SCALE
            val alpha = lerp(MIN_ALPHA, MAX_ALPHA, w)
            val z = w * Z_ELEVATION_RANGE

            photoCard.scaleX = scale
            photoCard.scaleY = scale
            photoCard.alpha = alpha
            photoCard.translationZ = z

            item.findViewById<TextView>(R.id.tvName).translationZ = z + 1f
            item.findViewById<TextView>(R.id.tvRole).translationZ = z + 1f
        }
    }

    private fun computeBaseAngles(n: Int): List<Float> {
        if (n <= 0) return emptyList()
        val start = 270f
        val step = 360f / n
        return List(n) { i -> start + i * step }
    }

    // ---------- Touch / snap ----------
    private fun setupDragRotate() {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var downX = 0f
        var lastX = 0f
        var dragging = false

        root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.x
                    lastX = event.x
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastX
                    if (!dragging && abs(event.x - downX) > touchSlop) dragging = true
                    if (dragging) {
                        rotateOrbit(dx * DRAG_TO_ANGLE_FACTOR)
                        lastX = event.x
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    snapToNearestMember()
                    dragging = false
                    true
                }
                else -> false
            }
        }
    }

    private fun rotateOrbit(delta: Float) {
        offsetAngle = normalizeAngle(offsetAngle + delta)
        updateOrbit()
    }

    private fun snapToNearestMember() {
        if (members.isEmpty()) return
        val baseAngles = computeBaseAngles(members.size)

        var bestAngle = 0f
        var bestDist = Float.MAX_VALUE
        for (a0 in baseAngles) {
            val a = normalizeAngle(a0 + offsetAngle)
            val d = angularDistanceDeg(a, SPOTLIGHT_CENTER_DEG)
            if (d < bestDist) {
                bestDist = d
                bestAngle = a
            }
        }

        val delta = shortestAngularDelta(bestAngle, SPOTLIGHT_CENTER_DEG)
        if (abs(delta) < 0.01f) return

        val duration = (SNAP_BASE_MS + (abs(delta) * SNAP_MS_PER_DEG)).toLong().coerceAtMost(SNAP_MAX_MS)
        val start = offsetAngle

        val animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { a ->
                val t = a.animatedFraction
                offsetAngle = normalizeAngle(start + delta * t)
                updateOrbit()
            }
        }
        animator.start()
    }

    // ---------- Robust image loader with cache & sampling ----------
    private suspend fun resolveBitmapForMember(m: TeamMember): Bitmap? = withContext(Dispatchers.IO) {
        // Prefer explicit URL (photoUrl). Else try storage path -> URL.
        val url = when {
            !m.photoUrl.isNullOrBlank() -> m.photoUrl!!
            !m.imagePath.isNullOrBlank() -> {
                try {
                    val normalized = if (m.imagePath!!.startsWith("meet_the_team/")) m.imagePath!!
                    else "meet_the_team/${m.imagePath}"
                    val ref = storage.reference.child(normalized)
                    ref.downloadUrl.await().toString()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get downloadUrl for ${m.imagePath}", e)
                    null
                }
            }
            else -> null
        } ?: return@withContext null

        // Cache hit?
        imageCache.get(url)?.let { return@withContext it }

        // Target size to decode to (container is 130x160dp, scaled up to ~1.4x)
        val targetW = (dp(130f) * BASE_PHOTO_SCALE).toInt()
        val targetH = (dp(160f) * BASE_PHOTO_SCALE).toInt()

        return@withContext try {
            val bytes = httpGetBytes(url) ?: run {
                Log.w(TAG, "httpGet returned null for $url")
                null
            }

            if (bytes == null) null else decodeSampledBitmap(bytes, targetW, targetH)?.also {
                imageCache.put(url, it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "resolveBitmapForMember decode failed", e)
            null
        }
    }

    private fun httpGetBytes(urlStr: String): ByteArray? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                instanceFollowRedirects = true
                useCaches = true
                setRequestProperty("User-Agent", "MeetTheTeam/1.0")
                doInput = true
            }
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) {
                Log.w(TAG, "HTTP $code for $urlStr")
                return null
            }
            BufferedInputStream(conn.inputStream).use { input ->
                val buffer = ByteArray(8 * 1024)
                val out = ByteArrayOutputStream()
                while (true) {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    out.write(buffer, 0, n)
                }
                out.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "httpGetBytes failed for $urlStr", e)
            null
        } finally {
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray, reqW: Int, reqH: Int): Bitmap? {
        // 1. Bounds
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

        // 2. Sample factor
        opts.inSampleSize = calculateInSampleSize(opts, reqW, reqH)

        // 3. Decode
        opts.inJustDecodeBounds = false
        // Memory-friendly config
        opts.inPreferredConfig = Bitmap.Config.RGB_565
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // ---------- Math/Utils ----------
    private fun normalizeAngle(a: Float): Float {
        var x = a % 360f
        if (x < 0f) x += 360f
        return x
    }

    private fun shortestAngularDelta(from: Float, to: Float): Float {
        var d = (to - from) % 360f
        if (d > 180f) d -= 360f
        if (d <= -180f) d += 360f
        return d
    }

    private fun angularDistanceDeg(a: Float, b: Float): Float {
        var d = (a - b) % 360f
        if (d < -180f) d += 360f
        if (d > 180f) d -= 360f
        return abs(d)
    }

    private fun spotlightWeight(distDeg: Float, halfWidthDeg: Float): Float {
        if (halfWidthDeg <= 0f) return if (distDeg == 0f) 1f else 0f
        val t = min(1f, distDeg / halfWidthDeg)
        val w = cos(t * (PI.toFloat() / 2f))
        return w.coerceIn(0f, 1f)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()
}
