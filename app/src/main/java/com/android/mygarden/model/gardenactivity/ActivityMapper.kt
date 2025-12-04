package com.android.mygarden.model.gardenactivity

import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.gardenactivity.activityclasses.GardenActivity
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedAchievement
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedActivity
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedAddFriend
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedAddedPlant
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedWaterPlant
import com.android.mygarden.model.plant.FirestoreMapper
import java.sql.Timestamp

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
              pseudo = activity.pseudo,
              createdAt = activity.createdAt.time,
              ownedPlant =
                  FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant(activity.ownedPlant))
      is ActivityAchievement ->
          SerializedAchievement(
              userId = activity.userId,
              pseudo = activity.pseudo,
              createdAt = activity.createdAt.time,
              achievementType = activity.achievementType.name)
      is ActivityAddFriend ->
          SerializedAddFriend(
              userId = activity.userId,
              pseudo = activity.pseudo,
              createdAt = activity.createdAt.time,
              friendId = activity.friendUserId,
              friendPseudo = activity.friendPseudo)
      is ActivityWaterPlant ->
          SerializedWaterPlant(
              userId = activity.userId,
              pseudo = activity.pseudo,
              createdAt = activity.createdAt.time,
              ownedPlant =
                  FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant(activity.ownedPlant))
    }
  }

  /**
   * Maps an activity type string to its corresponding [SerializedActivity] class.
   *
   * This function is used to determine which class to deserialize Firestore documents into based on
   * their "type" field.
   *
   * @param type The activity type string (e.g., "ADDED_PLANT", "ACHIEVEMENT", etc.).
   * @return The corresponding [SerializedActivity] class, or null if the type is unknown.
   */
  fun mapTypeToSerializedClass(type: String): Class<out SerializedActivity>? {
    return when (type) {
      ActivityType.ADDED_PLANT.toString() -> SerializedAddedPlant::class.java
      ActivityType.ACHIEVEMENT.toString() -> SerializedAchievement::class.java
      ActivityType.ADDED_FRIEND.toString() -> SerializedAddFriend::class.java
      ActivityType.WATERED_PLANT.toString() -> SerializedWaterPlant::class.java
      else -> null
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
            createdAt = Timestamp(serializedActivity.createdAt),
            ownedPlant =
                FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant(serializedActivity.ownedPlant))
      }
      is SerializedAchievement -> {
        ActivityAchievement(
            userId = serializedActivity.userId,
            pseudo = serializedActivity.pseudo,
            createdAt = Timestamp(serializedActivity.createdAt),
            achievementType = AchievementType.valueOf(serializedActivity.achievementType),
        )
      }
      is SerializedAddFriend -> {
        ActivityAddFriend(
            userId = serializedActivity.userId,
            pseudo = serializedActivity.pseudo,
            createdAt = Timestamp(serializedActivity.createdAt),
            friendUserId = serializedActivity.friendId,
            friendPseudo = serializedActivity.friendPseudo)
      }
      is SerializedWaterPlant -> {
        ActivityWaterPlant(
            userId = serializedActivity.userId,
            pseudo = serializedActivity.pseudo,
            createdAt = Timestamp(serializedActivity.createdAt),
            ownedPlant =
                FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant(serializedActivity.ownedPlant))
      }
    }
  }
}
