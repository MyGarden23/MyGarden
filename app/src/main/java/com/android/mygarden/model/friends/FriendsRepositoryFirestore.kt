package com.android.mygarden.model.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of [FriendsRepository].
 *
 * This repository stores friends inside: users/{uid}/friends/{friendUid}
 *
 * Only the authenticated user can add friends to their own list.
 */
class FriendsRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : FriendsRepository {

  /**
   * Returns a reference to the authenticated user's "friends" subcollection.
   *
   * Path: users/{userId}/friends
   */
  private fun friendsCollection(userId: String) =
      db.collection("users").document(userId).collection("friends")

  override suspend fun getFriends(userId: String): List<String> {
    val snapshot = friendsCollection(userId).get().await()

    return snapshot.documents.mapNotNull { doc -> doc.getString("friendUid") }
  }

  override suspend fun addFriend(friendUserId: String) {
    val currentUserId =
        auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    require(currentUserId != friendUserId) { "You cannot add yourself as a friend." }

    // Add friend in current user’s list
    val currentDoc = friendsCollection(currentUserId).document(friendUserId)
    currentDoc.set(mapOf("friendUid" to friendUserId)).await()
  }

  override fun friendsFlow(userId: String): Flow<List<String>> = callbackFlow {
    val registration =
        friendsCollection(userId).addSnapshotListener { snapshot, error ->
          if (error != null) {
            // optionally log error
            // close(error) // if you want the flow to terminate
            return@addSnapshotListener
          }

          if (snapshot != null) {
            val friends = snapshot.documents.mapNotNull { it.getString("friendUid") }
            trySend(friends).isSuccess
          }
        }

    // Clean up the listener when the flow collector is cancelled
    awaitClose { registration.remove() }
  }
}
