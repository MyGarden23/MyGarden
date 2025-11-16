package com.android.mygarden.model.gardenactivity.activitiyclasses

import com.android.mygarden.model.gardenactivity.ActivityType
import com.google.firebase.Timestamp

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
}
