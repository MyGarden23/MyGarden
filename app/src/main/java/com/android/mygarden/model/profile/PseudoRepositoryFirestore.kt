package com.android.mygarden.model.profile

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Constant value for the collection of pseudos in firestore. */
private const val PSEUDO_COLLECTION_PATH = "pseudos"

/** Field that associates each pseudo to its user */
private const val USER_ID_FIELD = "userID"

/** Repository that implements PseudoRepository but stores the data in Firestore. */
class PseudoRepositoryFirestore(private val db: FirebaseFirestore) : PseudoRepository {

  /** Helper function that returns a reference to the pseudo document in Firestore. */
  private fun pseudoRef(pseudo: String) =
      db.collection(PSEUDO_COLLECTION_PATH).document(pseudo.lowercase())

  override suspend fun isPseudoAvailable(pseudo: String): Boolean {
    return !pseudoRef(pseudo).get().await().exists()
  }

  override suspend fun savePseudo(pseudo: String, userId: String) {
    val pseudoRef = pseudoRef(pseudo)

    check(!(pseudoRef.get().await().exists())) { "Pseudo already taken" }

    pseudoRef.set(mapOf(USER_ID_FIELD to userId)).await()
  }

  override suspend fun deletePseudo(pseudo: String) {
    pseudoRef(pseudo).delete().await()
  }
}
