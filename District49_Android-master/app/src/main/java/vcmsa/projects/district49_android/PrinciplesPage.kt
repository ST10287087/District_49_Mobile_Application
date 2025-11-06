package vcmsa.projects.district49_android

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import vcmsa.projects.district49_android.ui.nav.NavBarBinder

class PrinciplesPage : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.principles_page)

        // Safe window insets padding (expects root id = main in principles_page.xml)
        findViewById<View?>(R.id.main)?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }

        // Bind navbar with proper navigation
        NavBarBinder.bind(
            findViewById(R.id.include_bottom_nav),
            NavBarBinder.Actions(
                onBack = {
                    onBackPressedDispatcher.onBackPressed()
                },
                onPlus = {
                    // Navigate to MenuActivity
                    runCatching {
                        startActivity(Intent(this, MenuActivity::class.java))
                    }.onFailure {
                        Toast.makeText(this, "Menu screen not available", Toast.LENGTH_SHORT).show()
                    }
                },
                onNotifications = {
                    // Navigate to NotificationSettingsActivity
                    runCatching {
                        startActivity(Intent(this, NotificationSettingsActivity::class.java))
                    }.onFailure {
                        Toast.makeText(this, "Notifications not available", Toast.LENGTH_SHORT).show()
                    }
                },
                onProfile = { isLoggedIn ->
                    if (isLoggedIn) {
                        runCatching {
                            startActivity(Intent(this, ProfileActivity::class.java))
                        }.onFailure {
                            Toast.makeText(this, "Profile screen not available", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        // Navigate to login screen if not logged in
                        runCatching {
                            startActivity(Intent(this, LoginPage::class.java))
                        }.onFailure {
                            Toast.makeText(this, "Login screen not available", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onHome = {
                    // Navigate to Homepage with clear stack
                    runCatching {
                        val intent = Intent(this, Homepage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish() // Finish current activity so back button goes to home
                    }.onFailure {
                        Toast.makeText(this, "Home screen not available", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        )
    }
}