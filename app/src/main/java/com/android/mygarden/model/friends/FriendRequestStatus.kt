package com.android.mygarden.model.friends

import androidx.annotation.Keep

/** Represents the current status of a friend request between two users. */
@Keep
enum class FriendRequestStatus {
  PENDING, // The request has been sent but no response has been received yet
  ACCEPTED, // The request has been accepted by both parties
  REFUSED // The request has been refused by the other party
}
