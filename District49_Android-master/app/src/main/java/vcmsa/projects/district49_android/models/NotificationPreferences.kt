package vcmsa.projects.district49_android.models

data class NotificationPreferences(
    val userId: String = "",
    val eventsEnabled: Boolean = true,
    val newsEnabled: Boolean = true,
    val newsletterEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", true, true, true, true, 0L)
}