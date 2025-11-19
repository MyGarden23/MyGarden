package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep
import com.android.mygarden.model.plant.SerializedOwnedPlant

/**
 * Represents a serialized "added plant" activity for Firestore.
 *
 * @property userId The user's Firebase Auth UID.
 * @property type Always "ADDED_PLANT"
 * @property pseudo The username of who added the plant.
 * @property timestamp When the plant was added.
 * @property ownedPlant The serialized plant data.
 */
@Keep
data class SerializedAddedPlant(
    override val userId: String = "",
    override val pseudo: String = "",
    override val createdAt: Long = 0,
    val ownedPlant: SerializedOwnedPlant = SerializedOwnedPlant()
) : SerializedActivity() {
  override val type: String = "ADDED_PLANT"
}
