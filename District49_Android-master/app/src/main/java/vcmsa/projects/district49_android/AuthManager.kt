package vcmsa.projects.district49_android.utils

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import vcmsa.projects.district49_android.models.NotificationPreferences
import vcmsa.projects.district49_android.models.User
import vcmsa.projects.district49_android.models.UserRole

class AuthManager private constructor() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "AuthManager"

        @Volatile
        private var INSTANCE: AuthManager? = null

        fun getInstance(): AuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthManager().also { INSTANCE = it }
            }
        }
    }

    // Register new user and save to Firestore
    suspend fun registerUser(
        email: String,
        password: String,
        name: String,
        surname: String
    ): Result<User> {
        return try {
            // Create Firebase Auth user
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                // Create user data object
                val userData = User(
                    uid = firebaseUser.uid,
                    email = email,
                    name = name,
                    surname = surname,
                    userRole = "user" // Default role
                )

                // Save to Firestore
                firestore.collection("users")
                    .document(firebaseUser.uid)
                    .set(userData)
                    .await()

                // 🔔 Create default notification preferences
                val defaultPrefs = NotificationPreferences(
                    userId = firebaseUser.uid,
                    eventsEnabled = true,
                    newsEnabled = true,
                    newsletterEnabled = true,
                    soundEnabled = true,
                    updatedAt = System.currentTimeMillis()
                )

                firestore.collection("user_notification_preferences")
                    .document(firebaseUser.uid)
                    .set(defaultPrefs)
                    .await()

                Log.d(TAG, "✅ Created default notification preferences for ${firebaseUser.uid}")

                Result.success(userData)
            } else {
                Result.failure(Exception("User creation failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(e)
        }
    }

    // Login user
    suspend fun loginUser(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                // Update last login timestamp
                updateLastLogin(user.uid)

                // 🔔 Ensure notification preferences exist for this user
                ensureNotificationPreferencesExist(user.uid)

                Result.success(user)
            } else {
                Result.failure(Exception("Login failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            Result.failure(e)
        }
    }

    // 🔔 NEW: Ensure notification preferences exist
    private suspend fun ensureNotificationPreferencesExist(uid: String) {
        try {
            val doc = firestore.collection("user_notification_preferences")
                .document(uid)
                .get()
                .await()

            if (!doc.exists()) {
                Log.d(TAG, "Creating default notification preferences for existing user $uid")
                val defaultPrefs = NotificationPreferences(
                    userId = uid,
                    eventsEnabled = true,
                    newsEnabled = true,
                    newsletterEnabled = true,
                    soundEnabled = true,
                    updatedAt = System.currentTimeMillis()
                )

                firestore.collection("user_notification_preferences")
                    .document(uid)
                    .set(defaultPrefs)
                    .await()

                Log.d(TAG, "✅ Created default notification preferences for existing user $uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure notification preferences exist", e)
        }
    }

    // Get current user data from Firestore
    suspend fun getCurrentUserData(): Result<User> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                val document = firestore.collection("users")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        Result.success(user)
                    } else {
                        Result.failure(Exception("User data not found"))
                    }
                } else {
                    Result.failure(Exception("User document does not exist"))
                }
            } else {
                Result.failure(Exception("No authenticated user"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current user data", e)
            Result.failure(e)
        }
    }

    // Get current user role
    suspend fun getCurrentUserRole(): Result<UserRole> {
        return try {
            val userResult = getCurrentUserData()
            if (userResult.isSuccess) {
                val user = userResult.getOrNull()!!
                Result.success(UserRole.fromString(user.userRole))
            } else {
                Result.failure(userResult.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Check if current user is admin
    suspend fun isCurrentUserAdmin(): Result<Boolean> {
        return try {
            val roleResult = getCurrentUserRole()
            if (roleResult.isSuccess) {
                Result.success(roleResult.getOrNull() == UserRole.ADMIN)
            } else {
                Result.failure(roleResult.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update last login timestamp
    private suspend fun updateLastLogin(uid: String) {
        try {
            firestore.collection("users")
                .document(uid)
                .update("lastLogin", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update last login", e)
        }
    }

    // Sign out
    fun signOut() {
        auth.signOut()
    }

    // Get current Firebase user
    fun getCurrentFirebaseUser(): FirebaseUser? = auth.currentUser

    // Promote user to admin (only for existing admins or first setup)
    suspend fun promoteUserToAdmin(userEmail: String): Result<Boolean> {
        return try {
            // First check if current user is admin (skip this check for first admin setup)
            val currentUserResult = getCurrentUserData()
            if (currentUserResult.isSuccess) {
                val currentUser = currentUserResult.getOrNull()!!
                if (!currentUser.isAdmin()) {
                    return Result.failure(Exception("Only admins can promote users"))
                }
            }

            // Find user by email
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .await()

            if (!querySnapshot.isEmpty) {
                val userDoc = querySnapshot.documents.first()
                userDoc.reference.update("userRole", "admin").await()
                Result.success(true)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}