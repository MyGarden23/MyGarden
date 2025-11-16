package com.android.mygarden.model.gardenactivity

import androidx.annotation.Keep
import com.android.mygarden.model.plant.FirestoreMapper
import com.android.mygarden.model.plant.SerializedOwnedPlant
import com.google.firebase.Timestamp

/**
 * Base serialized activity compatible for Firestore.
 *
 * This serves as the base for all serialized activity types.
 *
 * @property userId The Firebase Auth UID of the user who performed this activity.
 * @property type The type of activity as a string.
 * @property pseudo The username of who performed the activity.
 * @property timestamp The Firestore timestamp of when the activity occurred.
 */
@Keep
abstract class SerializedActivity {
  abstract val userId: String
  abstract val type: String
  abstract val pseudo: String
  abstract val timestamp: Timestamp
}

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
    override val timestamp: Timestamp = Timestamp.now()
) : SerializedActivity()

/**
 * Utility object providing mapping functions between [GardenActivity] instances and their
 * Firestore-serializable [SerializedActivity] representations.
 *
 * These conversion functions are needed because [GardenActivity] uses types that Firestore cannot
 * directly store.
 */
object ActivityMapper {

  /**
   * Converts a [GardenActivity] into its Firestore-compatible [SerializedActivity] representation.
   *
   * @param activity The activity instance to serialize.
   * @return A [SerializedActivity] ready to be stored in Firestore.
   */
  fun fromActivityToSerializedActivity(activity: GardenActivity): SerializedActivity {
    return when (activity) {
      is GardenActivity.AddedPlant ->
          SerializedAddedPlant(
              userId = activity.userId,
              type = activity.type.name,
              pseudo = activity.pseudo,
              timestamp = activity.timestamp,
              ownedPlant =
                  FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant(activity.ownedPlant))
      is GardenActivity.Achievement ->
          SerializedAchievement(
              userId = activity.userId,
              type = activity.type.name,
              pseudo = activity.pseudo,
              timestamp = activity.timestamp)
    }
  }

  /**
   * Converts a Firestore-compatible [SerializedActivity] back into a [GardenActivity].
   *
   * This function handles invalid or unknown enum values safely by returning null if the activity
   * cannot be reconstructed.
   *
   * @param serializedActivity The serialized activity data retrieved from Firestore.
   * @return The corresponding [GardenActivity], or null if parsing fails.
   */
  fun fromSerializedActivityToActivity(serializedActivity: SerializedActivity): GardenActivity? {
    return when (serializedActivity) {
      is SerializedAddedPlant -> {
        GardenActivity.AddedPlant(
            userId = serializedActivity.userId,
            pseudo = serializedActivity.pseudo,
            timestamp = serializedActivity.timestamp,
            ownedPlant =
                FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant(serializedActivity.ownedPlant))
      }
      is SerializedAchievement -> {
        GardenActivity.Achievement(
            userId = serializedActivity.userId,
            pseudo = serializedActivity.pseudo,
            timestamp = serializedActivity.timestamp)
      }
      else -> null // Unknown serialized activity type
    }
  }
}
