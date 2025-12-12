package com.android.mygarden.model.users

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Singleton-style provider for the UserProfileRepository.
// Basically, this gives easy access to one shared instance across the app.
object UserProfileRepositoryProvider {

  // Repository that we can override in tests
  private var overrideRepo: UserProfileRepository? = null

  // Instead of using `lazy`, we use a nullable field and initialize it on first access.
  private var defaultRepo: UserProfileRepository? = null

  // Public reference to the current repository.
  var repository: UserProfileRepository
    get() = overrideRepo ?: getOrCreateDefault()
    set(value) {
      overrideRepo = value
    }

  /** Reset the repository if needed. */
  private fun getOrCreateDefault(): UserProfileRepository {
    if (defaultRepo == null) {
      defaultRepo = UserProfileRepositoryFirestore(Firebase.firestore)
    }
    return defaultRepo!!
  }
}
