package vcmsa.projects.district49_android

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import java.io.IOException

class DonateTwo : AppCompatActivity() {

    private lateinit var edtAmount: TextInputEditText

    private val apiInitiateUrl = "https://payfastapi-49.onrender.com/api/Payment/initiate"
    private val appDeepLinkBase = "myapp://payment"

    private val client by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.donate2)

        edtAmount = findViewById(R.id.edtAmount)

        // Wire up the navbar
        wireNavBar()

        findViewById<ImageButton>(R.id.btnR100).setOnClickListener { edtAmount.setText("100") }
        findViewById<ImageButton>(R.id.btnR200).setOnClickListener { edtAmount.setText("200") }
        findViewById<ImageButton>(R.id.btnR500).setOnClickListener { edtAmount.setText("500") }

        findViewById<ImageButton>(R.id.btnDonate).setOnClickListener {
            val amountStr = edtAmount.text?.toString()?.trim().orEmpty()
            if (amountStr.isEmpty()) {
                edtAmount.error = "Enter an amount"
                return@setOnClickListener
            }
            val amount = amountStr.toBigDecimalOrNull()
            if (amount == null || amount <= java.math.BigDecimal.ZERO) {
                edtAmount.error = "Enter a valid amount"
                return@setOnClickListener
            }

            // Show terms dialog before proceeding
            showTermsAndConditionsDialog(amountStr)
        }
    }

    private fun wireNavBar() {
        // Back button - goes back to DonateOne
        findViewById<ImageButton?>(R.id.nav_btn_arrow_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Plus (create/new) - goes to MenuActivity
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

    private fun showTermsAndConditionsDialog(amount: String) {
        // Create scrollable content
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }

        // Create styled terms text
        val termsText = TextView(this).apply {
            text = buildStyledTermsText()
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#333333"))
            setPadding(16, 16, 16, 16)
            setLineSpacing(4f, 1.2f)
        }

        layout.addView(termsText)
        scrollView.addView(layout)

        // Create and show dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Terms and Conditions")
            .setView(scrollView)
            .setPositiveButton("Accept & Donate") { _, _ ->
                // User accepted - proceed with payment
                initiatePayment(amount)
            }
            .setNegativeButton("Decline") { dialog, _ ->
                // User declined
                Toast.makeText(
                    this,
                    "You must accept the Terms and Conditions to proceed with your donation",
                    Toast.LENGTH_LONG
                ).show()
                dialog.dismiss()
            }
            .setCancelable(false) // User must choose Accept or Decline
            .create()

        dialog.show()

        // Style the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            android.graphics.Color.parseColor("#4CAF50")
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            android.graphics.Color.parseColor("#F44336")
        )
    }

    private fun buildStyledTermsText(): String {
        return TermsAndConditionsContent.sections.joinToString("\n\n") { section ->
            "${section.title}\n${section.content}"
        }
    }

    private fun initiatePayment(amount: String) {
        val name = "Donor"
        val email = ""

        val json = JSONObject().apply {
            put("amount", amount.toBigDecimal())
            put("name", name)
            put("email", email)
            put("description", "Donation")
            put("itemName", "Donation")
            put("baseUrl", appDeepLinkBase)
            put("clientType", "mobile")
        }

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url(apiInitiateUrl)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@DonateTwo, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val respStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(
                                this@DonateTwo,
                                "API error: ${response.code}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return
                    }
                    try {
                        val root = JSONObject(respStr)
                        val success = root.optBoolean("success", false)
                        if (!success) {
                            val msg = root.optString("message", "Failed to start payment")
                            runOnUiThread { Toast.makeText(this@DonateTwo, msg, Toast.LENGTH_LONG).show() }
                            return
                        }
                        val orderId = root.optString("orderId")
                        val paymentUrl = root.optString("paymentUrl")
                        val paymentData = root.optJSONObject("paymentData") ?: JSONObject()

                        val map = HashMap<String, String>()
                        paymentData.keys().forEach { key ->
                            map[key] = paymentData.optString(key, "")
                        }

                        val intent = Intent(this@DonateTwo, PaymentWebViewActivity::class.java).apply {
                            putExtra(PaymentWebViewActivity.EXTRA_PAYMENT_URL, paymentUrl)
                            putExtra(PaymentWebViewActivity.EXTRA_ORDER_ID, orderId)
                            putExtra(PaymentWebViewActivity.EXTRA_FORM_FIELDS, map)
                        }
                        runOnUiThread {
                            startActivity(intent)
                        }
                    } catch (ex: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@DonateTwo, "Parse error: ${ex.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }
}