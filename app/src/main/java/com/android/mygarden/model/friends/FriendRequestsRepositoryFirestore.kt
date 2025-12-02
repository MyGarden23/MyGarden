package com.android.mygarden.model.friends

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of [FriendRequestsRepository].
 *
 * This repository stores friend requests in: users/{uid}/friend_requests/{requestId}
 *
 * Each request document contains:
 * - fromUserId: String - The user who sent the request
 * - toUserId: String - The user who received the request
 * - status: String - "PENDING", "ACCEPTED", or "REFUSED"
 * - createdAt: Timestamp - When the request was created
 * - seenByReceiver: Boolean - Whether the receiver has seen the request
 */
class FriendRequestsRepositoryFirestore(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val friendsRepository: FriendsRepository = FriendsRepositoryFirestore(db, auth)
) : FriendRequestsRepository {

  companion object {
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_FRIEND_REQUESTS = "friend_requests"
    private const val FIELD_FROM_USER_ID = "fromUserId"
    private const val FIELD_TO_USER_ID = "toUserId"
    private const val FIELD_STATUS = "status"
    private const val FIELD_CREATED_AT = "createdAt"
    private const val FIELD_SEEN_BY_RECEIVER = "seenByReceiver"

    // Error message
    private const val NOT_AUTHENTICATED_ERROR = "User not authenticated"
    private const val ERROR_REQUEST_NOT_FOUND = "Friend request not found"
  }

  // Keep track of active listeners so we can clean them up
  private val activeListeners = mutableListOf<ListenerRegistration>()

  override fun getCurrentUserId(): String? = auth.currentUser?.uid

  /**
   * Returns a reference to a user's friend_requests subcollection.
   *
   * Path: users/{userId}/friend_requests
   *
   * @param userId The user ID
   */
  private fun friendRequestsCollection(userId: String) =
      db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_FRIEND_REQUESTS)

  /**
   * Parses a Firestore document into a FriendRequest object.
   *
   * @param doc The Firestore document snapshot
   * @return A FriendRequest object, or null if parsing fails
   */
  private fun parseFriendRequest(
      doc: com.google.firebase.firestore.DocumentSnapshot
  ): FriendRequest? {
    return try {
      FriendRequest(
          id = doc.id,
          fromUserId = doc.getString(FIELD_FROM_USER_ID) ?: "",
          toUserId = doc.getString(FIELD_TO_USER_ID) ?: "",
          status = FriendRequestStatus.valueOf(doc.getString(FIELD_STATUS) ?: "PENDING"),
          createdAt =
              doc.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time ?: System.currentTimeMillis(),
          seenByReceiver = doc.getBoolean(FIELD_SEEN_BY_RECEIVER) ?: false)
    } catch (e: Exception) {
      Log.e("FriendRequestsRepo", "Error parsing friend request", e)
      null
    }
  }

  /**
   * Creates a Flow that listens to a Firestore query and emits parsed FriendRequest objects.
   *
   * @param query The Firestore query to listen to
   * @param errorTag A tag for logging errors
   * @return A Flow of FriendRequest lists
   */
  private fun observeRequestsQuery(query: Query, errorTag: String): Flow<List<FriendRequest>> =
      callbackFlow {
        val reg =
            query.addSnapshotListener { snapshots, error ->
              if (error != null) {
                Log.e("FriendRequestsRepo", "Error listening to $errorTag", error)
                trySend(emptyList())
                return@addSnapshotListener
              }

              val requests =
                  snapshots?.documents?.mapNotNull { parseFriendRequest(it) } ?: emptyList()
              trySend(requests)
            }

        activeListeners.add(reg)

        awaitClose {
          reg.remove()
          activeListeners.remove(reg)
        }
      }

  /**
   * Updates the status of a friend request in both users' subcollections.
   *
   * @param requestId The ID of the request to update
   * @param newStatus The new status to set
   * @param onSuccess Optional callback to execute after successful status update
   * @throws IllegalStateException if user is not authenticated
   * @throws IllegalArgumentException if request is not found or user is not the recipient
   */
  private suspend fun updateRequestStatus(
      requestId: String,
      newStatus: FriendRequestStatus,
      onSuccess: suspend (fromUserId: String) -> Unit = {}
  ) {
    val currentUserId = getCurrentUserId() ?: throw IllegalStateException(NOT_AUTHENTICATED_ERROR)

    // Get the request document
    val requestDoc = friendRequestsCollection(currentUserId).document(requestId).get().await()

    // Check if request exists
    check(requestDoc.exists()) { ERROR_REQUEST_NOT_FOUND }

    val fromUserId = requestDoc.getString(FIELD_FROM_USER_ID) ?: ""
    val toUserId = requestDoc.getString(FIELD_TO_USER_ID) ?: ""

    // Verify current user is the receiver
    require(toUserId == currentUserId) { "Only the recipient can modify this friend request" }

    // Verify the request is still pending
    val status = requestDoc.getString(FIELD_STATUS)
    require(status == FriendRequestStatus.PENDING.name) {
      "Friend request is not pending (current status: $status)"
    }

    // Update status in both subcollections using a batch
    val batch = db.batch()
    val requestRef = friendRequestsCollection(currentUserId).document(requestId)
    val requestRefOther = friendRequestsCollection(fromUserId).document(requestId)
    batch.update(requestRef, FIELD_STATUS, newStatus.name)
    batch.update(requestRefOther, FIELD_STATUS, newStatus.name)
    batch.commit().await()

    // Execute success callback
    onSuccess(fromUserId)
  }

  /**
   * Returns all friend requests for the current user (both sent and received).
   *
   * This queries both the current user's subcollection and all other users' subcollections where
   * the current user is mentioned.
   */
  override fun myRequests(): Flow<List<FriendRequest>> {
    val uid = getCurrentUserId() ?: return flowOf(emptyList())
    return observeRequestsQuery(friendRequestsCollection(uid), "friend requests")
  }

  /**
   * Returns only incoming friend requests (where current user is the receiver). Requests receive
   */
  override fun incomingRequests(): Flow<List<FriendRequest>> {
    val uid = getCurrentUserId() ?: return flowOf(emptyList())

    val query =
        friendRequestsCollection(uid)
            .whereEqualTo(FIELD_TO_USER_ID, uid)
            .whereEqualTo(FIELD_STATUS, FriendRequestStatus.PENDING.name)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)

    return observeRequestsQuery(query, "incoming requests")
  }

  /** Returns only outgoing friend requests (where current user is the sender). Requests send */
  override fun outgoingRequests(): Flow<List<FriendRequest>> {
    val uid = getCurrentUserId() ?: return flowOf(emptyList())

    val query =
        friendRequestsCollection(uid)
            .whereEqualTo(FIELD_FROM_USER_ID, uid)
            .whereEqualTo(FIELD_STATUS, FriendRequestStatus.PENDING.name)
            .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)

    return observeRequestsQuery(query, "outgoing requests")
  }

  /**
   * Sends a friend request to another user.
   *
   * Creates the request in BOTH users' subcollections atomically using a WriteBatch:
   * - users/{currentUserId}/friend_requests/{requestId}
   * - users/{targetUserId}/friend_requests/{requestId}
   */
  override suspend fun askFriend(targetUserId: String) {
    val currentUserId = getCurrentUserId() ?: throw IllegalStateException(NOT_AUTHENTICATED_ERROR)

    require(currentUserId != targetUserId) { "Cannot send friend request to yourself" }

    try {
      // Check if a request already exists
      val existingRequests =
          friendRequestsCollection(currentUserId)
              .whereEqualTo(FIELD_FROM_USER_ID, currentUserId)
              .whereEqualTo(FIELD_TO_USER_ID, targetUserId)
              .get()
              .await()

      if (!existingRequests.isEmpty) {
        Log.w("FriendRequestsRepo", "Friend request already exists")
        return
      }

      val requestData =
          mapOf(
              FIELD_FROM_USER_ID to currentUserId,
              FIELD_TO_USER_ID to targetUserId,
              FIELD_STATUS to FriendRequestStatus.PENDING.name,
              FIELD_CREATED_AT to Timestamp.now(),
              FIELD_SEEN_BY_RECEIVER to false)

      // Use WriteBatch to write to both subcollections atomically
      val batch = db.batch()
      val senderDoc = friendRequestsCollection(currentUserId).document()
      batch.set(senderDoc, requestData)
      batch.set(friendRequestsCollection(targetUserId).document(senderDoc.id), requestData)
      batch.commit().await()

      Log.d("FriendRequestsRepo", "Friend request sent successfully")
    } catch (e: Exception) {
      Log.e("FriendRequestsRepo", "Failed to send friend request", e)
      throw e
    }
  }

  /**
   * Marks a friend request as seen.
   *
   * Updates the seenByReceiver field of the request to true.
   */
  override suspend fun markRequestAsSeen(requestId: String) {
    val uid = getCurrentUserId() ?: return
    friendRequestsCollection(uid).document(requestId).update(FIELD_SEEN_BY_RECEIVER, true)
  }

  /**
   * Accepts a friend request.
   * - Updates the status to ACCEPTED in both users' subcollections atomically
   * - Adds each user to the other's friends list using FriendsRepository
   */
  override suspend fun acceptRequest(requestId: String) {
    try {
      updateRequestStatus(requestId, FriendRequestStatus.ACCEPTED) { fromUserId ->
        // Add to friends list (both directions)
        friendsRepository.addFriend(fromUserId)
      }
      Log.d("FriendRequestsRepo", "Friend request accepted successfully")
    } catch (e: Exception) {
      Log.e("FriendRequestsRepo", "Failed to accept friend request", e)
      throw e
    }
  }

  /**
   * Refuses a friend request.
   *
   * Updates the status to REFUSED in both users' subcollections atomically using a batch.
   */
  override suspend fun refuseRequest(requestId: String) {
    try {
      updateRequestStatus(requestId, FriendRequestStatus.REFUSED)
      Log.d("FriendRequestsRepo", "Friend request refused successfully")
    } catch (e: Exception) {
      Log.e("FriendRequestsRepo", "Failed to refuse friend request", e)
      throw e
    }
  }

  override fun cleanup() {
    activeListeners.forEach { it.remove() }
    activeListeners.clear()
  }
}
