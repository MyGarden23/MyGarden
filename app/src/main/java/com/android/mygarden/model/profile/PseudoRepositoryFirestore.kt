package com.android.mygarden.model.profile

import com.google.firebase.firestore.FieldPath
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

  override suspend fun searchPseudoStartingWith(query: String): List<String> {
    val q = query.trim().lowercase()
    if (query.isBlank()) return emptyList()

    val snapshot =
        db.collection(PSEUDO_COLLECTION_PATH)
            .orderBy(FieldPath.documentId())
            .startAt(q)
            .endAt(q + "\uf8ff")
            .get()
            .await()

    return snapshot.documents.map { it.id }
  }

  override suspend fun getUidFromPseudo(pseudo: String): String? {
    val pseudoRef = pseudoRef(pseudo).get().await()

    if (!pseudoRef.exists()) return null

    return pseudoRef.getString(USER_ID_FIELD)
  }

  override suspend fun updatePseudoAtomic(oldPseudo: String?, newPseudo: String, userId: String) {
    val newRef = pseudoRef(newPseudo)
    val oldRef = oldPseudo?.let { pseudoRef(it) }

    if (newRef != oldRef) {
      db.runTransaction { transaction ->
            val newPseudo = transaction.get(newRef)

            if (oldRef != null) {
              val oldSnap = transaction.get(oldRef)
              if (oldSnap.exists()) {
                transaction.delete(oldRef)
              }
            }

            check(!(newPseudo.exists())) { "Pseudo already taken" }
            transaction.set(newRef, mapOf(USER_ID_FIELD to userId))
          }
          .await()
    }
  }
}
