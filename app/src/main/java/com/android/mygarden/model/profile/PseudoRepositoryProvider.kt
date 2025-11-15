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

  // Public reference to the current repository.
  var repository: PseudoRepository = _repository
}
