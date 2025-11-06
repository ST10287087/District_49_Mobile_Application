package vcmsa.projects.district49_android

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ResetPassword : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.reset_password)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.fieldEmail)
        val resetButton = findViewById<ImageButton>(R.id.btnResetPassword)

        resetButton.setOnClickListener {
            // Animate button "press"
            resetButton.animate()
                .scaleX(0.93f)
                .scaleY(0.93f)
                .setDuration(70)
                .withEndAction {
                    resetButton.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(70)
                        .start()
                }
                .start()

            // 🔑 Handle reset logic here
            val email = emailField.text.toString().trim()
            if (email.isEmpty()) {
                emailField.error = "Enter your email"
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset email sent. Check your inbox.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish() // Optionally close and return to login
                    } else {
                        Toast.makeText(
                            this,
                            "Error: ${task.exception?.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }
    }
}
