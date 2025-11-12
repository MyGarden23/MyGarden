package com.android.mygarden.model.notifications

import android.content.Context
import androidx.core.content.edit
import com.android.mygarden.model.profile.ProfileRepositoryProvider

/**
 * Utility object responsible for synchronizing locally stored FCM tokens with the user's Firestore
 * profile.
 *
 * This object is used when a new FCM token was generated while no user was logged in (see
 * [PushNotificationsService]). Once the user successfully signs in, this object can be invoked to
 * upload the pending token to Firestore and clean up local Shared Preferences.
 */
object FCMTokenSyncer {

  /**
   * Attempts to synchronize any locally stored FCM token with the user's Firestore profile.
   *
   * @param context The application [Context] used to access SharedPreferences.
   */
  suspend fun trySync(context: Context) {
    // If the user currently has has no account on Firestore then do nothing
    val profileRepo = ProfileRepositoryProvider.repository
    profileRepo.getCurrentUserId() ?: return

    // If there is no pending token to upload to Firestore then do nothing
    val prefs = context.getSharedPreferences("notifications_prefs", Context.MODE_PRIVATE)
    val token = prefs.getString("fcm_token", null) ?: return

    // If there is already the same token on Firestore then do nothing
    val remoteToken = profileRepo.getFCMToken()
    if ((remoteToken != null) && (remoteToken == token)) return

    // Here we know that the user has a Firestore profile linked with a different or no token, hence
    // hence update it
    val success = profileRepo.attachFCMToken(token)

    // Delete local shared preference that was holding the new token if it succeeds
    if (success) {
      prefs.edit { remove("fcm_token") }
    }
  }
}
