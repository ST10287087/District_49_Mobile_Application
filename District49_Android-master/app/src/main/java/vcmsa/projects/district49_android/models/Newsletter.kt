package vcmsa.projects.district49_android.models

data class Newsletter(
    val id: String = "",
    val title: String = "",
    val pdfUrl: String = "",
    val pdfPath: String = "",
    val sentAt: Long = System.currentTimeMillis(),
    val sentBy: String = "",
    val recipientCount: Int = 0
) {
    constructor() : this("", "", "", "", 0L, "", 0)
}