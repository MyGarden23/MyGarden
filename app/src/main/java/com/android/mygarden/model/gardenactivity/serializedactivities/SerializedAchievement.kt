package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep

/**
 * Represents a serialized achievement activity for Firestore.
 *
 * @property userId The user's Firebase Auth UID.
 * @property type Always "ACHIEVEMENT"
 * @property pseudo The username of who earned the achievement.
 * @property createdAt When the achievement was earned.
 */
@Keep
data class SerializedAchievement(
    override val userId: String = "",
    override val pseudo: String = "",
    override val createdAt: Long = 0,
    val achievementName: String = ""
) : SerializedActivity() {
  override val type: String = "ACHIEVEMENT"
}
