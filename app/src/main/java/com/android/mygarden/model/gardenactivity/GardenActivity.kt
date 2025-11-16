package com.android.mygarden.model.gardenactivity

import com.android.mygarden.model.plant.OwnedPlant
import com.google.firebase.Timestamp
import java.time.Instant

/**
 * Base sealed class representing an activity in the garden application.
 *
 * Each activity has:
 * - A userId (Firebase Auth UID of who performed the activity)
 * - A type (from ActivityType enum)
 * - A pseudo (username/identifier of who performed the activity)
 * - A timestamp (when the activity occurred)
 *
 * Sealed classes allow for different activity types with their own specific fields and methods
 * while maintaining type safety.
 */
sealed class GardenActivity {
  abstract val userId: String
  abstract val type: ActivityType
  abstract val pseudo: String
  abstract val timestamp: Timestamp

  /** Returns a human-readable description of the activity. */

  /** Activity for when a user adds a new plant to their garden. */
  data class AddedPlant(
      override val userId: String,
      override val pseudo: String,
      override val timestamp: Timestamp = Timestamp(Instant.now()),
      val ownedPlant: OwnedPlant,
  ) : GardenActivity() {
    override val type = ActivityType.ADDED_PLANT
  }

  data class Achievement(
      override val userId: String,
      override val pseudo: String,
      override val timestamp: Timestamp
  ) : GardenActivity() {
    override val type = ActivityType.ACHIEVEMENT
  }
}
