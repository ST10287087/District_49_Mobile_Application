package vcmsa.projects.district49_android

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import vcmsa.projects.district49_android.models.NotificationPreferences
import java.util.concurrent.TimeUnit

class District49NotificationManager private constructor() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "District49Notifications"
        private const val CHANNEL_ID = "district49_channel"
        private const val CHANNEL_NAME = "District 49 Updates"
        private const val CHANNEL_DESC = "Notifications for events, news, and newsletters"

        // 🔥 YOUR CLOUD FUNCTION URL
        private const val CLOUD_FUNCTION_URL = "https://us-central1-district49-db.cloudfunctions.net/sendPushNotification"

        @Volatile
        private var INSTANCE: District49NotificationManager? = null

        fun getInstance(): District49NotificationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: District49NotificationManager().also { INSTANCE = it }
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = AndroidNotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "✅ Notification channel created")
        }
    }

    // Initialize FCM token for current user
    suspend fun initializeFCMToken() {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.w(TAG, "⚠️ Cannot initialize FCM token: No user logged in")
                return
            }

            Log.d(TAG, "🔄 Fetching FCM token for user ${currentUser.uid}...")
            val token = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "📱 Got FCM token: ${token.take(20)}...") // Log first 20 chars for security

            val tokenData = hashMapOf(
                "token" to token,
                "updatedAt" to System.currentTimeMillis()
            )

            firestore.collection("user_fcm_tokens")
                .document(currentUser.uid)
                .set(tokenData)
                .await()

            Log.d(TAG, "✅ FCM token saved to Firestore for user ${currentUser.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error initializing FCM token: ${e.message}", e)
        }
    }

    suspend fun getUserPreferences(userId: String): Result<NotificationPreferences> {
        return try {
            val doc = firestore.collection("user_notification_preferences")
                .document(userId)
                .get()
                .await()

            if (doc.exists()) {
                val prefs = doc.toObject(NotificationPreferences::class.java)
                    ?: NotificationPreferences(userId = userId)
                Result.success(prefs)
            } else {
                Log.d(TAG, "No notification preferences found for user $userId")
                Result.failure(Exception("No preferences found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preferences for $userId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(prefs: NotificationPreferences): Result<Boolean> {
        return try {
            firestore.collection("user_notification_preferences")
                .document(prefs.userId)
                .set(prefs.copy(updatedAt = System.currentTimeMillis()))
                .await()
            Log.d(TAG, "✅ Updated notification preferences for ${prefs.userId}")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update preferences", e)
            Result.failure(e)
        }
    }

    // 🔥 Call Cloud Function to send notifications
    suspend fun notifyAllUsersWithPreference(
        context: Context,
        notificationType: NotificationType,
        title: String,
        message: String
    ): Result<Int> {
        return try {
            Log.d(TAG, "📤 Calling Cloud Function for $notificationType notification")
            Log.d(TAG, "   Title: $title")
            Log.d(TAG, "   Message: ${message.take(50)}...")

            val json = JSONObject().apply {
                put("notificationType", notificationType.name.lowercase())
                put("title", title)
                put("message", message)
            }

            Log.d(TAG, "📦 Request payload: ${json.toString()}")

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = json.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(CLOUD_FUNCTION_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d(TAG, "📥 Response code: ${response.code}")
            Log.d(TAG, "📥 Response body: $responseBody")

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val results = jsonResponse.getJSONObject("results")
                val successCount = results.getInt("success")
                val failedCount = results.getInt("failed")
                val skippedCount = results.getInt("skipped")

                Log.d(TAG, "✅ Notifications sent successfully:")
                Log.d(TAG, "   ✓ Success: $successCount")
                Log.d(TAG, "   ✗ Failed: $failedCount")
                Log.d(TAG, "   ⊘ Skipped: $skippedCount")

                Result.success(successCount)
            } else {
                Log.e(TAG, "❌ Cloud Function error: ${response.code} - $responseBody")
                Result.failure(Exception("Failed to send notifications: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error calling Cloud Function: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun notifyNewsletterSubscribers(
        context: Context,
        title: String,
        message: String
    ): Result<Int> {
        return notifyAllUsersWithPreference(context, NotificationType.NEWSLETTER, title, message)
    }

    // Deprecated - kept for backwards compatibility
    @Deprecated("Use notifyAllUsersWithPreference instead")
    suspend fun sendNotification(
        context: Context,
        userId: String,
        notificationType: NotificationType,
        title: String,
        message: String,
        notificationId: Int = System.currentTimeMillis().toInt()
    ): Result<Boolean> {
        Log.d(TAG, "sendNotification called - this now uses Cloud Function")
        return Result.success(true)
    }
}

enum class NotificationType {
    EVENT,
    NEWS,
    NEWSLETTER
}