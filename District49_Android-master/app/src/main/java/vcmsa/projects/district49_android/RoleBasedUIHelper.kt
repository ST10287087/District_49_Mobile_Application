// RoleBasedUIHelper.kt
package vcmsa.projects.district49_android.utils

import android.app.Activity
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RoleBasedUIHelper {
    companion object {
        /**
         * Show/hide views based on admin role
         * @param activity The activity context
         * @param adminOnlyViews List of views that should only be visible to admins
         * @param callback Optional callback when role check is complete
         */
        fun setupAdminOnlyViews(
            activity: Activity,
            adminOnlyViews: List<View>,
            callback: ((isAdmin: Boolean) -> Unit)? = null
        ) {
            if (activity is androidx.lifecycle.LifecycleOwner) {
                activity.lifecycleScope.launch {
                    try {
                        val authManager = AuthManager.getInstance()
                        val isAdminResult = authManager.isCurrentUserAdmin()

                        val isAdmin = isAdminResult.getOrDefault(false)

                        // Update UI on main thread
                        activity.runOnUiThread {
                            adminOnlyViews.forEach { view ->
                                view.visibility = if (isAdmin) View.VISIBLE else View.GONE
                            }
                            callback?.invoke(isAdmin)
                        }
                    } catch (e: Exception) {
                        // Hide admin views if error occurs
                        activity.runOnUiThread {
                            adminOnlyViews.forEach { view ->
                                view.visibility = View.GONE
                            }
                            callback?.invoke(false)
                        }
                    }
                }
            }
        }

        /**
         * Hide views for normal users (opposite of admin-only)
         * @param activity The activity context
         * @param userOnlyViews List of views that should only be visible to normal users
         * @param callback Optional callback when role check is complete
         */
        fun setupUserOnlyViews(
            activity: Activity,
            userOnlyViews: List<View>,
            callback: ((isUser: Boolean) -> Unit)? = null
        ) {
            if (activity is androidx.lifecycle.LifecycleOwner) {
                activity.lifecycleScope.launch {
                    try {
                        val authManager = AuthManager.getInstance()
                        val isAdminResult = authManager.isCurrentUserAdmin()

                        val isAdmin = isAdminResult.getOrDefault(false)
                        val isUser = !isAdmin

                        // Update UI on main thread
                        activity.runOnUiThread {
                            userOnlyViews.forEach { view ->
                                view.visibility = if (isUser) View.VISIBLE else View.GONE
                            }
                            callback?.invoke(isUser)
                        }
                    } catch (e: Exception) {
                        // Show user views if error occurs (default behavior)
                        activity.runOnUiThread {
                            userOnlyViews.forEach { view ->
                                view.visibility = View.VISIBLE
                            }
                            callback?.invoke(true)
                        }
                    }
                }
            }
        }

        /**
         * Setup role-based navigation or functionality
         * @param activity The activity context
         * @param onAdminAccess Function to execute for admin users
         * @param onUserAccess Function to execute for normal users
         */
        fun executeBasedOnRole(
            activity: Activity,
            onAdminAccess: () -> Unit,
            onUserAccess: () -> Unit
        ) {
            if (activity is androidx.lifecycle.LifecycleOwner) {
                activity.lifecycleScope.launch {
                    try {
                        val authManager = AuthManager.getInstance()
                        val isAdminResult = authManager.isCurrentUserAdmin()

                        val isAdmin = isAdminResult.getOrDefault(false)

                        activity.runOnUiThread {
                            if (isAdmin) {
                                onAdminAccess()
                            } else {
                                onUserAccess()
                            }
                        }
                    } catch (e: Exception) {
                        // Default to user access if error occurs
                        activity.runOnUiThread {
                            onUserAccess()
                        }
                    }
                }
            }
        }
    }
}