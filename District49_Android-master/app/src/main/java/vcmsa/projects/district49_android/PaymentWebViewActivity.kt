package vcmsa.projects.district49_android

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity

class PaymentWebViewActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PAYMENT_URL = "extra_payment_url"
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_FORM_FIELDS = "extra_form_fields" // HashMap<String, String>
    }

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lightweight inline layout (WebView + progress)
        val root = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        webView = WebView(this)
        progress = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.VISIBLE
        }
        root.addView(webView)
        root.addView(progress, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.CENTER
        })
        setContentView(root)

        val paymentUrl = intent.getStringExtra(EXTRA_PAYMENT_URL).orEmpty()
        val orderId = intent.getStringExtra(EXTRA_ORDER_ID).orEmpty()
        val formFields = intent.getSerializableExtra(EXTRA_FORM_FIELDS) as? HashMap<*, *> ?: hashMapOf<String, String>()

        webView.settings.javaScriptEnabled = true // PayFast pages may use JS
        webView.settings.domStorageEnabled = true

        // Intercept deep links (myapp://payment/...)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString().orEmpty()
                if (url.startsWith("myapp://")) {
                    // Send to PaymentResultActivity to display result
                    val intent = Intent(this@PaymentWebViewActivity, PaymentResultActivity::class.java).apply {
                        data = Uri.parse(url)
                        putExtra(PaymentResultActivity.EXTRA_ORDER_ID, orderId)
                    }
                    startActivity(intent)
                    finish()
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress.visibility = View.GONE
            }
        }

        // Build an HTML form that auto-posts to PayFast
        val html = buildAutoPostHtml(paymentUrl, formFields as HashMap<String, String>)
        webView.loadDataWithBaseURL(
            null,
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun buildAutoPostHtml(actionUrl: String, fields: HashMap<String, String>): String {
        val inputs = fields.entries.joinToString("\n") { (k, v) ->
            // Escape minimal HTML
            val key = android.text.Html.escapeHtml(k)
            val value = android.text.Html.escapeHtml(v)
            """<input type="hidden" name="$key" value="$value"/>"""
        }
        return """
            <!DOCTYPE html>
            <html>
              <head>
                <meta charset="utf-8"/>
                <title>Redirecting to PayFast…</title>
              </head>
              <body onload="document.forms[0].submit();">
                <p>Redirecting to PayFast…</p>
                <form action="${actionUrl}" method="post">
                  $inputs
                  <noscript><button type="submit">Continue</button></noscript>
                </form>
              </body>
            </html>
        """.trimIndent()
    }
}
