package vcmsa.projects.district49_android

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import vcmsa.projects.district49_android.models.SuccessStory
import java.io.ByteArrayOutputStream
import java.util.UUID

class SuccessStoryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "SuccessStoryRepository"
    }

    suspend fun getAllStories(): Result<List<SuccessStory>> {
        return try {
            val snapshot = firestore.collection("success_stories")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val stories = snapshot.documents.mapNotNull {
                it.toObject(SuccessStory::class.java)
            }
            Result.success(stories)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading stories", e)
            Result.failure(e)
        }
    }

    suspend fun addStory(
        name: String,
        storyParts: List<String>,
        imageUri: Uri?,
        context: Context
    ): Result<SuccessStory> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")

            // Upload image if provided
            var imageUrl = ""
            if (imageUri != null) {
                imageUrl = uploadStoryImage(imageUri, context)
            }

            val story = SuccessStory(
                id = UUID.randomUUID().toString(),
                name = name,
                storyParts = storyParts,
                imageUrl = imageUrl,
                createdAt = System.currentTimeMillis(),
                createdBy = currentUser.uid,
                order = 0 // Not using order for sorting anymore
            )

            firestore.collection("success_stories")
                .document(story.id)
                .set(story)
                .await()

            Result.success(story)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding story", e)
            Result.failure(e)
        }
    }

    suspend fun deleteStory(storyId: String, imageUrl: String): Result<Boolean> {
        return try {
            // Delete image from storage if exists
            if (imageUrl.isNotEmpty()) {
                try {
                    storage.getReferenceFromUrl(imageUrl).delete().await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to delete image", e)
                }
            }

            // Delete from Firestore
            firestore.collection("success_stories")
                .document(storyId)
                .delete()
                .await()

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting story", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadStoryImage(imageUri: Uri, context: Context): String {
        val imageId = UUID.randomUUID().toString()
        val imagePath = "success_story_images/$imageId.jpg"
        val storageRef = storage.reference.child(imagePath)

        // Compress image
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val baos = ByteArrayOutputStream()
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
        val imageBytes = baos.toByteArray()

        storageRef.putBytes(imageBytes).await()
        return storageRef.downloadUrl.await().toString()
    }
}