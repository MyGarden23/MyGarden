package com.android.mygarden.model.caretips

object CareTipsRepositoryProvider {

  // private firestore repository lazily created on need only
  private val _repository: CareTipsRepository by lazy { CareTipsRepositoryFirestore() }

  // Allows overriding the repository for testing purposes
  private var _overrideRepositoryTest: CareTipsRepository? = null

  /**
   * Public reference to the current repository Can be swapped in tests with a fake one if needed
   */
  var repository: CareTipsRepository
    get() = _overrideRepositoryTest ?: _repository
    set(value) {
      _overrideRepositoryTest = value
    }
}
