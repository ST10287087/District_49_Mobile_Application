package vcmsa.projects.district49_android

import android.content.Intent
import android.os.Bundle
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vcmsa.projects.district49_android.databinding.RegisterPageBinding

class RegisterPage : AppCompatActivity() {
    private lateinit var binding: RegisterPageBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = vcmsa.projects.district49_android.databinding.RegisterPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = Firebase.auth

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
            // UPDATE THESE TO MATCH XML IDs
            val email = binding.registerEmail.text.toString().trim()
            val password = binding.registerPassword.text.toString().trim()
            val confirmPassword = binding.registerConfirmPassword.text.toString().trim()

            if (!validateInput(email, password, confirmPassword)) return@setOnClickListener

            lifecycleScope.launch {
                try {
                    auth.createUserWithEmailAndPassword(email, password).await()
                    Toast.makeText(
                        this@RegisterPage,
                        "Registration successful",
                        Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@RegisterPage, LoginPage::class.java))
                    finish()
                } catch (e: Exception) {
                    when (e) {
                        is FirebaseAuthWeakPasswordException -> binding.registerPassword.error = "Password too weak (min 6 chars)"
                        is FirebaseAuthUserCollisionException -> binding.registerEmail.error = "Email already in use"
                        else -> Toast.makeText(
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
        password: String,
        confirmPassword: String
    ): Boolean {
        var valid = true
        if (email.isEmpty()) {
            binding.registerEmail.error = "Email required"
            valid = false
        }
        if (password.isEmpty()) {
            binding.registerPassword.error = "Password required"
            valid = false
        } else if (password.length < 6) {
            binding.registerPassword.error = "Min 6 characters"
            valid = false
        }
        if (confirmPassword.isEmpty()) {
            binding.registerConfirmPassword.error = "Confirm your password"
            valid = false
        }
        if (password != confirmPassword) {
            binding.registerConfirmPassword.error = "Passwords do not match"
            valid = false
        }
        return valid
    }
}