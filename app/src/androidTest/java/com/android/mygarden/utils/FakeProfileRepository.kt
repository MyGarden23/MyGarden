package com.android.mygarden.utils

import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Fake implementation of [ProfileRepository] for testing purposes.
 *
 * This test double provides a simple, in-memory implementation that can be used in instrumented
 * tests to avoid dependencies on actual data sources.
 *
 * @property profile The profile to return from [getProfile]. If null, the repository will emit
 *   null, simulating a scenario where no profile exists.
 */
class FakeProfileRepository(val profile: Profile? = null) : ProfileRepository {
  /**
   * Returns a fake user ID.
   *
   * @return Always returns "fake_uid" for testing purposes.
   */
  override fun getCurrentUserId(): String? = "fake_uid"

  /**
   * Returns a Flow emitting the profile provided in the constructor.
   *
   * @return A Flow that emits the [profile] value once and completes. If [profile] is null, emits
   *   null to simulate a scenario where no profile has been saved yet.
   */
  override fun getProfile(): Flow<Profile?> = flowOf(profile)

  /**
   * Saves a profile (no-op implementation).
   *
   * This method does nothing but succeeds silently, allowing tests to call it without side effects.
   *
   * @param profile The profile to save (ignored in this fake implementation).
   */
  override suspend fun saveProfile(profile: Profile) {
    /* no-op, succeed */
  }

  /**
   * Token handling does nothing.
   */
  override suspend fun attachFCMToken(token: String): Boolean {
    return false
  }

  /**
   * No token is available.
   */
  override suspend fun getFCMToken(): String? {
    return null
  }
}
