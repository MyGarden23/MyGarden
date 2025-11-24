package com.android.mygarden.model.gardenactivity

// Singleton-style provider for the ActivityRepository.
// Basically, this gives easy access to one shared instance across the app.
object ActivityRepositoryProvider {

  // Lazily create the Firestore-backed repository when first used.
  private val _repository: ActivityRepository by lazy { ActivityRepositoryFirestore() }

  // Public reference to the current repository.
  // Can be swapped out in tests if needed (e.g., replaced with a fake repo).
  var repository: ActivityRepository = _repository
}
