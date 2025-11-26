package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep
import com.android.mygarden.model.gardenactivity.ActivityType

/**
 * Represents a serialized "add friend" activity for Firestore.
 *
 * @property userId Firebase Auth UID of the user who initiated the friend request or friendship.
 * @property type Always "ADDED_FRIEND".
 * @property pseudo Public‑facing username of the user who added the friend.
 * @property createdAt Moment at which the friend was added.
 * @property friendId Firebase Auth UID (or other unique identifier) of the friend that was added.
 */
@Keep
data class SerializedAddFriend(
    override val userId: String = "",
    override val pseudo: String = "",
    override val createdAt: Long = 0,
    val friendId: String = "",
) : SerializedActivity() {
  override val type: String = ActivityType.ADDED_FRIEND.toString()
}
