package com.android.mygarden.model.profile

/** Repository interface for managing uniqueness of pseudos in profiles */
interface PseudoRepository {

  /**
   * Checks if a pseudo is available for use.
   *
   * @param pseudo The pseudo to check.
   * @return True if the pseudo is available, false otherwise.
   */
  suspend fun isPseudoAvailable(pseudo: String): Boolean

  /**
   * Saves a pseudo to the repository.
   *
   * @param pseudo The pseudo to save.
   * @throws IllegalStateException if the pseudo already exists.
   */
  suspend fun savePseudo(pseudo: String)

  /**
   * Deletes a pseudo from the repository.
   *
   * @param pseudo The pseudo to delete.
   */
  suspend fun deletePseudo(pseudo: String)
}
