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
   */
  private fun friendRequestsCollection(userId: String) =
      db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_FRIEND_REQUESTS)

  /**
   * Returns all friend requests for the current user (both sent and received).
   *
   * This queries both the current user's subcollection and all other users' subcollections where
   * the current user is mentioned.
   */
  override fun myRequests(): Flow<List<FriendRequest>> {
    val uid = getCurrentUserId() ?: return flowOf(emptyList())

    return callbackFlow {
      // We need to listen to requests in the current user's subcollection
      // These are requests where current user is either sender or receiver
      val reg =
          friendRequestsCollection(uid).addSnapshotListener { snapshots, error ->
            if (error != null) {
              Log.e("FriendRequestsRepo", "Error listening to friend requests", error)
              trySend(emptyList())
              return@addSnapshotListener
            }

            val requests =
                snapshots?.documents?.mapNotNull { doc ->
                  try {
                    FriendRequest(
                        id = doc.id, // Id document Firestore
                        fromUserId = doc.getString(FIELD_FROM_USER_ID) ?: "",
                        toUserId = doc.getString(FIELD_TO_USER_ID) ?: "",
                        status =
                            FriendRequestStatus.valueOf(doc.getString(FIELD_STATUS) ?: "PENDING"),
                        createdAt =
                            doc.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time
                                ?: System.currentTimeMillis())
                  } catch (e: Exception) {
                    Log.e("FriendRequestsRepo", "Error parsing friend request", e)
                    null
                  }
                } ?: emptyList()

            trySend(requests) // Try to send the list of requests
          }

      activeListeners.add(reg)

      awaitClose {
        reg.remove()
        activeListeners.remove(reg)
      }
    }
  }

  /**
   * Returns only incoming friend requests (where current user is the receiver). Requests receive
   */
  override fun incomingRequests(): Flow<List<FriendRequest>> {
    val uid = getCurrentUserId() ?: return flowOf(emptyList())

    return callbackFlow {
      val reg =
          friendRequestsCollection(uid)
              .whereEqualTo(FIELD_TO_USER_ID, uid)
              .whereEqualTo(FIELD_STATUS, FriendRequestStatus.PENDING.name)
              .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
              .addSnapshotListener { snapshots, error ->
                if (error != null) {
                  Log.e("FriendRequestsRepo", "Error listening to incoming requests", error)
                  trySend(emptyList())
                  return@addSnapshotListener
                }

                val requests =
                    snapshots?.documents?.mapNotNull { doc ->
                      try {
                        FriendRequest(
                            id = doc.id,
                            fromUserId = doc.getString(FIELD_FROM_USER_ID) ?: "",
                            toUserId = doc.getString(FIELD_TO_USER_ID) ?: "",
                            status =
                                FriendRequestStatus.valueOf(
                                    doc.getString(FIELD_STATUS) ?: "PENDING"),
                            createdAt =
                                doc.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time
                                    ?: System.currentTimeMillis())
                      } catch (e: Exception) {
                        Log.e("FriendRequestsRepo", "Error parsing incoming request", e)
                        null
                      }
                    } ?: emptyList()

                trySend(requests)
              }

      activeListeners.add(reg)

      awaitClose {
        reg.remove()
        activeListeners.remove(reg)
      }
    }
  }

  /** Returns only outgoing friend requests (where current user is the sender). Requests send */
  override fun outgoingRequests(): Flow<List<FriendRequest>> {
    val uid = getCurrentUserId() ?: return flowOf(emptyList())

    return callbackFlow {
      val reg =
          friendRequestsCollection(uid)
              .whereEqualTo(FIELD_FROM_USER_ID, uid)
              .whereEqualTo(FIELD_STATUS, FriendRequestStatus.PENDING.name)
              .orderBy(FIELD_CREATED_AT, Query.Direction.DESCENDING)
              .addSnapshotListener { snapshots, error ->
                if (error != null) {
                  Log.e("FriendRequestsRepo", "Error listening to outgoing requests", error)
                  trySend(emptyList())
                  return@addSnapshotListener
                }

                val requests =
                    snapshots?.documents?.mapNotNull { doc ->
                      try {
                        FriendRequest(
                            id = doc.id,
                            fromUserId = doc.getString(FIELD_FROM_USER_ID) ?: "",
                            toUserId = doc.getString(FIELD_TO_USER_ID) ?: "",
                            status =
                                FriendRequestStatus.valueOf(
                                    doc.getString(FIELD_STATUS) ?: "PENDING"),
                            createdAt =
                                doc.getTimestamp(FIELD_CREATED_AT)?.toDate()?.time
                                    ?: System.currentTimeMillis())
                      } catch (e: Exception) {
                        Log.e("FriendRequestsRepo", "Error parsing outgoing request", e)
                        null
                      }
                    } ?: emptyList()

                trySend(requests)
              }

      activeListeners.add(reg)

      awaitClose {
        reg.remove()
        activeListeners.remove(reg)
      }
    }
  }

  /**
   * Sends a friend request to another user.
   *
   * Creates the request in BOTH users' subcollections:
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
              FIELD_CREATED_AT to Timestamp.now())

      // Add to sender's subcollection
      val senderDoc = friendRequestsCollection(currentUserId).document()
      senderDoc.set(requestData).await()

      // Add to receiver's subcollection with same ID
      friendRequestsCollection(targetUserId).document(senderDoc.id).set(requestData).await()

      Log.d("FriendRequestsRepo", "Friend request sent successfully")
    } catch (e: Exception) {
      Log.e("FriendRequestsRepo", "Failed to send friend request", e)
      throw e
    }
  }

  /**
   * Accepts a friend request.
   * - Updates the status to ACCEPTED in both users' subcollections
   * - Adds each user to the other's friends list using FriendsRepository
   */
  override suspend fun acceptRequest(requestId: String) {
    val currentUserId = getCurrentUserId() ?: throw IllegalStateException(NOT_AUTHENTICATED_ERROR)

    try {
      // Get the request document
      val requestDoc = friendRequestsCollection(currentUserId).document(requestId).get().await()

      // Check if request exists
      check(requestDoc.exists()) { ERROR_REQUEST_NOT_FOUND }

      val fromUserId = requestDoc.getString(FIELD_FROM_USER_ID) ?: ""
      val toUserId = requestDoc.getString(FIELD_TO_USER_ID) ?: ""

      // Verify current user is the receiver
      require(toUserId == currentUserId) { "Only the recipient can accept this friend request" }

      // Update status to ACCEPTED in both subcollections
      val updateData = mapOf(FIELD_STATUS to FriendRequestStatus.ACCEPTED.name)

      friendRequestsCollection(currentUserId).document(requestId).update(updateData).await()
      friendRequestsCollection(fromUserId).document(requestId).update(updateData).await()

      // Add to friends list (both directions)
      friendsRepository.addFriend(fromUserId)

      Log.d("FriendRequestsRepo", "Friend request accepted successfully")
    } catch (e: Exception) {
      Log.e("FriendRequestsRepo", "Failed to accept friend request", e)
      throw e
    }
  }

  /**
   * Refuses a friend request.
   *
   * Updates the status to REFUSED in both users' subcollections (or you could delete the documents
   * instead).
   */
  override suspend fun refuseRequest(requestId: String) {
    val currentUserId = getCurrentUserId() ?: throw IllegalStateException(NOT_AUTHENTICATED_ERROR)

    try {
      // Get the request document
      val requestDoc = friendRequestsCollection(currentUserId).document(requestId).get().await()

      // Check if request exists
      check(requestDoc.exists()) { ERROR_REQUEST_NOT_FOUND }

      val fromUserId = requestDoc.getString(FIELD_FROM_USER_ID) ?: ""
      val toUserId = requestDoc.getString(FIELD_TO_USER_ID) ?: ""

      // Verify current user is the receiver
      require(toUserId == currentUserId) { "Only the recipient can refuse this friend request" }

      // Update status to REFUSED
      val updateData = mapOf(FIELD_STATUS to FriendRequestStatus.REFUSED.name)
      friendRequestsCollection(currentUserId).document(requestId).update(updateData).await()
      friendRequestsCollection(fromUserId).document(requestId).update(updateData).await()

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
