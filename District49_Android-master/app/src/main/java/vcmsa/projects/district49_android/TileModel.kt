package vcmsa.projects.district49_android

import kotlinx.serialization.Serializable

@Serializable
data class TileModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    var date: Long,
    val offsetX: Float,
    val offsetY: Float,
    val width: Float,
    val height: Float,
    val colorHex: Long,
    val createdBy: String = "" // Track which user created this tile
)