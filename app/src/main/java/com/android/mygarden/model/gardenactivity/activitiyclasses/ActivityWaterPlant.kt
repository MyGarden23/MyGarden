package com.android.mygarden.model.gardenactivity.activitiyclasses

import com.android.mygarden.model.gardenactivity.ActivityType
import com.android.mygarden.model.plant.OwnedPlant
import com.google.firebase.Timestamp

/**
 * Activity emitted when a user waters a plant in their garden.
 *
 * This models the watering action itself and can be used to build watering histories or streaks. If
 * you need to track which specific plant was watered, extend this type with an identifier to that
 * plant.
 *
 * @property userId Firebase Auth UID of the user who watered the plant.
 * @property pseudo Public‑facing username of the user who watered the plant.
 * @property timestamp Moment at which the watering action occurred.
 */
data class ActivityWaterPlant(
    override val userId: String,
    override val pseudo: String,
    override val timestamp: Timestamp,
    val ownedPlant: OwnedPlant,
) : GardenActivity() {

  /** Indicates that this activity represents a [ActivityType.WATERED_PLANT] event. */
  override val type = ActivityType.WATERED_PLANT
}
