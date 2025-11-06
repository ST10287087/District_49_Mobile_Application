package vcmsa.projects.district49_android.models

data class SuccessStory(
    val id: String = "",
    val name: String = "",
    val storyParts: List<String> = emptyList(),
    val imageUrl: String = "", // URL to profile image or empty for anonymous
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val order: Int = 0
) {
    constructor() : this("", "", emptyList(), "", 0L, "", 0)
}