package com.android.mygarden.model.profile

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
}
