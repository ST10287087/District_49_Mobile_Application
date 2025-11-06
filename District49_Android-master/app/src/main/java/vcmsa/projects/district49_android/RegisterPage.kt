package vcmsa.projects.district49_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import vcmsa.projects.district49_android.databinding.RegisterPageBinding
import vcmsa.projects.district49_android.utils.AuthManager

class RegisterPage : AppCompatActivity() {
    private lateinit var binding: RegisterPageBinding
    private lateinit var authManager: AuthManager

    companion object {
        private const val TAG = "RegisterPage"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RegisterPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authManager = AuthManager.getInstance()

        // Handle insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        // Navigate to Login if user already has an account
        binding.loginCheckbox.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            if (isChecked) {
                startActivity(Intent(this, LoginPage::class.java))
                finish()
            }
        }

        // Register button click
        binding.registerButton.setOnClickListener {
            val email = binding.registerEmail.text.toString().trim()
            val name = binding.registerName.text.toString().trim()
            val surname = binding.registerSurname.text.toString().trim()
            val password = binding.registerPassword.text.toString().trim()
            val confirmPassword = binding.registerConfirmPassword.text.toString().trim()

            if (!validateInput(email, name, surname, password, confirmPassword)) {
                return@setOnClickListener
            }

            // Show loading state
            binding.registerButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    val result = authManager.registerUser(email, password, name, surname)

                    if (result.isSuccess) {
                        val user = result.getOrNull()!!
                        runOnUiThread {
                            // 🔔 Initialize FCM token for new user
                            lifecycleScope.launch {
                                try {
                                    District49NotificationManager.getInstance().initializeFCMToken()
                                    Log.d(TAG, "✅ FCM token initialized for new user")
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Failed to initialize FCM token: ${e.message}", e)
                                }
                            }

                            Toast.makeText(
                                this@RegisterPage,
                                "Registration successful! Welcome ${user.getFullName()}",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this@RegisterPage, LoginPage::class.java))
                            finish()
                        }
                    } else {
                        val error = result.exceptionOrNull()!!
                        runOnUiThread {
                            binding.registerButton.isEnabled = true
                            when {
                                error.message?.contains("weak password") == true -> {
                                    binding.registerPassword.error = "Password too weak (min 6 characters)"
                                }
                                error.message?.contains("already in use") == true -> {
                                    binding.registerEmail.error = "Email already in use"
                                }
                                error.message?.contains("email address is badly formatted") == true -> {
                                    binding.registerEmail.error = "Invalid email format"
                                }
                                else -> {
                                    Toast.makeText(
                                        this@RegisterPage,
                                        "Registration failed: ${error.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        binding.registerButton.isEnabled = true
                        Toast.makeText(
                            this@RegisterPage,
                            "Registration failed: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun validateInput(
        email: String,
        name: String,
        surname: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        var valid = true

        // Clear previous errors
        binding.registerEmail.error = null
        binding.registerName.error = null
        binding.registerSurname.error = null
        binding.registerPassword.error = null
        binding.registerConfirmPassword.error = null

        if (email.isEmpty()) {
            binding.registerEmail.error = "Email required"
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.registerEmail.error = "Invalid email format"
            valid = false
        }

        if (name.isEmpty()) {
            binding.registerName.error = "Name required"
            valid = false
        }

        if (surname.isEmpty()) {
            binding.registerSurname.error = "Surname required"
            valid = false
        }

        if (password.isEmpty()) {
            binding.registerPassword.error = "Password required"
            valid = false
        } else if (password.length < 6) {
            binding.registerPassword.error = "Password must be at least 6 characters"
            valid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.registerConfirmPassword.error = "Please confirm your password"
            valid = false
        } else if (password != confirmPassword) {
            binding.registerConfirmPassword.error = "Passwords do not match"
            valid = false
        }

        return valid
    }
}