package vcmsa.projects.district49_android.ui.nav

import android.content.Intent
import android.view.View
import android.widget.ImageButton
import com.google.firebase.auth.FirebaseAuth
import vcmsa.projects.district49_android.NotificationSettingsActivity
import vcmsa.projects.district49_android.R

object NavBarBinder {

    data class Actions(
        val onBack: () -> Unit = {},
        val onPlus: () -> Unit = {},
        val onNotifications: () -> Unit = {}, // 🔔 Now opens settings
        val onProfile: (isLoggedIn: Boolean) -> Unit = {},
        val onHome: () -> Unit = {},
    )

    fun bind(navRoot: View?, actions: Actions = Actions()) {
        if (navRoot == null) return

        navRoot.findViewById<ImageButton?>(R.id.nav_btn_arrow_back)
            ?.apply {
                isClickable = true
                setOnClickListener { actions.onBack() }
            }

        navRoot.findViewById<ImageButton?>(R.id.nav_btn_plus_circle)
            ?.apply {
                isClickable = true
                setOnClickListener { actions.onPlus() }
            }

        // 🔔 UPDATED: Notifications button opens settings
        navRoot.findViewById<ImageButton?>(R.id.nav_btn_notifications)
            ?.apply {
                isClickable = true
                setOnClickListener { actions.onNotifications() }
                contentDescription = "Notification Settings"
            }

        navRoot.findViewById<ImageButton?>(R.id.nav_btn_profile)
            ?.apply {
                isClickable = true
                setOnClickListener {
                    val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
                    actions.onProfile(isLoggedIn)
                }
            }

        navRoot.findViewById<ImageButton?>(R.id.nav_btn_home)
            ?.apply {
                isClickable = true
                setOnClickListener { actions.onHome() }
            }
    }
}