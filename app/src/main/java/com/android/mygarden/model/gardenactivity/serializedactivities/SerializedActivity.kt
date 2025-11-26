package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep

/**
 * Base serialized activity compatible for Firestore.
 *
 * This serves as the base for all serialized activity types.
 *
 * @property userId The Firebase Auth UID of the user who performed this activity.
 * @property type The type of activity as a string.
 * @property pseudo The username of who performed the activity.
 * @property createdAt The Firestore timestamp of when the activity occurred.
 */
@Keep
sealed class SerializedActivity {
  abstract val userId: String
  abstract val type: String
  abstract val pseudo: String
  abstract val createdAt: Long
}
