package vcmsa.projects.district49_android

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class District49MessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "district49_channel"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM Token: $token")

        // Save token to Firestore
        saveFCMTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d(TAG, "Message received from: ${message.from}")

        // Check if message contains notification payload
        message.notification?.let {
            Log.d(TAG, "Notification Title: ${it.title}")
            Log.d(TAG, "Notification Body: ${it.body}")

            showNotification(
                title = it.title ?: "District 49",
                body = it.body ?: "",
                notificationType = message.data["type"] ?: "general"
            )
        }
    }

    private fun showNotification(title: String, body: String, notificationType: String) {
        createNotificationChannel()

        val intent = Intent(this, Homepage::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.final_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "District 49 Notifications"
            val descriptionText = "Notifications for events, news, and newsletters"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun saveFCMTokenToFirestore(token: String) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return

        val tokenData = hashMapOf(
            "token" to token,
            "updatedAt" to System.currentTimeMillis()
        )

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("user_fcm_tokens")
            .document(userId)
            .set(tokenData)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token saved successfully")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving FCM token", e)
            }
    }
}