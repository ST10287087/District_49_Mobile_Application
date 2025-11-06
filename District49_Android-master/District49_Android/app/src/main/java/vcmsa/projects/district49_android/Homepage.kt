package vcmsa.projects.district49_android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent

class Homepage : AppCompatActivity() {

    private fun addClickAnimation(button: ImageButton, onClick: () -> Unit = {}) {
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
                            onClick()
                        }
                        .start()
                }
                .start()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.homepage)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        animateMenuButtonsSequentially()

        addClickAnimation(findViewById(R.id.btnProfile)) { /* TODO: Handle Profile click */ }
        addClickAnimation(findViewById(R.id.btnSearch)) { /* TODO: Handle Search click */ }
//        addClickAnimation(findViewById(R.id.btnMenu))

        addClickAnimation(findViewById(R.id.btnMenu)) {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }
    }
    private fun animateMenuButtonsSequentially() {
        val buttonIds = listOf(
            R.id.btnNewsAnnouncements,
            R.id.btnRectangle42,
            R.id.btnUpcomingEvents,
            R.id.btnDonationHistory,
            R.id.btnIncentives
        )
        val handler = Handler(Looper.getMainLooper())
        var delay = 0L
        buttonIds.forEach { id ->
            val button = findViewById<ImageButton>(id)
            button.animate().cancel()
            button.alpha = 0f

            // Move to bottom and shrink (start state)
            button.translationY += 300f
            button.scaleX = 0.7f
            button.scaleY = 0.7f
            button.visibility = View.VISIBLE

            handler.postDelayed({
                button.animate()
                    .translationYBy(-300f)     // Move up to position
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(420)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
                addClickAnimation(button) {
                    when (id) {
                        R.id.btnNewsAnnouncements -> { /* News/Announcements click */ }
                        R.id.btnRectangle42 -> { /* Rectangle42 click */ }
                        R.id.btnUpcomingEvents -> { /* Upcoming Events click */ }
                        R.id.btnDonationHistory -> { /* Donation History click */ }
                        R.id.btnIncentives -> { /* Incentives click */ }
                    }
                }
            }, delay)
            delay += 170
        }
    }
}
