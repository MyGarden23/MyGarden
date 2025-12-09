package com.android.mygarden.utils

import com.android.mygarden.model.notifications.FriendRequestNotifier

class FakeNotifier : FriendRequestNotifier {
  var calls = mutableListOf<Pair<String, String>>()

  override suspend fun notifyRequestSent(targetUid: String, fromPseudo: String) {
    calls.add(targetUid to fromPseudo)
  }
}
