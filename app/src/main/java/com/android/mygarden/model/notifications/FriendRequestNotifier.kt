package com.android.mygarden.model.notifications

interface FriendRequestNotifier {
  suspend fun notifyRequestSent(targetUid: String, fromPseudo: String)
}
