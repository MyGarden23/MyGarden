package com.android.mygarden.model.gardenactivity.activitiyclasses

import com.android.mygarden.model.gardenactivity.ActivityType
import com.android.mygarden.model.plant.OwnedPlant
import java.sql.Timestamp

/**
 * Activity emitted when a user adds a new plant to their garden.
 *
 * This activity is typically created after successfully persisting the new [OwnedPlant] in the
 * user's collection, and can then be displayed in an activity feed or shared with friends.
 *
 * @property userId Firebase Auth UID of the user who added the plant.
 * @property pseudo Public‑facing username of the user who added the plant.
 * @property createdAt Moment at which the plant was added. Defaults to the current time if not
 *   provided.
 * @property ownedPlant The domain model of the plant that was added to the user's garden.
 */
data class ActivityAddedPlant(
    override val userId: String,
    override val pseudo: String,
    override val createdAt: Timestamp = Timestamp(System.currentTimeMillis()),
    val ownedPlant: OwnedPlant,
) : GardenActivity() {

  /** Indicates that this activity represents an [ActivityType.ADDED_PLANT] event. */
  override val type = ActivityType.ADDED_PLANT
}
