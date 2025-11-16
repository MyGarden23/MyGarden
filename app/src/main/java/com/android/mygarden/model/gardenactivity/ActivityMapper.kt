package com.android.mygarden.model.gardenactivity

import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityWaterPlant
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedAchievement
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedActivity
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedAddedPlant
import com.android.mygarden.model.plant.FirestoreMapper

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
      is ActivityAddedPlant ->
          SerializedAddedPlant(
              userId = activity.userId,
              type = activity.type.name,
              pseudo = activity.pseudo,
              timestamp = activity.timestamp,
              ownedPlant =
                  FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant(activity.ownedPlant))
      is ActivityAchievement ->
          SerializedAchievement(
              userId = activity.userId,
              type = activity.type.name,
              pseudo = activity.pseudo,
              timestamp = activity.timestamp)
      is ActivityAddFriend -> TODO()
      is ActivityWaterPlant -> TODO()
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
        ActivityAddedPlant(
            userId = serializedActivity.userId,
            pseudo = serializedActivity.pseudo,
            timestamp = serializedActivity.timestamp,
            ownedPlant =
                FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant(serializedActivity.ownedPlant))
      }
      is SerializedAchievement -> {
        ActivityAchievement(
            userId = serializedActivity.userId,
            pseudo = serializedActivity.pseudo,
            timestamp = serializedActivity.timestamp)
      }
    }
  }
}
