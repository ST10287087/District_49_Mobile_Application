package vcmsa.projects.district49_android

data class GalleryImage(
    val id: String = "",
    val imageUrl: String = "",
    val uploadedAt: Long = 0L,
    val uploadedBy: String = ""
) {
    constructor() : this("", "", 0L, "")
}