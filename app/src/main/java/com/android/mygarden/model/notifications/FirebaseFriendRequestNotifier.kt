package com.android.mygarden.model.notifications

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

/** Notifier implemented by Firebase functions. */
class FirebaseFriendRequestNotifier : FriendRequestNotifier {
  override suspend fun notifyRequestSent(targetUid: String, fromPseudo: String) {
    FirebaseFunctions.getInstance()
        .getHttpsCallable("send_friend_request_notification")
        .call(mapOf("targetUid" to targetUid, "fromPseudo" to fromPseudo))
        .await()
  }
}
