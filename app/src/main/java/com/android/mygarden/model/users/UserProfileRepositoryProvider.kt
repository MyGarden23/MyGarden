package com.android.mygarden.model.users

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Singleton-style provider for the UserProfileRepository.
// Basically, this gives easy access to one shared instance across the app.
object UserProfileRepositoryProvider {

  // Lazily create the Firestore-backed repository when first used.
  private val _repository: UserProfileRepository by lazy {
    UserProfileRepositoryFirestore(Firebase.firestore)
  }

  // Repository that we can override in tests
  private var _overrideRepositoryTest: UserProfileRepository? = null

  // Public reference to the current repository.
  var repository: UserProfileRepository
    get() = _overrideRepositoryTest ?: _repository
    set(value) {
      _overrideRepositoryTest = value
    }
}
