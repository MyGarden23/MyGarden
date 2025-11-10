package com.android.mygarden.model.notifications

import androidx.core.content.edit
import com.android.mygarden.model.profile.ProfileRepositoryFirestore
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A [FirebaseMessagingService] implementation responsible for handling Firebase Cloud Messaging
 * (FCM) interactions with the app.
 */
class PushNotificationsService : FirebaseMessagingService() {

  /**
   * Called when a new token for the Firebase project is generated. This is invoked after app
   * install when a token is first generated, and again if the token changes. It handles new tokens
   * by first storing them locally on the device and try to attach the token to the user's Firestore
   * profile (see [FCMTokenSyncer]).
   *
   * @param token the token used for sending messages to this application instance. This token is
   *   the same as the one retrieved by FirebaseMessaging.getToken()
   */
  override fun onNewToken(token: String) {
    super.onNewToken(token)

    val profileRepo = ProfileRepositoryProvider.repository as ProfileRepositoryFirestore

    CoroutineScope(Dispatchers.IO).launch {
      // Try to attach the new token to the user's Firestore profile
      val success = profileRepo.attachFCMToken(token)

      // If there is none, save it locally until the user logs in
      if (!success) {
        getSharedPreferences("notifications_prefs", MODE_PRIVATE).edit {
          putString("fcm_token", token)
        }
      }
    }
  }
}
