package com.android.mygarden.model.gardenactivity

import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing garden activities.
 *
 * Provides access to garden activities for users, including reading and creating activities in
 * Firestore or another data source.
 */
interface ActivityRepository {

  /** Returns the Firebase Auth UID of the currently signed-in user. */
  fun getCurrentUserId(): String?

  /**
   * Returns all activities for the current user's profile.
   *
   * @return A [Flow] emitting a list of [GardenActivity] objects whenever the activities change.
   */
  fun getActivities(): Flow<List<GardenActivity>>

  /**
   * Returns activities from a specific user.
   *
   * @param userId The Firebase Auth UID of the user whose activities to retrieve.
   * @return A [Flow] emitting a list of [GardenActivity] objects for that user.
   */
  fun getActivitiesForUser(userId: String): Flow<List<GardenActivity>>

  /**
   * Returns a feed of activities from multiple users (e.g., friends feed).
   *
   * @param userIds List of Firebase Auth UIDs whose activities to include in the feed.
   * @param limit Maximum number of activities to return (default 50).
   * @return A [Flow] emitting a list of [GardenActivity] objects sorted by most recent.
   */
  fun getFeedActivities(userIds: List<String>, limit: Int = 50): Flow<List<GardenActivity>>

  /**
   * Adds a new activity to the current user's profile.
   *
   * This stores the activity in the user's activities subcollection.
   *
   * @param activity The [GardenActivity] to add to the profile.
   */
  suspend fun addActivity(activity: GardenActivity)

  /**
   * Cleans up any active listeners or resources.
   *
   * This should be called before signing out to prevent PERMISSION_DENIED errors from Firestore
   * listeners attempting to access data after the user is logged out.
   */
  fun cleanup()
}
