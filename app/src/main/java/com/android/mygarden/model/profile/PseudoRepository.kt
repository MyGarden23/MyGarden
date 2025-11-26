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
   * @param userId The ID of the user associated with the pseudo.
   * @throws IllegalStateException if the pseudo already exists.
   */
  suspend fun savePseudo(pseudo: String, userId: String)

  /**
   * Deletes a pseudo from the repository.
   *
   * @param pseudo The pseudo to delete.
   */
  suspend fun deletePseudo(pseudo: String)

  /**
   * Searches pseudos that start with the given query, for friend suggestions.
   *
   * @param query The prefix to match. Example: "ma" → ["matt", "marie", ...]
   * @return A list of matching pseudo → userId pairs.
   */
  suspend fun searchPseudoStartingWith(query: String): List<String>

  /**
   * Retrieves the user ID associated with a given pseudo.
   *
   * @param pseudo The pseudo to look up.
   * @return The user ID if it exists, or null otherwise.
   */
  suspend fun getUidFromPseudo(pseudo: String): String?
}
