package com.android.mygarden.model.friends

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing friend requests.
 *
 * Provides access to friend requests for users, including reading, creating, accepting, and
 * refusing requests in Firestore or another data source.
 */
interface FriendRequestsRepository {

  /** Returns the Firebase Auth UID of the currently signed-in user. */
  fun getCurrentUserId(): String?

  /**
   * Returns all friend requests for the current user (both sent and received).
   *
   * @return A [Flow] emitting a list of [FriendRequest] objects whenever the requests change.
   */
  fun myRequests(): Flow<List<FriendRequest>>

  /**
   * Returns only incoming friend requests for the current user (requests received).
   *
   * @return A [Flow] emitting a list of [FriendRequest] objects where current user is the receiver.
   */
  fun incomingRequests(): Flow<List<FriendRequest>>

  /**
   * Returns only outgoing friend requests for the current user (requests sent).
   *
   * @return A [Flow] emitting a list of [FriendRequest] objects where current user is the sender.
   */
  fun outgoingRequests(): Flow<List<FriendRequest>>

  /**
   * Sends a friend request to another user.
   *
   * Creates a new friend request document with status PENDING.
   *
   * @param targetUserId The Firebase Auth UID of the user to send the request to.
   * @throws IllegalStateException if the user is not authenticated.
   * @throws IllegalArgumentException if trying to send a request to oneself.
   */
  suspend fun askFriend(targetUserId: String)

  /**
   * Marks a friend request as seen.
   *
   * Updates the seenByReceiver field of the request to true.
   *
   * @param requestId The ID of the friend request to mark as seen.
   */
  suspend fun markRequestAsSeen(requestId: String)

  /**
   * Accepts a friend request.
   *
   * Updates the request status to ACCEPTED and optionally adds the friend to both users' friend
   * lists.
   *
   * @param requestId The ID of the friend request to accept.
   * @throws IllegalStateException if the current user is not the recipient of the request.
   */
  suspend fun acceptRequest(requestId: String)

  /**
   * Refuses a friend request.
   *
   * Updates the request status to REFUSED or deletes the request document.
   *
   * @param requestId The ID of the friend request to refuse.
   * @throws IllegalStateException if the current user is not the recipient of the request.
   */
  suspend fun refuseRequest(requestId: String)

  /**
   * Cleans up any active listeners or resources.
   *
   * This should be called before signing out to prevent PERMISSION_DENIED errors from Firestore
   * listeners attempting to access data after the user is logged out.
   */
  fun cleanup()
}
