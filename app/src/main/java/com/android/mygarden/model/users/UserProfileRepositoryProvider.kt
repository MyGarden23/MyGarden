package com.android.mygarden.model.users

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Singleton-style provider for the UserProfileRepository.
// Basically, this gives easy access to one shared instance across the app.
object UserProfileRepositoryProvider {

  private var overrideRepo: UserProfileRepository? = null

  // Instead of using `lazy`, we use a nullable field and initialize it on first access.
  private var defaultRepo: UserProfileRepository? = null

  var repository: UserProfileRepository
    get() = overrideRepo ?: getOrCreateDefault()
    set(value) {
      overrideRepo = value
    }

  private fun getOrCreateDefault(): UserProfileRepository {
    if (defaultRepo == null) {
      defaultRepo = UserProfileRepositoryFirestore(Firebase.firestore)
    }
    return defaultRepo!!
  }

  fun resetForTest() {
    overrideRepo = null
    defaultRepo = null // this is the key — avoids early Firebase init across tests
  }
  //  // Lazily create the Firestore-backed repository when first used.
  //  private val _repository: UserProfileRepository by lazy {
  //    UserProfileRepositoryFirestore(Firebase.firestore)
  //  }
  //
  //  // Repository that we can override in tests
  //  private var _overrideRepositoryTest: UserProfileRepository? = null
  //
  //  // Public reference to the current repository.
  //  var repository: UserProfileRepository
  //    get() = _overrideRepositoryTest ?: _repository
  //    set(value) {
  //      _overrideRepositoryTest = value
  //    }
}
