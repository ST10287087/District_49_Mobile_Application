package vcmsa.projects.district49_android.models

enum class UserRole(val value: String) {
    ADMIN("admin"),
    USER("user");

    companion object {
        fun fromString(value: String): UserRole {
            return values().find { it.value == value } ?: USER
        }
    }
}