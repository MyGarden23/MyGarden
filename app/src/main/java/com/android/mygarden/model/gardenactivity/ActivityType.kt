package com.android.mygarden.model.gardenactivity

import androidx.annotation.Keep

/**
 * Enumerates the different types of activities that can occur in the garden.
 *
 * These values are typically used for:
 * - Mapping domain activities to their serialized Firestore representation
 * - Rendering different activity types in the UI (e.g. feed, history)
 * - Analytics and filtering of user actions
 */
@Keep
enum class ActivityType {

  /** User has added a new plant to their garden. */
  ADDED_PLANT,

  /** User has watered one or more plants. */
  WATERED_PLANT,

  /** User has unlocked or earned an achievement. */
  ACHIEVEMENT,

  /** User has added a new friend or connection. */
  ADDED_FRIEND,
}
