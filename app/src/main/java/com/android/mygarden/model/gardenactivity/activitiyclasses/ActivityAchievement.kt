package com.android.mygarden.model.gardenactivity.activitiyclasses

import com.android.mygarden.model.gardenactivity.ActivityType
import com.google.firebase.Timestamp

/**
 * Activity emitted when a user unlocks or earns an achievement.
 *
 * Additional achievement metadata (such as the achievement identifier or name) can be attached
 * either by extending this type or via associated models when needed.
 *
 * @property userId Firebase Auth UID of the user who earned the achievement.
 * @property pseudo Public‑facing username of the user who earned the achievement.
 * @property timestamp Moment at which the achievement was awarded.
 */
data class ActivityAchievement(
    override val userId: String,
    override val pseudo: String,
    override val timestamp: Timestamp
) : GardenActivity() {

  /** Indicates that this activity represents an [ActivityType.ACHIEVEMENT] event. */
  override val type = ActivityType.ACHIEVEMENT
}
