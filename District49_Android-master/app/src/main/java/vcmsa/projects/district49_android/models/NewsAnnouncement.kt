package vcmsa.projects.district49_android.models

data class NewsAnnouncement(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val isActive: Boolean = true,
    val priority: Int = 0 // Higher priority shows first
) {
    constructor() : this("", "", "", 0L, "", true, 0)
}