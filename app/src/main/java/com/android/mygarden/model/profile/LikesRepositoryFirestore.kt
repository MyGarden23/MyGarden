package com.android.mygarden.model.profile

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await


/**
 * Firestore implementation of [LikesRepository].
 *
 * Likes are stored as documents whose ID is the UID of the liker,
 * ensuring that a user can like a given garden only once.
 */
class LikesRepositoryFirestore(
    private val db: FirebaseFirestore
) : LikesRepository {

    private fun userDoc(uid: String) =
        db.collection("users").document(uid)

    private fun likeDoc(targetUid: String, myUid: String) =
        userDoc(targetUid).collection("likes").document(myUid)

    override fun observeLikesCount(targetUid: String): Flow<Int> = callbackFlow {
        val reg = userDoc(targetUid).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.getLong("likesCount")?.toInt() ?: 0)
        }
        awaitClose { reg.remove() }
    }

    override fun observeHasLiked(
        targetUid: String,
        myUid: String
    ): Flow<Boolean> = callbackFlow {
        val reg = likeDoc(targetUid, myUid).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            trySend(snap?.exists() == true)
        }
        awaitClose { reg.remove() }
    }

    override suspend fun toggleLike(targetUid: String, myUid: String) {
        if (targetUid == myUid) return

        val targetRef = userDoc(targetUid)
        val likeRef = likeDoc(targetUid, myUid)

        db.runTransaction { tx ->
            val likeSnap = tx.get(likeRef)
            val profileSnap = tx.get(targetRef)
            val current = profileSnap.getLong("likesCount") ?: 0L

            if (!likeSnap.exists()) {
                tx.set(likeRef, mapOf("createdAt" to FieldValue.serverTimestamp()))
                tx.update(targetRef, "likesCount", current + 1)
            } else {
                tx.delete(likeRef)
                tx.update(targetRef, "likesCount", maxOf(0L, current - 1))
            }
            null
        }.await()
    }
}