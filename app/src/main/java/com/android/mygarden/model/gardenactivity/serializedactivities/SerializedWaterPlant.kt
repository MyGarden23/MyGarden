package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep
import com.android.mygarden.model.plant.SerializedOwnedPlant

/**
 * Represents a serialized "watered plant" activity for Firestore.
 *
 * @property userId Firebase Auth UID of the user who watered the plant.
 * @property type Always "WATERED_PLANT".
 * @property pseudo Public‑facing username of the user who watered the plant.
 * @property createdAt Moment at which the watering action occurred.
 * @property ownedPlant The serialized plant data that was watered.
 */
@Keep
data class SerializedWaterPlant(
    override val userId: String = "",
    override val pseudo: String = "",
    override val createdAt: Long = 0,
    val ownedPlant: SerializedOwnedPlant = SerializedOwnedPlant(),
) : SerializedActivity() {
  override val type: String = "WATERED_PLANT"
}
