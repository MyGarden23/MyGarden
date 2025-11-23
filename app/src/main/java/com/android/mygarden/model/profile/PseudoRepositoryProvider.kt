package com.android.mygarden.model.profile

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Singleton-style provider for the PseudoRepository.
// Basically, this gives easy access to one shared instance across the app.
object PseudoRepositoryProvider {

  // Lazily create the Firestore-backed repository when first used.
  private val _repository: PseudoRepository by lazy {
    PseudoRepositoryFirestore(Firebase.firestore)
  }

  // Repository that we can override in tests
  private var _overrideTesting: PseudoRepository? = null

  // Public reference to the current repository.
  var repository: PseudoRepository
    get() = _overrideTesting ?: _repository
    set(value) {
      _overrideTesting = value
    }
}
