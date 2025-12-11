package com.android.mygarden.model.notifications

/** Interface to notify a user when a friend request is sent. */
fun interface FriendRequestNotifier {

  /**
   * Notifies a user that a friend request has been sent.
   *
   * @param targetUid The UID of the user to notify.
   * @param fromPseudo The pseudo of the user who sent the request.
   */
  suspend fun notifyRequestSent(targetUid: String, fromPseudo: String)
}
