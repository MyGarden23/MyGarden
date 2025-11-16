package com.android.mygarden.model.gardenactivity.activitiyclasses

import com.android.mygarden.model.gardenactivity.ActivityType
import com.android.mygarden.model.plant.OwnedPlant
import com.google.firebase.Timestamp
import java.time.Instant

/** Activity for when a user adds a new plant to their garden. */
data class ActivityAddedPlant(
    override val userId: String,
    override val pseudo: String,
    override val timestamp: Timestamp = Timestamp(Instant.now()),
    val ownedPlant: OwnedPlant,
) : GardenActivity() {
  override val type = ActivityType.ADDED_PLANT
}
