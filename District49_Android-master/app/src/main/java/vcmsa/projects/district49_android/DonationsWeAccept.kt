package vcmsa.projects.district49_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DonationsWeAccept : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.donations_we_accept)

        wireNavBar()
    }

    private fun wireNavBar() {
        // Back button - goes back to previous screen
        findViewById<ImageButton?>(R.id.nav_btn_arrow_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        findViewById<ImageButton?>(R.id.nav_btn_plus_circle)?.setOnClickListener {
            runCatching {
                startActivity(Intent(this, MenuActivity::class.java))
            }.onFailure {
                Toast.makeText(this, "Menu not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Notifications - goes to NotificationSettingsActivity
        findViewById<ImageButton?>(R.id.nav_btn_notifications)?.setOnClickListener {
            runCatching {
                startActivity(Intent(this, NotificationSettingsActivity::class.java))
            }.onFailure {
                Toast.makeText(this, "Notifications not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Profile - goes to ProfileActivity
        findViewById<ImageButton?>(R.id.nav_btn_profile)?.setOnClickListener {
            runCatching {
                startActivity(Intent(this, ProfileActivity::class.java))
            }.onFailure {
                Toast.makeText(this, "Profile not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Home - goes back to Homepage
        findViewById<ImageButton?>(R.id.nav_btn_home)?.setOnClickListener {
            runCatching {
                val intent = Intent(this, Homepage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }.onFailure {
                Toast.makeText(this, "Home not available", Toast.LENGTH_SHORT).show()
            }
        }
    }
}