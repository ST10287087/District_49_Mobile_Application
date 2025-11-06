package vcmsa.projects.district49_android

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import vcmsa.projects.district49_android.databinding.LoginPageBinding

class LoginPage : AppCompatActivity() {
    private lateinit var binding: LoginPageBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth
        prefs = getSharedPreferences("login_prefs", MODE_PRIVATE)

        // Pre-fill email if Remember Me was checked
        val savedEmail = prefs.getString("saved_email", null)
        if (!savedEmail.isNullOrEmpty()) {
            binding.loginEmail.setText(savedEmail)
            binding.rememberCheckBox.isChecked = true
        }

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (email.isEmpty()) {
                binding.loginEmail.error = "Email required"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                binding.loginPassword.error = "Password required"
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        if (binding.rememberCheckBox.isChecked) {
                            prefs.edit().putString("saved_email", email).apply()
                        } else {
                            prefs.edit().remove("saved_email").apply()
                        }
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, Homepage::class.java)) // <--- UPDATED HERE
                        finish()
                    } else {
                        when (val e = task.exception) {
                            is FirebaseAuthInvalidUserException -> binding.loginEmail.error = "No account with this email"
                            is FirebaseAuthInvalidCredentialsException -> binding.loginPassword.error = "Invalid credentials"
                            else -> Toast.makeText(
                                this,
                                "Login failed: ${e?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }

        binding.forgotPasswordButton.setOnClickListener {
            val intent = Intent(this, ResetPassword::class.java)
            startActivity(intent)
        }
    }
}