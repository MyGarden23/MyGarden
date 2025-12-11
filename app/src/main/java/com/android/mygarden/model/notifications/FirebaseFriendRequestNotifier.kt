package com.android.mygarden.model.notifications

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

private const val send_friend_request_server_function = "send_friend_request_notification"
private const val targetUidKey = "targetUid"
private const val fromPseudoKey = "fromPseudo"

/** Notifier implemented by Firebase functions. */
class FirebaseFriendRequestNotifier : FriendRequestNotifier {

  /**
   * Notifies a user that a friend request has been sent.
   *
   * @param targetUid The UID of the user to notify.
   * @param fromPseudo The pseudo of the user who sent the request.
   * @throws java.io.IOException If the HTTPS request failed to connect.
   * @throws FirebaseFunctionsException If the request connected, but the function returned an
   *   error.
   */
  override suspend fun notifyRequestSent(targetUid: String, fromPseudo: String) {
    FirebaseFunctions.getInstance()
        .getHttpsCallable(send_friend_request_server_function)
        .call(mapOf(targetUidKey to targetUid, fromPseudoKey to fromPseudo))
        .await()
  }
}
