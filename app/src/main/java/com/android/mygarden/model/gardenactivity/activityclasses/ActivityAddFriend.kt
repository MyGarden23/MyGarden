package com.android.mygarden.model.gardenactivity.activityclasses

import com.android.mygarden.model.gardenactivity.ActivityType
import java.sql.Timestamp

/**
 * Activity emitted when a user adds another user as a friend.
 *
 * This activity can be used to populate a social feed or to build statistics about a user's social
 * interactions inside the app.
 *
 * @property userId Firebase Auth UID of the user who initiated the friend request or friendship.
 * @property pseudo Public‑facing username of the user who added the friend.
 * @property createdAt Moment at which the friend was added.
 * @property friendUserId Firebase Auth UID of the friend that was added.
 */
data class ActivityAddFriend(
    override val userId: String,
    override val pseudo: String,
    override val createdAt: Timestamp = Timestamp(System.currentTimeMillis()),
    val friendUserId: String,
) : GardenActivity() {

  /** Indicates that this activity represents an [ActivityType.ADDED_FRIEND] event. */
  override val type = ActivityType.ADDED_FRIEND
}
