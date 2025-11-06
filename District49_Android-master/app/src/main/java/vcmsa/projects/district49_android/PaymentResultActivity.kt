//package vcmsa.projects.district49_android
//
//import android.net.Uri
//import android.os.Bundle
//import android.widget.Button
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import okhttp3.OkHttpClient
//import okhttp3.Request
//import org.json.JSONObject
//import java.util.concurrent.Executors
//
//class PaymentResultActivity : AppCompatActivity() {
//
//    companion object {
//        const val EXTRA_ORDER_ID = "extra_order_id"
//    }
//
//    private val io = Executors.newSingleThreadExecutor()
//    private val client = OkHttpClient()
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(buildSimpleLayout())
//
//        val info = findViewById<TextView>(1001)
//        val btnClose = findViewById<Button>(1002)
//
//        val dataUri: Uri? = intent?.data
//        val localOrderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
//
//        val isSuccess = dataUri?.path?.startsWith("/success") == true
//        val isCancel = dataUri?.path?.startsWith("/cancel") == true
//
//        val orderIdFromUri = dataUri?.getQueryParameter("orderId")
//        val orderId = orderIdFromUri ?: localOrderId
//
//        info.text = when {
//            isSuccess -> "Payment successful.\n\nOrder: $orderId\nConfirming status…"
//            isCancel  -> "Payment cancelled.\n\nOrder: $orderId"
//            else      -> "Returned from payment.\n\nOrder: $orderId"
//        }
//
//        // Optional: confirm on server if success
//        if (isSuccess && orderId.isNotBlank()) {
//            confirmStatus(orderId) { statusText ->
//                runOnUiThread {
//                    info.text = "Payment successful.\n\nOrder: $orderId\nStatus: $statusText"
//                }
//            }
//        }
//
//        btnClose.setOnClickListener {
//            finish() // or navigate to Homepage if you want
//        }
//    }
//
//    private fun confirmStatus(orderId: String, onDone: (String) -> Unit) {
//        io.execute {
//            try {
//                val req = Request.Builder()
//                    .url("https://payfastapi-49.onrender.com/api/Payment/status/$orderId")
//                    .get()
//                    .build()
//                client.newCall(req).execute().use { resp ->
//                    val body = resp.body?.string().orEmpty()
//                    if (!resp.isSuccessful || body.isBlank()) {
//                        onDone("unknown")
//                        return@use
//                    }
//                    val json = JSONObject(body)
//                    val status = json.optString("status", "unknown")
//                    onDone(status)
//                }
//            } catch (_: Exception) {
//                onDone("unknown")
//            }
//        }
//    }
//
//    /**
//     * Small programmatic layout: Text + Close button
//     */
//    private fun buildSimpleLayout(): android.view.View {
//        val root = android.widget.LinearLayout(this).apply {
//            orientation = android.widget.LinearLayout.VERTICAL
//            setPadding(48, 48, 48, 48)
//        }
//        val tv = TextView(this).apply {
//            id = 1001
//            textSize = 18f
//        }
//        val btn = Button(this).apply {
//            id = 1002
//            text = "Close"
//        }
//        root.addView(tv, android.widget.LinearLayout.LayoutParams(
//            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
//            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
//        ))
//        root.addView(btn, android.widget.LinearLayout.LayoutParams(
//            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
//            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
//        ))
//        return root
//    }
//}

package vcmsa.projects.district49_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.Executors

class PaymentResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
    }

    private val io = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildSimpleLayout())

        val info = findViewById<TextView>(1001)
        val btnClose = findViewById<Button>(1002)

        val dataUri: Uri? = intent?.data
        val localOrderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()

        val isSuccess = dataUri?.path?.startsWith("/success") == true
        val isCancel = dataUri?.path?.startsWith("/cancel") == true

        val orderIdFromUri = dataUri?.getQueryParameter("orderId")
        val orderId = orderIdFromUri ?: localOrderId

        // Update the text based on success/cancel
        info.text = when {
            isSuccess -> "Your donation has been successfully processed.\n\nThank you for your generosity!"
            isCancel  -> "Payment was cancelled.\n\nNo charges were made."
            else      -> "Payment complete."
        }


        if (isSuccess && orderId.isNotBlank()) {
            confirmStatus(orderId) { statusText ->
          
            }
        }

        btnClose.setOnClickListener {
            val intent = Intent(this, Homepage::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun confirmStatus(orderId: String, onDone: (String) -> Unit) {
        io.execute {
            try {
                val req = Request.Builder()
                    .url("https://payfastapi-49.onrender.com/api/Payment/status/$orderId")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful || body.isBlank()) {
                        onDone("unknown")
                        return@use
                    }
                    val json = JSONObject(body)
                    val status = json.optString("status", "unknown")
                    onDone(status)
                }
            } catch (_: Exception) {
                onDone("unknown")
            }
        }
    }

    private fun buildSimpleLayout(): android.view.View {
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 80, 48, 80)
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
        }

        // Success checkmark
        val checkmark = TextView(this).apply {
            text = "✓"
            textSize = 72f
            setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 20)
        }

        // Title
        val title = TextView(this).apply {
            text = "Thank You!"
            textSize = 28f
            setTextColor(android.graphics.Color.parseColor("#28348D"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Main message (this is the TextView with id 1001)
        val message = TextView(this).apply {
            id = 1001
            text = "Your donation has been successfully processed.\n\nThank you for your generosity!"
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            gravity = android.view.Gravity.CENTER
            setPadding(20, 10, 20, 30)
            setLineSpacing(4f, 1.2f)
        }

        // Info about impact
        val impact = TextView(this).apply {
            text = "Your donation to District 49 helps us provide care, shelter, and support to children in need."
            textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#555555"))
            gravity = android.view.Gravity.CENTER
            setPadding(20, 10, 20, 20)
            setLineSpacing(4f, 1.2f)
        }

        // Info box for tax certificate
        val infoBox = TextView(this).apply {
            text = "Should you need any details or wish to request a Section 18A tax certificate, please contact us through the Contact Us form."
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#F57F17"))
            gravity = android.view.Gravity.CENTER
            setPadding(30, 20, 30, 20)
            setBackgroundColor(android.graphics.Color.parseColor("#FFF8E1"))
            setLineSpacing(4f, 1.2f)
        }

        // Return to Home button
        val btn = android.widget.Button(this).apply {
            id = 1002
            text = "Return to Home"
            textSize = 16f
            setBackgroundColor(android.graphics.Color.parseColor("#28348D"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(60, 30, 60, 30)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        root.addView(checkmark)
        root.addView(title)
        root.addView(message)
        root.addView(impact)
        root.addView(infoBox, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 20
            bottomMargin = 40
            leftMargin = 20
            rightMargin = 20
        })
        root.addView(btn, android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 20
        })

        return root
    }
}
