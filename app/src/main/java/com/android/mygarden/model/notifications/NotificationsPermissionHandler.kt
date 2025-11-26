package com.android.mygarden.model.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.edit

/**
 * Handles the notification permission workflow in MainActivity.
 *
 * Workflow:
 * - When users enter the app for the first time, they are prompted for the notification permission.
 * - They are not prompted again afterward, regardless of whether they accepted or denied it.
 * - If the permission is later changed in system settings, the app automatically updates its
 *   behavior accordingly.
 */
object NotificationsPermissionHandler {

  // Shared Preferences used to retrieve information about notifications permission
  private const val PREFS_NAME = "notifications_prefs"
  // The actual key that stores the data
  private const val HAS_DENIED_NOTIFICATIONS = "has_denied_notifications"

  /**
   * Returns true or false depending on whether the user has granted notifications permission
   *
   * @param context the context used to access permission state
   * @return true if the user has granted the app notifications permission, false otherwise
   */
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  fun hasNotificationsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
  }

  /**
   * Returns whether the user has previously denied the notifications permission.
   *
   * Used to avoid repeatedly asking the user for permission and to provide a better UX. The value
   * is stored in SharedPreferences to persist across app launches.
   *
   * @param context the context used to access the Shared Preferences
   */
  fun hasAlreadyDeniedNotificationsPermission(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(HAS_DENIED_NOTIFICATIONS, false)
  }

  /**
   * Updates the stored Shared Preferences indicating whether the user has already denied the
   * notifications permission. Works with hasAlreadyDeniedNotificationsPermission().
   *
   * @param context the context used to access the Shared Preferences
   * @param value true if the user has already denied notifications permission, false otherwise
   */
  fun setHasAlreadyDeniedNotificationsPermission(context: Context, value: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(HAS_DENIED_NOTIFICATIONS, value) }
  }
}
