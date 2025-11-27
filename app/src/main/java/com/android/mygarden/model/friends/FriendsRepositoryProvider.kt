package com.android.mygarden.model.friends

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Singleton-style provider for the FriendsRepository.
// Basically, this gives easy access to one shared instance across the app.
object FriendsRepositoryProvider {

  // Lazily create the Firestore-backed repository when first used.
  private val _repository: FriendsRepository by lazy {
    FriendsRepositoryFirestore(Firebase.firestore)
  }

  // Repository that we can override in tests
  private var _overrideRepositoryTest: FriendsRepository? = null

  // Public reference to the current repository.
  // Can be swapped out in tests if needed (e.g., replaced with a fake repo).
  var repository: FriendsRepository
    get() = _overrideRepositoryTest ?: _repository
    set(value) {
      _overrideRepositoryTest = value
    }
}
