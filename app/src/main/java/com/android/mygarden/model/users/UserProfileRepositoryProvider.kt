package com.android.mygarden.model.users

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

// Singleton-style provider for the UserProfileRepository.
// Basically, this gives easy access to one shared instance across the app.
object UserProfileRepositoryProvider {

  // Repository that we can override in tests
  private var overrideRepo: UserProfileRepository? = null

  // Instead of using `lazy`, we use a nullable field and initialize it on first access.
  private var _overrideRepositoryTest: UserProfileRepository? = null

  // Public reference to the current repository.
  var repository: UserProfileRepository
    get() =
        overrideRepo
            ?: _overrideRepositoryTest
            ?: UserProfileRepositoryFirestore(Firebase.firestore)
    set(value) {
      _overrideRepositoryTest = value
    }
}
