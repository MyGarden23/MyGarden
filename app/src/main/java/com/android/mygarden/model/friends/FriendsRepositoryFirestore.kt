package com.android.mygarden.model.friends

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Firestore collection and field names
private const val COLLECTION_USERS = "users"
private const val COLLECTION_FRIENDS = "friends"
private const val FIELD_FRIEND_UID = "friendUid"

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

  // Variable that stores locally the friends count to avoid repeating calls to the database
  private var friendsCount: Int? = null
  private val nonAuthErrorMessage = "User not authenticated"

  // Keep track of active listeners so we can clean them up
  private val activeFriendsListeners = mutableListOf<ListenerRegistration>()

  /**
   * Returns a reference to the authenticated user's "friends" subcollection.
   *
   * Path: users/{userId}/friends
   */
  private fun friendsCollection(userId: String) =
      db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_FRIENDS)

  override suspend fun getFriends(userId: String): List<String> {
    val snapshot = friendsCollection(userId).get().await()
    return snapshot.documents.mapNotNull { doc -> doc.getString(FIELD_FRIEND_UID) }
  }

  override suspend fun addFriend(friendUserId: String): AddFriendResult {
    val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException(nonAuthErrorMessage)
    require(currentUserId != friendUserId) { "You cannot add yourself as a friend." }

    // Add friend in both users' lists (bidirectional friendship) using a batch
    val currentDoc = friendsCollection(currentUserId).document(friendUserId)
    val friendDoc = friendsCollection(friendUserId).document(currentUserId)

    val batch = db.batch()
    batch[currentDoc] = mapOf(FIELD_FRIEND_UID to friendUserId)
    batch[friendDoc] = mapOf(FIELD_FRIEND_UID to currentUserId)
    batch.commit().await()

    currentDoc.set(mapOf(FIELD_FRIEND_UID to friendUserId)).await()

    // Get the number of friends and update the friends number achievement
    val friendUserFriendsCount = friendsCollection(friendUserId).get().await().size()
    val currentUserFriendsCount = friendsCollection(currentUserId).get().await().size()

    return AddFriendResult(
        currentUserFriendCount = currentUserFriendsCount, addedFriendCount = friendUserFriendsCount)
  }

  /**
   * Checks whether the given user ID is already listed as a friend of the current user.
   *
   * @param friendUserId The UID of the user to check.
   * @return `true` if the user appears in the current user's friends list, `false` otherwise.
   * @throws IllegalStateException if no authenticated user is available.
   */
  override suspend fun isFriend(friendUserId: String): Boolean {
    val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException(nonAuthErrorMessage)
    val friends = getFriends(currentUserId)
    return friends.contains(friendUserId)
  }

  override suspend fun deleteFriend(friendUserId: String) {
    // get current user id
    val currentUserId = auth.currentUser?.uid ?: throw IllegalStateException(nonAuthErrorMessage)
    require(currentUserId != friendUserId) { "You cannot delete yourself." }

    // get both documents we want to delete
    val currentDoc = friendsCollection(currentUserId).document(friendUserId)
    val friendDoc = friendsCollection(friendUserId).document(currentUserId)

    // make both deletions atomic with a batch
    val batch = db.batch()
    batch.delete(friendDoc)
    batch.delete(currentDoc)
    batch.commit().await()
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
            val friends = snapshot.documents.mapNotNull { it.getString(FIELD_FRIEND_UID) }
            trySend(friends).isSuccess
          }
        }

    activeFriendsListeners.add(registration)

    // Clean up the listener when the flow collector is cancelled
    awaitClose {
      registration.remove()
      activeFriendsListeners.remove(registration)
    }
  }

  /** Cleanup method to remove active listeners before logout. */
  override fun cleanup() {
    activeFriendsListeners.forEach { it.remove() }
    activeFriendsListeners.clear()
  }
}
