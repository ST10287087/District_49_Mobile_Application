package vcmsa.projects.district49_android.models

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val surname: String = "",
    val phone: String = "", 
    val profilePictureUrl: String = "",
    val userRole: String = "user",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firestore
    constructor() : this("", "", "", "", "", "", "user", 0L, 0L)

    fun getFullName(): String = "$name $surname"

    fun isAdmin(): Boolean = userRole == "admin"
}