package vcmsa.projects.district49_android

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vcmsa.projects.district49_android.models.NewsAnnouncement
import vcmsa.projects.district49_android.utils.AuthManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Homepage : AppCompatActivity() {

    private lateinit var newsletterManager: NewsletterManager
    private lateinit var adminRepository: AdminRepository
    private var newsletterPopupShown = false

    private lateinit var btnProfile: ImageButton

    // Progress bar views (inside progressContainer in XML)
    private lateinit var progressLine: View
    private lateinit var tvProgressLabel: TextView

    // Firebase
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    companion object {
        private const val TAG = "Homepage"
    }

    /** Generic click animation that works for Button/MaterialButton/ImageButton/etc. */
    private fun addClickAnimation(view: View, onClick: () -> Unit = {}) {
        view.setOnClickListener {
            view.animate()
                .scaleX(0.93f)
                .scaleY(0.93f)
                .setDuration(70)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(70)
                        .withEndAction { onClick() }
                        .start()
                }
                .start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.homepage)

        findViewById<View?>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        newsletterManager = NewsletterManager.getInstance()
        adminRepository = AdminRepository()

        // Progress views
        progressLine = findViewById(R.id.progressLine)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)

        // --- Admin/Profile button (Admin Dashboard) ---
        btnProfile = findViewById(R.id.btnProfile)
        btnProfile.setOnClickListener {
            runCatching { startActivity(Intent(this, AdminDashboard::class.java)) }
                .onFailure { Toast.makeText(this, "Admin dashboard not available", Toast.LENGTH_SHORT).show() }
        }
        btnProfile.visibility = View.GONE

        // --- Wire main image buttons to existing nav methods ---
        findViewById<View?>(R.id.btnPrinciples)?.let { addClickAnimation(it) { navigateToPrinciples() } }
        findViewById<View?>(R.id.btnEvents)?.let { addClickAnimation(it) { navigateToEventsPage() } }

        // Make the pink rectangle clickable to open donation page
        findViewById<View?>(R.id.imgPinkRect)?.let { addClickAnimation(it) { navigateToDonation() } }

        // NEWS BUTTON - Show news popup
        findViewById<View?>(R.id.btnNews)?.setOnClickListener {
            showNewsPopup()
        }

        // LOGOUT BUTTON
        findViewById<View?>(R.id.btnLogout)?.setOnClickListener {
            showLogoutConfirmation()
        }

        animateMenuButtonsSequentially()
        setupNewsletter()
        loadLatestDonationGoal()
        wireOptionalNavBar()

        lifecycleScope.launch {
            val isAdmin = fetchIsAdmin()
            btnProfile.visibility = if (isAdmin) View.VISIBLE else View.GONE
            if (isAdmin) Toast.makeText(this@Homepage, "Admin mode enabled", Toast.LENGTH_SHORT).show()
        }
    }

    //  Show logout confirmation dialog
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ -> performLogout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //  Perform logout
    private fun performLogout() {
        try {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginPage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Logout failed", e)
            Toast.makeText(this, "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    //  Show news announcements popup
    private fun showNewsPopup() {
        lifecycleScope.launch {
            try {
                val newsList = withContext(Dispatchers.IO) {
                    db.collection("news_announcements")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(20)
                        .get()
                        .await()
                        .documents
                        .mapNotNull { it.toObject(NewsAnnouncement::class.java) }
                        .filter { it.isActive }
                }

                if (newsList.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@Homepage, "No news available", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                runOnUiThread { showNewsDialog(newsList) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading news", e)
                runOnUiThread {
                    Toast.makeText(this@Homepage, "Failed to load news: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //  Show news in dialog with RecyclerView
    private fun showNewsDialog(newsList: List<NewsAnnouncement>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_news_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.newsRecyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = NewsAdapter(newsList)

        AlertDialog.Builder(this)
            .setTitle("News & Announcements")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
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

    /** Make navbar buttons safe regardless of actual view class in navbar layout. */
    private fun wireOptionalNavBar() {
        findViewById<View?>(R.id.nav_btn_arrow_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        findViewById<View?>(R.id.nav_btn_notifications)?.setOnClickListener {
            runCatching {
                startActivity(Intent(this, NotificationSettingsActivity::class.java))
            }.onFailure {
                Toast.makeText(this, "Notification settings not available", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<View?>(R.id.nav_btn_profile)?.setOnClickListener {
            runCatching { startActivity(Intent(this, ProfileActivity::class.java)) }
                .onFailure { Toast.makeText(this, "Profile screen not available", Toast.LENGTH_SHORT).show() }
        }
        findViewById<View?>(R.id.nav_btn_home)?.setOnClickListener {
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
        }
        findViewById<View?>(R.id.nav_btn_plus_circle)?.setOnClickListener {
            runCatching { startActivity(Intent(this, MenuActivity::class.java)) }
                .onFailure { Toast.makeText(this, "Menu screen not available", Toast.LENGTH_SHORT).show() }
        }
    }

    private fun loadLatestDonationGoal() {
        lifecycleScope.launch {
            try {
                val result = adminRepository.getCurrentDonationGoal()
                result.getOrNull()?.let { updateDonationDisplay(it) }
            } catch (_: Exception) { }
        }
    }

    /**
     * Robustly updates the percent-width progress fill.
     * - Clamps and sanitizes values (handles 0/negative/NaN/∞).
     * - Ensures the bar fills from the LEFT by forcing horizontalBias = 0.
     * - Because progressLine is inside progressContainer (which matches imgPinkRect), the percent is scoped correctly.
     */
    private fun updateDonationDisplay(goal: vcmsa.projects.district49_android.models.DonationGoal) {
        val raised = goal.raisedAmount.toDouble()
        val goalAmt = goal.goalAmount.toDouble()

        val safeRaised = if (raised.isFinite() && raised >= 0.0) raised else 0.0
        val safeGoal = if (goalAmt.isFinite() && goalAmt > 0.0) goalAmt else 0.0

        val ratio = if (safeGoal > 0.0) safeRaised / safeGoal else 0.0
        val clamped = ratio.coerceIn(0.0, 1.0).toFloat()

        val lp = progressLine.layoutParams
        if (lp is androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) {
            lp.matchConstraintPercentWidth = clamped   // now relative to progressContainer width
            lp.horizontalBias = 0f                      // fill from left
            progressLine.layoutParams = lp
        } else {
            // Fallback: visual approximation if unexpected LayoutParams type
            progressLine.pivotX = 0f
            progressLine.scaleX = clamped.coerceAtLeast(0.0001f)
        }

        tvProgressLabel.text = "Progress bar"
    }

    /**
     * Animate the main menu buttons.
     * Note: btnProfile is the admin button and is excluded from animation since it's conditionally visible.
     */
    private fun animateMenuButtonsSequentially() {
        val buttonIds = listOf(
            R.id.btnNews,
            R.id.btnPrinciples,
            R.id.btnEvents
        )

        val handler = Handler(Looper.getMainLooper())
        val delayIncrement = 140L

        buttonIds.forEachIndexed { index, id ->
            val button = findViewById<View?>(id) ?: return@forEachIndexed

            button.animate().cancel()
            button.alpha = 0f
            button.translationY = 60f
            button.scaleX = 0.92f
            button.scaleY = 0.92f
            button.visibility = View.VISIBLE

            handler.postDelayed({
                button.animate()
                    .translationY(0f)
                    .scaleX(1f).scaleY(1f)
                    .alpha(1f)
                    .setDuration(300)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }, delayIncrement * index)
        }
    }

    private fun navigateToDonation() =
        runCatching { startActivity(Intent(this, DonateOne::class.java)) }
            .onFailure { Toast.makeText(this, "Donate screen not available", Toast.LENGTH_SHORT).show() }

    private fun navigateToPrinciples() =
        runCatching { startActivity(Intent(this, PrinciplesPage::class.java)) }
            .onFailure { Toast.makeText(this, "Principles page not available", Toast.LENGTH_SHORT).show() }

    private fun navigateToEventsPage() =
        runCatching { startActivity(Intent(this, EventsPage::class.java)) }
            .onFailure { Toast.makeText(this, "Events page not available", Toast.LENGTH_SHORT).show() }

    private fun setupNewsletter() {
        Handler(Looper.getMainLooper()).postDelayed({ showNewsletterPopupIfNeeded() }, 3000)
    }

    private fun showNewsletterPopupIfNeeded() {
        if (newsletterPopupShown) return
        lifecycleScope.launch {
            try {
                val auth = AuthManager.getInstance()
                val user = auth.getCurrentFirebaseUser()
                val shouldShow = if (user != null) {
                    val result = newsletterManager.isCurrentUserSubscribed(user.uid)
                    result.exceptionOrNull() != null || !result.getOrDefault(false)
                } else true

                if (shouldShow) showNewsletterPopup()
                newsletterPopupShown = true
            } catch (_: Exception) {
                newsletterPopupShown = true
            }
        }
    }

    private fun showNewsletterPopup() {
        runCatching {
            val view = layoutInflater.inflate(R.layout.newsletter, null)
            val emailEditText = view.findViewById<EditText?>(R.id.etEmail)
            val nameEditText = view.findViewById<EditText?>(R.id.etName)
            val signupButton = view.findViewById<View?>(R.id.btnSignUpImage)

            val dialog = android.app.AlertDialog.Builder(this)
                .setView(view)
                .setCancelable(true)
                .create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            signupButton?.setOnClickListener {
                val email = emailEditText?.text?.toString()?.trim().orEmpty()
                val name = nameEditText?.text?.toString()?.trim().orEmpty()
                if (!validateNewsletterInput(email, name)) return@setOnClickListener

                lifecycleScope.launch {
                    runCatching {
                        val user = AuthManager.getInstance().getCurrentFirebaseUser()
                        if (user != null) {
                            val result = newsletterManager.subscribeToNewsletter(email, name, user.uid)
                            runOnUiThread {
                                if (result.isSuccess) {
                                    Toast.makeText(this@Homepage, "Subscribed!", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()
                                } else {
                                    Toast.makeText(this@Homepage, "Subscription failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@Homepage, "No user ID", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.onFailure {
                        runOnUiThread {
                            Toast.makeText(this@Homepage, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            dialog.show()
        }.onFailure {
            Toast.makeText(this, "Failed to show newsletter popup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateNewsletterInput(email: String, name: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(this, "Email required", Toast.LENGTH_SHORT).show()
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show()
            return false
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "Name required", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        // Reload donation data when returning to homepage
        loadLatestDonationGoal()
    }
}

//  RecyclerView Adapter for News
class NewsAdapter(private val newsList: List<vcmsa.projects.district49_android.models.NewsAnnouncement>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.newsTitle)
        val contentText: TextView = view.findViewById(R.id.newsContent)
        val dateText: TextView = view.findViewById(R.id.newsDate)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): NewsViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]
        holder.titleText.text = news.title
        holder.contentText.text = news.content

        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        holder.dateText.text = dateFormat.format(Date(news.createdAt))
    }

    override fun getItemCount() = newsList.size
}
