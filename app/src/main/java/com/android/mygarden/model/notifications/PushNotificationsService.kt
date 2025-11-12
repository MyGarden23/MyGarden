package com.android.mygarden.model.notifications

import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A [FirebaseMessagingService] implementation responsible for handling Firebase Cloud Messaging
 * (FCM) interactions with the app.
 */
class PushNotificationsService : FirebaseMessagingService() {

  /** Tracker for the app's foreground/background state. */
  companion object {
    var isAppInForeGround = false
  }

  var testDispatcher: CoroutineDispatcher? = null

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

    val profileRepo = ProfileRepositoryProvider.repository
    val dispatcher = testDispatcher ?: Dispatchers.IO

    CoroutineScope(dispatcher).launch {
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

  /**
   * Called when the app receives a new notification. The notification is not displayed when the app
   * is running but only when the app is in background, because a popup appears instead.
   *
   * @param message the message containing notification/data payload sent from FCM
   */
  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)

    // If the app is currently on the foreground then do not show the received message
    if (isAppInForeGround) {
      return
    }

    // The notification is shown otherwise by default
  }
}

/**
 * Lifecycle observer that track the app's foreground/background state. Used to show notifications
 * only when the app is in background (otherwise show the WaterPlantPopup).
 */
object AppLifecycleObserver : DefaultLifecycleObserver {

  override fun onStart(owner: LifecycleOwner) {
    super.onStart(owner)
    PushNotificationsService.isAppInForeGround = true
  }

  override fun onStop(owner: LifecycleOwner) {
    super.onStop(owner)
    PushNotificationsService.isAppInForeGround = false
  }
}
