package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep
import com.google.firebase.Timestamp

/**
 * Represents a serialized achievement activity for Firestore.
 *
 * @property userId The user's Firebase Auth UID.
 * @property type Always "ACHIEVEMENT"
 * @property pseudo The username of who earned the achievement.
 * @property timestamp When the achievement was earned.
 */
@Keep
data class SerializedAchievement(
    override val userId: String = "",
    override val type: String = "ACHIEVEMENT",
    override val pseudo: String = "",
    override val timestamp: Timestamp = Timestamp.now(),
    val achievementName: String = ""
) : SerializedActivity()
