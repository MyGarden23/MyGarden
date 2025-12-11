package com.android.mygarden.model.friends

import com.android.mygarden.model.achievements.ACHIEVEMENTS_BASE_VALUE
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

// Firestore collection and field names
private const val COLLECTION_USERS = "users"
private const val COLLECTION_FRIENDS = "friends"
private const val FIELD_FRIEND_UID = "friendUid"

// Value used to update the number of friends
private const val FRIEND_ACHIEVEMENT_INCREMENT_STEP = 1

/**
 * Firestore-backed implementation of [FriendsRepository].
 *
 * This repository stores friends inside: users/{uid}/friends/{friendUid}
 *
 * Only the authenticated user can add friends to their own list.
 */
class FriendsRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val achievementsRep: AchievementsRepository = AchievementsRepositoryProvider.repository
) : FriendsRepository {

  // Variable that stores locally the friends count to avoid repeating calls to the database
  private var friendsCount: Int? = null

  /**
   * Returns a reference to the authenticated user's "friends" subcollection.
   *
   * Path: users/{userId}/friends
   */
  private fun friendsCollection(userId: String) =
      db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_FRIENDS)

  override suspend fun getFriends(userId: String): List<String> {
    val snapshot = friendsCollection(userId).get().await()
    val friends = snapshot.documents.mapNotNull { doc -> doc.getString(FIELD_FRIEND_UID) }
    friendsCount = friends.size
    return friends
  }

  override suspend fun addFriend(friendUserId: String) {
    val currentUserId =
        auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
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
    val newCount = (friendsCount ?: ACHIEVEMENTS_BASE_VALUE) + FRIEND_ACHIEVEMENT_INCREMENT_STEP
    friendsCount = newCount
    achievementsRep.updateAchievementValue(
        currentUserId, AchievementType.FRIENDS_NUMBER, friendsCount!!)
  }

  override suspend fun deleteFriend(friendUserId: String) {
    // get current user id
    val currentUserId =
        auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
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

    // Clean up the listener when the flow collector is cancelled
    awaitClose { registration.remove() }
  }
}
