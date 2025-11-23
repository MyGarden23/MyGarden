package com.android.mygarden.model.profile

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Singleton-style provider for the ProfileRepository.
// Basically, this gives easy access to one shared instance across the app.
object ProfileRepositoryProvider {

  // Lazily create the Firestore-backed repository when first used.
  private val _repository: ProfileRepository by lazy {
    ProfileRepositoryFirestore(Firebase.firestore)
  }

  // Repository that we can override in tests
  private var _overrideTesting: ProfileRepository? = null

  // Public reference to the current repository.
  // Can be swapped out in tests if needed (e.g., replaced with a fake repo).
  var repository: ProfileRepository
    get() = _overrideTesting ?: _repository
    set(value) {
      _overrideTesting = value
    }
}
