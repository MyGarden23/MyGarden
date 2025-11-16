package com.android.mygarden.model.profile

import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing the current user's profile.
 *
 * Provides access to the authenticated user's profile data, and allows reading and updating it in
 * Firestore or another data source.
 */
interface ProfileRepository {

  /** Returns the Firebase Auth UID of the currently signed-in user. */
  fun getCurrentUserId(): String?

  /**
   * Observes the current user's profile.
   *
   * @return A [Flow] emitting the [Profile] whenever it changes, or `null` if the profile does not
   *   exist.
   */
  fun getProfile(): Flow<Profile?>

  /**
   * Updates (or creates) the user's profile with new information.
   *
   * @param profile The updated [Profile] to persist.
   */
  suspend fun saveProfile(profile: Profile)

  /**
   * Attach the given token to the user's profile in a field called "fcmToken". Create the field if
   * it does not already exist and updates it if it exists. If the user currently has no profile
   * stored, creates one.
   *
   * @param token the new token that will be attached to the profile
   * @return true if the query succeeded otherwise false
   */
  suspend fun attachFCMToken(token: String): Boolean

  /**
   * Return the token currently attached to the current user. Return null if there is an error or if
   * the user has no token attached yet.
   *
   * @return the token of the current user or null if there is none
   */
  suspend fun getFCMToken(): String?

  /**
   * Returns all activities for the current user's profile.
   *
   * For backward compatibility, this queries the global activities collection filtered by the
   * current user's ID.
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
   * This stores the activity in the global activities collection with the user's ID.
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
