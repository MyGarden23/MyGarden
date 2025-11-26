package com.android.mygarden.utils

import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of [ActivityRepository] for unit testing purposes.
 *
 * This test double provides a simple, in-memory implementation that can be used in unit tests to
 * avoid dependencies on actual data sources.
 */
class FakeActivityRepository : ActivityRepository {
  /** List of activities added during testing */
  val addedActivities = mutableListOf<GardenActivity>()

  /**
   * Returns a fake user ID.
   *
   * @return Always returns "fake-uid" for testing purposes.
   */
  override fun getCurrentUserId(): String = "fake-uid"

  /**
   * Returns a Flow emitting all activities.
   *
   * @return A Flow that emits the list of added activities.
   */
  override fun getActivities(): Flow<List<GardenActivity>> = flowOf(addedActivities)

  /**
   * Returns a Flow emitting activities for a specific user.
   *
   * @param userId The user ID to filter activities by.
   * @return A Flow that emits the filtered list of activities.
   */
  override fun getActivitiesForUser(userId: String): Flow<List<GardenActivity>> =
      flowOf(addedActivities.filter { it.userId == userId })

  /**
   * Returns a Flow emitting feed activities for a list of users.
   *
   * @param userIds The list of user IDs to filter activities by.
   * @param limit The maximum number of activities to return.
   * @return A Flow that emits the filtered and limited list of activities.
   */
  override fun getFeedActivities(userIds: List<String>, limit: Int): Flow<List<GardenActivity>> =
      flowOf(addedActivities.filter { it.userId in userIds }.take(limit))

  /**
   * Adds an activity to the repository.
   *
   * @param activity The activity to add.
   */
  override suspend fun addActivity(activity: GardenActivity) {
    addedActivities.add(activity)
  }

  /** Clears all activities from the repository. */
  override fun cleanup() {
    addedActivities.clear()
  }
}
