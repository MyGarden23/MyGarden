package com.android.mygarden.model.achievements

import kotlinx.coroutines.flow.Flow

/** Repository for reading and updating user achievement progress. */
interface AchievementsRepository {

  /** Returns the UID of the currently authenticated user, or null if none. */
  fun getCurrentUserId(): String?

  /**
   * Retrieves the progress of a specific achievement for the given user.
   *
   * @param userId ID of the user.
   * @param achievementType The achievement to retrieve.
   * @return The user's progress, or null if it does not exist.
   */
  suspend fun getUserAchievementProgress(
      userId: String,
      achievementType: AchievementType
  ): UserAchievementProgress?

  /**
   * Streams all achievement progress entries for the given user in a Flow.
   *
   * @param userId ID of the user.
   * @return A flow emitting the list of achievements whenever data changes.
   */
  fun getAllUserAchievementProgress(userId: String): Flow<List<UserAchievementProgress>>

  /**
   * Sets the value of an achievement for the given user, overwriting any existing value.
   *
   * @param userId ID of the user.
   * @param achievementType The type of achievement to update.
   * @param value The new value to assign.
   */
  suspend fun setAchievementValue(userId: String, achievementType: AchievementType, value: Int)

  /**
   * Creates the initial achievement entries for a new user that arrived on the app.
   *
   * @param userId ID of the user.
   */
  suspend fun initializeAchievementsForNewUser(userId: String)

  /**
   * Updates an achievement with a new value if any progress has been made. Typically called after
   * computing the new progress.
   *
   * @param userId ID of the user.
   * @param achievementType The type of achievement to update.
   * @param newValue The updated value.
   */
  suspend fun updateAchievementValue(
      userId: String,
      achievementType: AchievementType,
      newValue: Int
  )

  /** Cleanup method to remove active listeners before logout. */
  fun cleanup()
}
