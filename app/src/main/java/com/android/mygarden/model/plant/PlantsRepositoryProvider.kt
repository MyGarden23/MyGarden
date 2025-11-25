package com.android.mygarden.model.plant

/** Provides a singleton instance of the PlantsRepository. */
object PlantsRepositoryProvider {

  // Lazily create the Firestore-backed repository when first used.
  private val _plantsRepository: PlantsRepository by lazy { PlantsRepositoryFirestore() }

  // Container that allowed overriding the repository for testing purposes.
  private var _overrideRepositoryTest: PlantsRepository? = null

  // Public reference to the current repository.
  // Can be swapped out in tests if needed (e.g., replaced with a fake repo).
  var repository: PlantsRepository
    get() = _overrideRepositoryTest ?: _plantsRepository
    set(value) {
      _overrideRepositoryTest = value
    }
}
