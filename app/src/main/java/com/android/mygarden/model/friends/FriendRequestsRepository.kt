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
   * Checks whether the given user has a pending request for the target user.
   *
   * @param targetUserId The UID of the user to check.
   * @return `true` if a pending outGoing request exists for this user, `false` otherwise.
   */
  suspend fun isInOutgoingRequests(targetUserId: String): Boolean

  /**
   * Checks whether the target user has a pending request for the current user.
   *
   * @param targetUserId The UID of the user to check.
   * @return `true` if a pending outGoing request exists for this user, `false` otherwise.
   */
  suspend fun isInIncomingRequests(targetUserId: String): Boolean

  /**
   * Sends a friend request to another user.
   *
   * Creates a new friend request document with status PENDING. If the other user already sent a
   * request, this will mark that request as ACCEPTED and return `true`. The caller (typically
   * ViewModel) is responsible for adding to the friends list in that case.
   *
   * @param targetUserId The Firebase Auth UID of the user to send the request to.
   * @return `true` if an existing incoming request was auto-accepted, `false` if a new request was
   *   sent.
   * @throws IllegalStateException if the user is not authenticated.
   * @throws IllegalArgumentException if trying to send a request to oneself.
   */
  suspend fun askFriend(targetUserId: String): Boolean

  /**
   * Marks a friend request as seen.
   *
   * Updates the seenByReceiver field of the request to true.
   *
   * @param requestId The ID of the friend request to mark as seen.
   */
  suspend fun markRequestAsSeen(requestId: String)

  /**
   * Accepts a friend request by updating the status to ACCEPTED.
   *
   * Note: This only updates the request status. The caller (typically the ViewModel) is responsible
   * for adding the users to each other's friends list.
   *
   * @param requestId The ID of the friend request to accept.
   * @return The user ID of the person who sent the friend request.
   * @throws IllegalStateException if the current user is not the recipient of the request.
   */
  suspend fun acceptRequest(requestId: String): String

  /**
   * Refuses a friend request.
   *
   * deletes the request document.
   *
   * @param requestId The ID of the friend request to refuse.
   * @throws IllegalStateException if the current user is not the recipient of the request.
   */
  suspend fun refuseRequest(requestId: String)

  /**
   * Deletes a pending friend request.
   *
   * @param requestId The ID of the request to delete.
   */
  suspend fun deleteRequest(requestId: String)

  /**
   * Cleans up any active listeners or resources.
   *
   * This should be called before signing out to prevent PERMISSION_DENIED errors from Firestore
   * listeners attempting to access data after the user is logged out.
   */
  fun cleanup()
}
