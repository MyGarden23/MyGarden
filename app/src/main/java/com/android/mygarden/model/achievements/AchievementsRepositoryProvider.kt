package com.android.mygarden.model.achievements

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

object AchievementsRepositoryProvider {

  // Private Firestore repository lazily created on need only
  private val _repository: AchievementsRepository by lazy {
    AchievementsRepositoryFirestore(Firebase.firestore)
  }

  // Allows overriding the repository for testing purposes
  private var _overrideRepositoryTest: AchievementsRepository? = null

  /**
   * Public reference to the current repository Can be swapped in tests with a fake one if needed
   */
  var repository: AchievementsRepository
    get() = _overrideRepositoryTest ?: _repository
    set(value) {
      _overrideRepositoryTest = value
    }
}
