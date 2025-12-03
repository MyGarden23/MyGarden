package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep
import com.android.mygarden.model.gardenactivity.ActivityType

/**
 * Represents a serialized achievement activity for Firestore.
 *
 * @property userId The user's Firebase Auth UID.
 * @property type Always "ACHIEVEMENT"
 * @property pseudo The username of who earned the achievement.
 * @property createdAt When the achievement was earned.
 * @property achievementType The type of the achievement serialized using the `.name` function of
 *   the enumerated type.
 */
@Keep
data class SerializedAchievement(
    override val userId: String = "",
    override val pseudo: String = "",
    override val createdAt: Long = 0,
    val achievementType: String = ""
) : SerializedActivity() {
  override val type: String = ActivityType.ACHIEVEMENT.toString()
}
