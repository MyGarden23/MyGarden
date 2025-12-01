package com.android.mygarden.model.friends

import androidx.annotation.Keep

/**
 * Represents a friend request between two users.
 *
 * @property id Unique identifier for this request (Firestore document ID)
 * @property fromUserId The user who sent the request
 * @property toUserId The user who received the request
 * @property status Current status of the request (PENDING, ACCEPTED, REFUSED)
 * @property createdAt Timestamp in milliseconds when the request was created
 */
@Keep
data class FriendRequest(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: FriendRequestStatus = FriendRequestStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
