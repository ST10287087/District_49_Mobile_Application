package vcmsa.projects.district49_android

data class NewsletterSubscriber(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val subscribedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val userUid: String? = null // null if subscriber is not a registered user
) {
    constructor() : this("", "", "", 0L, true, null)
}