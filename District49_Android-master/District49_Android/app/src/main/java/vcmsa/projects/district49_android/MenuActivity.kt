//package vcmsa.projects.district49_android
//
//import android.animation.ObjectAnimator
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.view.View
//import android.view.animation.AccelerateDecelerateInterpolator
//import android.widget.ImageButton
//import android.widget.ImageView
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import android.content.Intent
//
//class MenuActivity : AppCompatActivity() {
//
//    private fun addClickAnimation(button: ImageButton) {
//        button.setOnClickListener {
//            button.animate()
//                .scaleX(0.93f)
//                .scaleY(0.93f)
//                .setDuration(70)
//                .withEndAction {
//                    button.animate()
//                        .scaleX(1f)
//                        .scaleY(1f)
//                        .setDuration(70)
//                        .start()
//
//
//                }
//                .start()
//            // TODO: Add your click logic here if needed
//        }
//    }
//
//    private fun animateCloud(cloud: ImageView, fromX: Float, toX: Float, duration: Long) {
//        cloud.translationX = fromX
//        val animator = ObjectAnimator.ofFloat(cloud, View.TRANSLATION_X, fromX, toX)
//        animator.duration = duration
//        animator.repeatMode = ObjectAnimator.REVERSE
//        animator.repeatCount = ObjectAnimator.INFINITE
//        animator.interpolator = AccelerateDecelerateInterpolator()
//        animator.start()
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_menu)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//
//        // 1. Animate clouds
//        val cloudLeft = findViewById<ImageView>(R.id.imgCloudLeft)
//        val cloudRight = findViewById<ImageView>(R.id.imgCloudRight)
//        animateCloud(cloudLeft, 20f, 500f, 12000L)
//        animateCloud(cloudRight, -40f, 350f, 10000L)
//
//        // 2. Animate pole rising up
//        val pole = findViewById<ImageView>(R.id.imgPole)
//        pole.post {
//            val startY = pole.translationY + 500f
//            pole.translationY = startY
//            pole.alpha = 0f
//            pole.animate()
//                .translationYBy(-500f)
//                .alpha(1f)
//                .setDuration(900)
//                .withEndAction {
//                    // 3. Animate buttons after pole is in place
//                    animateMenuButtonsSequentially()
//                }
//                .start()
//        }
//    }
//
//    private fun animateMenuButtonsSequentially() {
//        val buttonIds = listOf(
//            R.id.btnAbout,
//            R.id.btnDonate,
//            R.id.btnVolunteer,
//            R.id.btnContact,
//            R.id.btnTeam,
//            R.id.btnSuccess,
//            R.id.btnGallery,
//            R.id.btnEvents
//        )
//        val handler = Handler(Looper.getMainLooper())
//        var delay = 0L
//        buttonIds.forEach { id ->
//            val button = findViewById<ImageButton>(id)
//            button.animate().cancel()
//            button.alpha = 0f
//            button.translationX = button.translationX - 600f
//            button.visibility = View.VISIBLE
//            handler.postDelayed({
//                button.animate()
//                    .translationXBy(600f)
//                    .alpha(1f)
//                    .setDuration(320)
//                    .setInterpolator(AccelerateDecelerateInterpolator())
//                    .start()
//                addClickAnimation(button)
//
//
//                // 🚀 Launch DonateOne when btnDonate is clicked
//                if (id == R.id.btnDonate) {
//                    button.setOnClickListener {
//                        val intent = Intent(this, DonateOne::class.java)
//                        startActivity(intent)
//                    }
//                }
//            }, delay)
//            delay += 170
//        }
//
//
//
//        // Add clicky effect to other top-corner buttons if desired
//        addClickAnimation(findViewById(R.id.btnProfile))
//        addClickAnimation(findViewById(R.id.btnSearch))
//        addClickAnimation(findViewById(R.id.btnMenu))
//    }
//}
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

class MenuActivity : AppCompatActivity() {

    // Click animation helper with optional action
    private fun addClickAnimation(button: ImageButton, onClickAction: (() -> Unit)? = null) {
        button.setOnClickListener {
            button.animate()
                .scaleX(0.93f)
                .scaleY(0.93f)
                .setDuration(70)
                .withEndAction {
                    button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(70)
                        .withEndAction {
                            // Execute the actual click action after animation
                            onClickAction?.invoke()
                        }
                        .start()
                }
                .start()
        }
    }

    // Animate clouds back and forth
    private fun animateCloud(cloud: ImageView, fromX: Float, toX: Float, duration: Long) {
        cloud.translationX = fromX
        val animator = ObjectAnimator.ofFloat(cloud, View.TRANSLATION_X, fromX, toX)
        animator.duration = duration
        animator.repeatMode = ObjectAnimator.REVERSE
        animator.repeatCount = ObjectAnimator.INFINITE
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Animate clouds
        val cloudLeft = findViewById<ImageView>(R.id.imgCloudLeft)
        val cloudRight = findViewById<ImageView>(R.id.imgCloudRight)
        animateCloud(cloudLeft, 20f, 500f, 12000L)
        animateCloud(cloudRight, -40f, 350f, 10000L)

        // Animate pole rising
        val pole = findViewById<ImageView>(R.id.imgPole)
        pole.post {
            val startY = pole.translationY + 500f
            pole.translationY = startY
            pole.alpha = 0f
            pole.animate()
                .translationYBy(-500f)
                .alpha(1f)
                .setDuration(900)
                .withEndAction {
                    // Animate buttons after pole
                    animateMenuButtonsSequentially()
                }
                .start()
        }
    }

    private fun animateMenuButtonsSequentially() {
        val buttonIds = listOf(
            R.id.btnAbout,
            R.id.btnDonate,      // Ensure this ID exists in activity_menu.xml
            R.id.btnVolunteer,
            R.id.btnContact,
            R.id.btnTeam,
            R.id.btnSuccess,
            R.id.btnGallery,
            R.id.btnEvents
        )

        val handler = Handler(Looper.getMainLooper())
        var delay = 0L

        buttonIds.forEach { id ->
            val button = findViewById<ImageButton>(id)
            if (button == null) return@forEach  // Skip if button is missing in layout

            button.animate().cancel()
            button.alpha = 0f
            button.translationX = button.translationX - 600f
            button.visibility = View.VISIBLE

            handler.postDelayed({
                button.animate()
                    .translationXBy(600f)
                    .alpha(1f)
                    .setDuration(320)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                // Set click animations and actions
                if (id == R.id.btnDonate) {
                    // Donate button opens DonateOne activity
                    addClickAnimation(button) {
                        val intent = Intent(this@MenuActivity, DonateOne::class.java)
                        startActivity(intent)
                    }
                } else {
                    // Other buttons: just animate
                    addClickAnimation(button)
                }

            }, delay)
            delay += 170
        }

        // Add click animation to top-corner buttons (no navigation)
        findViewById<ImageButton>(R.id.btnProfile)?.let { addClickAnimation(it) }
        findViewById<ImageButton>(R.id.btnSearch)?.let { addClickAnimation(it) }
        findViewById<ImageButton>(R.id.btnMenu)?.let { addClickAnimation(it) }
    }
}
