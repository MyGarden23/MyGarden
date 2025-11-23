package com.android.mygarden.model.plant

/** Provides a singleton instance of the PlantsRepository. */
object PlantsRepositoryProvider {
  private val _plantsRepository: PlantsRepository by lazy { PlantsRepositoryFirestore() }

  private var _overrideTesting: PlantsRepository? = null

  // Public reference to the current repository.
  // Can be swapped out in tests if needed (e.g., replaced with a fake repo).
  var repository: PlantsRepository
    get() = _overrideTesting ?: _plantsRepository
    set(value) {
      _overrideTesting = value
    }
}
