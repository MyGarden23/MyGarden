package com.android.mygarden.utils

import com.android.mygarden.model.profile.PseudoRepository

/** Fake implementation of PseudoRepository for testing. */
class FakePseudoRepository() : PseudoRepository {

  /** Counter to track how many times searchPseudoStartingWith has been called */
  var searchCallCount = 0
    private set

  override suspend fun isPseudoAvailable(pseudo: String) = true

  override suspend fun savePseudo(pseudo: String, userId: String) {}

  override suspend fun deletePseudo(pseudo: String) {}

  override suspend fun updatePseudoAtomic(oldPseudo: String?, newPseudo: String, userId: String) {}

  override suspend fun searchPseudoStartingWith(query: String): List<String> {
    searchCallCount++
    return emptyList()
  }

  override suspend fun getUidFromPseudo(pseudo: String) = null

  /** Reset the call counter for a fresh test */
  fun resetCounters() {
    searchCallCount = 0
  }
}
