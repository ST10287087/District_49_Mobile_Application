package vcmsa.projects.district49_android

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import vcmsa.projects.district49_android.ui.nav.NavBarBinder

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // --- force navbar overlay on top of everything ---
        val root = findViewById<View>(R.id.main)
        val nav = findViewById<View>(R.id.include_bottom_nav)
//        root.clipToPadding = false
        root.clipToOutline = false
        nav.elevation = 100f
        nav.translationZ = 100f
        nav.bringToFront()
        (nav.parent as View).invalidate()
        // --------------------------------------------------

        // FIXED: Changed bottom padding from 0 to systemBars.bottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Include bottom padding to respect system navigation bar
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Bind navbar with proper navigation
        NavBarBinder.bind(
            findViewById(R.id.include_bottom_nav),
            NavBarBinder.Actions(
                onBack = { finish() },
                onPlus = { /* already here */ },
                onNotifications = {
                    startActivity(Intent(this, NotificationSettingsActivity::class.java))
                },
                onProfile = { isLoggedIn ->
                    if (isLoggedIn) {
                        startActivity(Intent(this, ProfileActivity::class.java))
                    } else {
                        startActivity(Intent(this, LoginPage::class.java))
                    }
                },
                onHome = {
                    startActivity(
                        Intent(this, Homepage::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    )
                    finish()
                }
            )
        )

        // Clouds
        animateCloud(findViewById(R.id.imgCloudLeft), 20f, 500f, 12000L)
        animateCloud(findViewById(R.id.imgCloudRight), -40f, 350f, 10000L)

        // Pole then buttons
        val pole = findViewById<ImageView>(R.id.imgPole)
        pole.post {
            val startY = pole.translationY + 500f
            pole.translationY = startY
            pole.alpha = 0f
            pole.animate().translationYBy(-500f).alpha(1f).setDuration(900).withEndAction {
                animateMenuButtonsSequentially()
            }.start()
        }
    }

    private fun animateCloud(cloud: ImageView, fromX: Float, toX: Float, duration: Long) {
        cloud.translationX = fromX
        ObjectAnimator.ofFloat(cloud, View.TRANSLATION_X, fromX, toX).apply {
            this.duration = duration
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }.start()
    }

    private fun addClickAnimation(button: ImageButton, onClickAction: (() -> Unit)? = null) {
        button.setOnClickListener {
            button.animate().scaleX(0.93f).scaleY(0.93f).setDuration(70).withEndAction {
                button.animate().scaleX(1f).scaleY(1f).setDuration(70).withEndAction {
                    onClickAction?.invoke()
                }.start()
            }.start()
        }
    }

    private fun animateMenuButtonsSequentially() {
        val ids = listOf(
            R.id.btnAbout,
            R.id.btnDonate,
            R.id.btnVolunteer,
            R.id.btnContact,
            R.id.btnTeam,
            R.id.btnSuccess,
            R.id.btnGallery,
            R.id.btnEvents
        )
        val handler = Handler(Looper.getMainLooper())
        var delay = 0L
        ids.forEach { id ->
            val button = findViewById<ImageButton>(id) ?: return@forEach
            button.animate().cancel()
            button.alpha = 0f
            button.translationX = button.translationX - 600f
            button.visibility = View.VISIBLE
            handler.postDelayed({
                button.animate().translationXBy(600f).alpha(1f).setDuration(320)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
                when (id) {
                    R.id.btnDonate -> addClickAnimation(button) {
                        startActivity(Intent(this@MenuActivity, DonateOne::class.java))
                    }
                    R.id.btnContact -> addClickAnimation(button) {
                        startActivity(Intent(this@MenuActivity, ContactUsActivity::class.java))
                    }
                    R.id.btnTeam -> addClickAnimation(button) {
                        startActivity(Intent(this@MenuActivity, MeetTheTeam::class.java))
                    }
                    R.id.btnGallery -> addClickAnimation(button) {
                        startActivity(Intent(this@MenuActivity, GalleryActivity::class.java))
                    }
                    R.id.btnSuccess -> addClickAnimation(button) {
                        startActivity(Intent(this@MenuActivity, SuccessStories::class.java))
                    }
                    R.id.btnEvents -> addClickAnimation(button) {
                        startActivity(Intent(this@MenuActivity, EventsPage::class.java))
                    }
                    R.id.btnVolunteer -> addClickAnimation(button) {
                        startActivity(Intent(this@MenuActivity, ActivityStory::class.java))
                    }
                    R.id.btnAbout -> addClickAnimation(button) {
                        startActivity(Intent(this@MenuActivity, PrinciplesPage::class.java))
                    }
                    else -> addClickAnimation(button) {}
                }
            }, delay)
            delay += 170
        }
    }
}