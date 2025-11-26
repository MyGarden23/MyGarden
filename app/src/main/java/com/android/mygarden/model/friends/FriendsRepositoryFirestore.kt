package com.android.mygarden.model.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
}
