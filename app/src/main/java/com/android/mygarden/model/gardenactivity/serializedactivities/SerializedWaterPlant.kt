package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep
import com.android.mygarden.model.plant.SerializedOwnedPlant
import com.google.firebase.Timestamp

/**
 * Represents a serialized "watered plant" activity for Firestore.
 *
 * @property userId Firebase Auth UID of the user who watered the plant.
 * @property type Always "WATERED_PLANT".
 * @property pseudo Public‑facing username of the user who watered the plant.
 * @property timestamp Moment at which the watering action occurred.
 * @property ownedPlant The serialized plant data that was watered.
 */
@Keep
data class SerializedWaterPlant(
    override val userId: String = "",
    override val type: String = "WATERED_PLANT",
    override val pseudo: String = "",
    override val timestamp: Timestamp = Timestamp.now(),
    val ownedPlant: SerializedOwnedPlant = SerializedOwnedPlant(),
) : SerializedActivity()
