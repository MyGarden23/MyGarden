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
}
