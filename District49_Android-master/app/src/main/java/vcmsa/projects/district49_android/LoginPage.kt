package vcmsa.projects.district49_android

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vcmsa.projects.district49_android.databinding.LoginPageBinding
import vcmsa.projects.district49_android.utils.AuthManager

class LoginPage : AppCompatActivity() {
    private lateinit var binding: LoginPageBinding
    private lateinit var authManager: AuthManager
    private lateinit var prefs: SharedPreferences

    companion object {
        private const val TAG = "LoginPage"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager.getInstance()
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

            if (!validateInput(email, password)) {
                return@setOnClickListener
            }

            // Show loading state
            binding.loginButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    val loginResult = authManager.loginUser(email, password)

                    if (loginResult.isSuccess) {
                        // Get user data to welcome them and check role
                        val userDataResult = authManager.getCurrentUserData()

                        runOnUiThread {
                            // Handle Remember Me
                            if (binding.rememberCheckBox.isChecked) {
                                prefs.edit().putString("saved_email", email).apply()
                            } else {
                                prefs.edit().remove("saved_email").apply()
                            }

                            // 🔔 CRITICAL: Initialize FCM token for logged-in user
                            lifecycleScope.launch {
                                try {
                                    District49NotificationManager.getInstance().initializeFCMToken()
                                    Log.d(TAG, "✅ FCM token initialized after login")
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Failed to initialize FCM token: ${e.message}", e)
                                }
                            }

                            if (userDataResult.isSuccess) {
                                val userData = userDataResult.getOrNull()!!
                                val roleText = if (userData.isAdmin()) "Admin" else "User"
                                Toast.makeText(
                                    this@LoginPage,
                                    "Welcome back, ${userData.getFullName()}! ($roleText)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(this@LoginPage, "Login successful", Toast.LENGTH_SHORT).show()
                            }

                            // Navigate to Homepage
                            startActivity(Intent(this@LoginPage, Homepage::class.java))
                            finish()
                        }
                    } else {
                        val error = loginResult.exceptionOrNull()!!
                        runOnUiThread {
                            binding.loginButton.isEnabled = true
                            when {
                                error.message?.contains("user not found") == true ||
                                        error.message?.contains("invalid-user-token") == true -> {
                                    binding.loginEmail.error = "No account found with this email"
                                }
                                error.message?.contains("wrong-password") == true ||
                                        error.message?.contains("invalid-credential") == true -> {
                                    binding.loginPassword.error = "Invalid password"
                                }
                                error.message?.contains("too-many-requests") == true -> {
                                    Toast.makeText(
                                        this@LoginPage,
                                        "Too many failed attempts. Please try again later.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                else -> {
                                    Toast.makeText(
                                        this@LoginPage,
                                        "Login failed: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.loginButton.isEnabled = true
                        Toast.makeText(
                            this@LoginPage,
                            "Login failed: ${e.message}",
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

    private fun validateInput(email: String, password: String): Boolean {
        var valid = true

        // Clear previous errors
        binding.loginEmail.error = null
        binding.loginPassword.error = null

        if (email.isEmpty()) {
            binding.loginEmail.error = "Email required"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginEmail.error = "Invalid email format"
            valid = false
        }

        if (password.isEmpty()) {
            binding.loginPassword.error = "Password required"
            valid = false
        }

        return valid
    }
}