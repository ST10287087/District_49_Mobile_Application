package vcmsa.projects.district49_android

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import vcmsa.projects.district49_android.models.DonationGoal
import vcmsa.projects.district49_android.models.Newsletter
import vcmsa.projects.district49_android.models.NewsAnnouncement
import java.io.ByteArrayOutputStream
import java.util.*

class AdminRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val client = OkHttpClient()

    // 🔔 NEW: Notification manager
    private val notificationManager = District49NotificationManager.getInstance()

    private val emailServiceUrl =
        "https://script.google.com/macros/s/AKfycbyFPZ2bTgGq5E74pnMGg8hoxM4PNpo_7lcSWdM2xvQuavcrIbA9rXyjoKg2LrJzM025/exec"

    private val MAX_BASE64_BYTES = 20 * 1024 * 1024

    // -------------------- Donation Goals --------------------
    suspend fun updateDonationGoal(goalAmount: Int, raisedAmount: Int): Result<DonationGoal> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

            val donationGoal = DonationGoal(
                id = "main_goal",
                goalAmount = goalAmount,
                raisedAmount = raisedAmount,
                lastUpdated = System.currentTimeMillis(),
                updatedBy = currentUser.uid
            )

            firestore.collection("donation_goals")
                .document("main_goal")
                .set(donationGoal)
                .await()

            Result.success(donationGoal)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentDonationGoal(): Result<DonationGoal> {
        return try {
            val document = firestore.collection("donation_goals")
                .document("main_goal")
                .get()
                .await()

            if (document.exists()) {
                val goal = document.toObject(DonationGoal::class.java)
                    ?: DonationGoal(id = "main_goal", goalAmount = 120000, raisedAmount = 50000)
                Result.success(goal)
            } else {
                val defaultGoal = DonationGoal(
                    id = "main_goal",
                    goalAmount = 120000,
                    raisedAmount = 50000
                )
                firestore.collection("donation_goals")
                    .document("main_goal")
                    .set(defaultGoal)
                    .await()
                Result.success(defaultGoal)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- Upload Newsletter (UPDATED with notifications) --------------------
    suspend fun uploadNewsletter(fileUri: Uri, context: Context): Result<Newsletter> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(fileUri) ?: "application/pdf"
            val extension = when {
                mimeType.contains("pdf") -> ".pdf"
                mimeType.contains("png") -> ".png"
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
                mimeType.contains("word") || mimeType.contains("document") -> ".docx"
                else -> ".pdf"
            }

            val fileName = "newsletter_${System.currentTimeMillis()}$extension"
            val storagePath = "newsletters/$fileName"
            val storageRef = storage.reference.child(storagePath)

            val downloadUrl = storageRef.putFile(fileUri).await()
                .storage.downloadUrl.await().toString()

            val newsletter = Newsletter(
                id = UUID.randomUUID().toString(),
                title = "District 49 Newsletter - ${java.text.SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())}",
                pdfUrl = downloadUrl,
                pdfPath = storagePath,
                sentAt = System.currentTimeMillis(),
                sentBy = currentUser.uid,
                recipientCount = 0
            )

            firestore.collection("newsletters")
                .document(newsletter.id)
                .set(newsletter)
                .await()

            val sendResult = sendNewsletterToSubscribers(newsletter, fileUri, context)
            val finalRecipientCount = if (sendResult.isSuccess) sendResult.getOrDefault(0) else 0

            firestore.collection("newsletters")
                .document(newsletter.id)
                .update("recipientCount", finalRecipientCount)
                .await()

            // 🔔 NEW: Send push notifications to newsletter subscribers
            withContext(Dispatchers.IO) {
                try {
                    notificationManager.notifyNewsletterSubscribers(
                        context,
                        "New Newsletter Available! 📬",
                        "A new District 49 newsletter has been sent to your email. Check your inbox!"
                    )
                } catch (e: Exception) {
                    // Log but don't fail main operation
                }
            }

            Result.success(newsletter.copy(recipientCount = finalRecipientCount))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------- Subscriber querying & sending --------------------
    private suspend fun sendNewsletterToSubscribers(
        newsletter: Newsletter,
        fileUri: Uri,
        context: Context
    ): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val subscribers = firestore.collection("newsletter_subscribers")
                    .whereEqualTo("active", true)
                    .get()
                    .await()

                val emailList = mutableListOf<String>()
                val nameList = mutableListOf<String>()

                subscribers.documents.forEach { doc ->
                    val email = doc.getString("email")
                    val name = doc.getString("name")
                    val active = doc.getBoolean("active") ?: false

                    if (!email.isNullOrBlank() && active) {
                        emailList.add(email.trim())
                        nameList.add((name ?: "Subscriber").trim())
                    }
                }

                if (emailList.isEmpty()) return@withContext Result.success(0)

                val base64Success = sendNewsletterEmailWithBase64(emailList, nameList, newsletter, fileUri, context)
                if (base64Success) return@withContext Result.success(emailList.size)

                val fallbackSuccess = sendNewsletterEmailUsingUrl(emailList, nameList, newsletter)
                if (fallbackSuccess) return@withContext Result.success(emailList.size)

                Result.failure(Exception("Failed to send newsletter emails"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun sendNewsletterEmailWithBase64(
        emails: List<String>,
        names: List<String>,
        newsletter: Newsletter,
        fileUri: Uri,
        context: Context
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                if (inputStream == null) return@withContext false

                val baos = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    baos.write(buffer, 0, read)
                    if (baos.size() > (MAX_BASE64_BYTES + 1024)) {
                        inputStream.close()
                        return@withContext false
                    }
                }
                inputStream.close()
                val fileBytes = baos.toByteArray()
                if (fileBytes.size > MAX_BASE64_BYTES) return@withContext false

                val filename = newsletter.pdfPath.split("/").lastOrNull()
                    ?: "newsletter_${System.currentTimeMillis()}.pdf"
                val base64 = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

                val json = JSONObject().apply {
                    put("type", "newsletter")
                    put("subject", newsletter.title)
                    put("emails", JSONArray(emails))
                    put("names", JSONArray(names))
                    put("pdfBase64", base64)
                    put("filename", filename)
                    put("mimeType", "application/pdf")
                    put("newsletterTitle", newsletter.title)
                    put("message", createNewsletterMessage(newsletter))
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(emailServiceUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        return@withContext JSONObject(responseBody).optBoolean("success", false)
                    } else false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private suspend fun sendNewsletterEmailUsingUrl(
        emails: List<String>,
        names: List<String>,
        newsletter: Newsletter
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("type", "newsletter")
                    put("subject", newsletter.title)
                    put("emails", JSONArray(emails))
                    put("names", JSONArray(names))
                    put("pdfUrl", newsletter.pdfUrl)
                    put("newsletterTitle", newsletter.title)
                    put("message", createNewsletterMessage(newsletter))
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url(emailServiceUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: ""
                        return@withContext JSONObject(responseBody).optBoolean("success", false)
                    } else false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun createNewsletterMessage(newsletter: Newsletter): String {
        return """
            We're excited to share our latest newsletter with you! This edition contains updates about our children, recent activities, upcoming events, and how your support continues to make a difference.

            Thank you for being part of the District 49 family!
        """.trimIndent()
    }

    // -------------------- News & Announcements (UPDATED with notifications) --------------------
    // 🔔 UPDATED: Added context parameter for notifications
    suspend fun addNewsAnnouncement(title: String, content: String, context: Context): Result<NewsAnnouncement> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

            val announcement = NewsAnnouncement(
                id = UUID.randomUUID().toString(),
                title = title,
                content = content,
                createdAt = System.currentTimeMillis(),
                createdBy = currentUser.uid,
                isActive = true,
                priority = 0
            )

            firestore.collection("news_announcements")
                .document(announcement.id)
                .set(announcement)
                .await()

            // 🔔 NEW: Send push notifications to all users with news enabled
            withContext(Dispatchers.IO) {
                try {
                    notificationManager.notifyAllUsersWithPreference(
                        context,
                        NotificationType.NEWS,
                        "📢 $title",
                        content.take(100) + if (content.length > 100) "..." else ""
                    )
                } catch (e: Exception) {
                    // Log but don't fail main operation
                }
            }

            Result.success(announcement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //Fetch latest announcements
    suspend fun getLatestNewsAnnouncements(limit: Int = 5): Result<List<NewsAnnouncement>> {
        return try {
            val snapshot = firestore.collection("news_announcements")
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val announcements = snapshot.documents.mapNotNull { it.toObject(NewsAnnouncement::class.java) }
            Result.success(announcements)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}