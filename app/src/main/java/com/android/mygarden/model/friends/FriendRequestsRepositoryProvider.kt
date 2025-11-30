package com.android.mygarden.model.friends

/**
 * Singleton-style provider for the FriendRequestsRepository.
 *
 * Provides easy access to one shared instance across the app.
 */
object FriendRequestsRepositoryProvider {

  // Lazily create the Firestore-backed repository when first used
  private val _repository: FriendRequestsRepository by lazy { FriendRequestsRepositoryFirestore() }

  // Container that allows overriding the repository for testing purposes
  private var _overrideRepositoryTest: FriendRequestsRepository? = null

  /**
   * Public reference to the current repository.
   *
   * Can be swapped out in tests if needed (e.g., replaced with a fake repo).
   */
  var repository: FriendRequestsRepository
    get() = _overrideRepositoryTest ?: _repository
    set(value) {
      _overrideRepositoryTest = value
    }
}
