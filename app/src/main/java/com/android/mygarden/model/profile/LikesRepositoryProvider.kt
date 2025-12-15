package com.android.mygarden.model.profile

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Singleton-style provider for the LikesRepository.
// Basically, this gives easy access to one shared instance across the app.
object LikesRepositoryProvider {
  // Lazily create the Firestore-backed repository when first used.
  private val _repository: LikesRepository by lazy { LikesRepositoryFirestore(Firebase.firestore) }

  // Repository that we can override in tests
  private var _overrideRepositoryTest: LikesRepository? = null

  // Public reference to the current repository.
  // Can be swapped out in tests if needed (e.g., replaced with a fake repo).
  var repository: LikesRepository
    get() = _overrideRepositoryTest ?: _repository
    set(value) {
      _overrideRepositoryTest = value
    }
}
