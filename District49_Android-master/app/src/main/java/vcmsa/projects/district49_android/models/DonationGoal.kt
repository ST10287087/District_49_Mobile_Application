package vcmsa.projects.district49_android.models

data class DonationGoal(
    val id: String = "",
    val goalAmount: Int = 0,
    val raisedAmount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis(),
    val updatedBy: String = ""
) {
    // No-argument constructor for Firestore
    constructor() : this("", 0, 0, 0L, "")

    fun getProgressPercentage(): Float {
        return if (goalAmount > 0) {
            ((raisedAmount.toFloat() / goalAmount.toFloat()) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }
    }

    fun isGoalReached(): Boolean = raisedAmount >= goalAmount

    fun getRemainingAmount(): Int = (goalAmount - raisedAmount).coerceAtLeast(0)
}