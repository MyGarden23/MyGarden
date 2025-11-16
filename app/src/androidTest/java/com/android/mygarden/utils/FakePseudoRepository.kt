package com.android.mygarden.utils

import com.android.mygarden.model.profile.PseudoRepository

/** Fake implementation of PseudoRepository for testing. */
class FakePseudoRepository() : PseudoRepository {
  override suspend fun isPseudoAvailable(pseudo: String) = true

  override suspend fun savePseudo(pseudo: String) {}

  override suspend fun deletePseudo(pseudo: String) {}
}
