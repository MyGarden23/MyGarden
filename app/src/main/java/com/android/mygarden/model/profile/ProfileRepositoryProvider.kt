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

  // Public reference to the current repository.
  // Can be swapped out in tests if needed (e.g., replaced with a fake repo).
  var repository: ProfileRepository = _repository
}
