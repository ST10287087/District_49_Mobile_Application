package vcmsa.projects.district49_android

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class NewsletterManager private constructor() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: NewsletterManager? = null

        fun getInstance(): NewsletterManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NewsletterManager().also { INSTANCE = it }
            }
        }
    }

    // Subscribe user to newsletter
    suspend fun subscribeToNewsletter(
        email: String,
        name: String,
        userUid: String
    ): Result<NewsletterSubscriber> {
        return try {
            println("Newsletter Debug: Attempting to subscribe user $userUid with email $email")

            // Check if user already subscribed
            val uidQuery = firestore.collection("newsletter_subscribers")
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("active", true)
                .get()
                .await()

            if (!uidQuery.isEmpty) {
                println("Newsletter Debug: User $userUid already subscribed")
                return Result.failure(Exception("User already subscribed to newsletter"))
            }

            // Create new subscriber with userUid
            val subscriber = NewsletterSubscriber(
                id = "",
                email = email,
                name = name,
                userUid = userUid,
                isActive = true
            )

            println("Newsletter Debug: Creating new subscriber: $subscriber")

            val documentRef = firestore.collection("newsletter_subscribers")
                .add(subscriber)
                .await()

            val subscriberWithId = subscriber.copy(id = documentRef.id)
            documentRef.update("id", documentRef.id).await()

            println("Newsletter Debug: Successfully created subscriber with ID: ${documentRef.id}")
            Result.success(subscriberWithId)
        } catch (e: Exception) {
            println("Newsletter Debug: Error subscribing user: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Check if current user is subscribed
    suspend fun isCurrentUserSubscribed(userUid: String?): Result<Boolean> {
        if (userUid.isNullOrEmpty()) {
            println("Newsletter Debug: userUid is null or empty")
            return Result.success(false)
        }

        return try {
            println("Newsletter Debug: Checking subscription for userUid: $userUid")

            val querySnapshot = firestore.collection("newsletter_subscribers")
                .whereEqualTo("userUid", userUid)
                .whereEqualTo("active", true)
                .get()
                .await()

            println("Newsletter Debug: Query returned ${querySnapshot.documents.size} documents")

            // Log all documents for debugging
            querySnapshot.documents.forEach { doc ->
                val data = doc.data
                println("Newsletter Debug: Found document - userUid: ${data?.get("userUid")}, email: ${data?.get("email")}, isActive: ${data?.get("isActive")}")
            }

            val isSubscribed = !querySnapshot.isEmpty
            println("Newsletter Debug: Final result - isSubscribed: $isSubscribed")

            Result.success(isSubscribed)
        } catch (e: Exception) {
            println("Newsletter Debug: Error checking subscription: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    // Update an existing subscriber with user UID if missing
    suspend fun updateSubscriberUid(subscriberId: String, userUid: String): Result<Boolean> {
        return try {
            val docRef = firestore.collection("newsletter_subscribers").document(subscriberId)
            docRef.update("userUid", userUid).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}