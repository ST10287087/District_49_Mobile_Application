package vcmsa.projects.district49_android

import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import vcmsa.projects.district49_android.ui.nav.NavBarBinder

class ContactUsActivity : AppCompatActivity() {

    // Google Apps Script URL that handles sending emails
    private val scriptUrl = "https://script.google.com/macros/s/AKfycbw7QW95u59sEZW58Hr7aL6EU9koLa89Bd3zjVjMVm5YMf0-No7QVsTx5i8zD0pTW47ROA/exec"

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contact_us_page) // Set layout for this activity

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

        // Get references to input fields and submit button
        val nameEditText = findViewById<EditText>(R.id.contact_name)
        val surnameEditText = findViewById<EditText>(R.id.contact_surname)
        val emailEditText = findViewById<EditText>(R.id.contact_email)
        val phoneEditText = findViewById<EditText>(R.id.contact_phone)
        val messageEditText = findViewById<EditText>(R.id.contact_message)
        val submitButton = findViewById<MaterialButton>(R.id.submitButton)

        submitButton.setOnClickListener {
            // Read input values and trim whitespace
            val name = nameEditText.text.toString().trim()
            val surname = surnameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()
            val message = messageEditText.text.toString().trim()

            // Basic validation: name, email, and message are required
            if (name.isEmpty() || email.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple submissions this is when the email get sent twice
            submitButton.isEnabled = false

            // Send the form data to Google Script in a background thread
            CoroutineScope(Dispatchers.IO).launch {
                sendContactForm(
                    name, surname, email, phone, message,
                    onSuccess = {
                        runOnUiThread {
                            // Success: Show confirmation to user
                            Toast.makeText(
                                this@ContactUsActivity,
                                "Message sent successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            // Clear all fields after successful submission
                            nameEditText.text.clear()
                            surnameEditText.text.clear()
                            emailEditText.text.clear()
                            phoneEditText.text.clear()
                            messageEditText.text.clear()
                            // Re-enable submit button for next message
                            submitButton.isEnabled = true
                        }
                    },
                    onFailure = { errorMsg ->
                        runOnUiThread {
                            // Failed: Show server response or error
                            Toast.makeText(
                                this@ContactUsActivity,
                                "Failed to send message: $errorMsg",
                                Toast.LENGTH_LONG
                            ).show()
                            // Re-enable submit button to allow retry
                            submitButton.isEnabled = true
                        }
                    }
                )
            }
        }
    }


    private fun sendContactForm(
        name: String,
        surname: String,
        email: String,
        phone: String,
        message: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            val client = OkHttpClient() // HTTP client to send requests

            // Build JSON object with form data
            val json = JSONObject().apply {
                put("name", name)
                put("surname", surname)
                put("email", email)
                put("phone", phone)
                put("message", message)
            }

            // Convert JSON to request body with content type application/json
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            // Build POST request to the Google Script URL
            val request = Request.Builder()
                .url(scriptUrl)
                .post(body)
                .build()

            // Execute the request and get the response
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: "No response"
                if (response.isSuccessful) {
                    onSuccess() // Call success callback
                } else {
                    onFailure(responseBody) // Call failure callback
                }
            }
        } catch (e: Exception) {
            // Handle any exceptions (network errors, invalid JSON, etc.)
            onFailure(e.localizedMessage ?: "Unknown error")
        }
    }
}