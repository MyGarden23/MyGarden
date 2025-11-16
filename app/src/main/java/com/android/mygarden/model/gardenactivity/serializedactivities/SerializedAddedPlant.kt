package com.android.mygarden.model.gardenactivity.serializedactivities

import androidx.annotation.Keep
import com.android.mygarden.model.plant.SerializedOwnedPlant
import com.google.firebase.Timestamp

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
    override val type: String = "ADDED_PLANT",
    override val pseudo: String = "",
    override val timestamp: Timestamp = Timestamp.now(),
    val ownedPlant: SerializedOwnedPlant = SerializedOwnedPlant()
) : SerializedActivity()
