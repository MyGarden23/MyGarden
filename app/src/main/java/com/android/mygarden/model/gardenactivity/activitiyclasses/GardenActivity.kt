package com.android.mygarden.model.gardenactivity.activitiyclasses

import com.android.mygarden.model.gardenactivity.ActivityType
import java.sql.Timestamp

/**
 * Base sealed class representing a user activity in the garden.
 *
 * A [GardenActivity] models a high‑level action performed by a user, such as adding a plant,
 * watering a plant, or earning an achievement. Concrete subclasses specialize this type with
 * additional data when needed (for example, [ActivityAddedPlant] carries an [OwnedPlant]).
 *
 * Instances of this class are typically:
 * - Produced by user actions inside the app
 * - Transformed to and from Firestore via `ActivityMapper`
 * - Displayed in activity feeds or history screens
 */
sealed class GardenActivity {

  /** Firebase Auth UID of the user who performed the activity. */
  abstract val userId: String

  /** Category of the activity, used for branching logic and UI rendering. */
  abstract val type: ActivityType

  /** Public‑facing username or pseudo of the user who performed the activity. */
  abstract val pseudo: String

  /** Timestamp indicating when the activity was created. */
  abstract val createdAt: Timestamp
}
