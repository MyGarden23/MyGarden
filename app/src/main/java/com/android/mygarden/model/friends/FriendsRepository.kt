package com.android.mygarden.model.friends

import kotlinx.coroutines.flow.Flow

/**
 * Result of adding a friend, containing the updated friend counts for both users.
 *
 * @property currentUserFriendCount The total number of friends the current user has after adding
 * @property addedFriendCount The total number of friends the added friend has after the operation
 */
data class AddFriendResult(val currentUserFriendCount: Int, val addedFriendCount: Int)

interface FriendsRepository {

  /** Returns the list of friends for the given user. */
  suspend fun getFriends(userId: String): List<String>

  /**
   * Adds a friend relationship between currentUserId and friendUserId. Typically implemented as a
   * symmetric friendship (both users see each other as friends).
   *
   * Returns the updated friend counts for both users so the caller (typically ViewModel) can update
   * achievements.
   *
   * @param friendUserId The ID of the friend to add
   * @return [AddFriendResult] containing the friend counts for both users after adding
   * @throws IllegalArgumentException if currentUserId == friendUserId
   */
  suspend fun addFriend(friendUserId: String): AddFriendResult

  /**
   * Checks whether the given user ID is in the current in-memory friends list.
   *
   * @param friendUserId The UID to check.
   */
  suspend fun isFriend(friendUserId: String): Boolean

  /**
   * Deletes the friend relationship between currentUserId and friendUserId. Because it's
   * symmetrically implemented, it deletes it in both list of friends.
   *
   * @param friendUserId the id of the friend to delete
   * @throws IllegalArgumentException if currentUserId == friendUserId
   */
  suspend fun deleteFriend(friendUserId: String)

  /** Flow of friends */
  fun friendsFlow(userId: String): Flow<List<String>>

  /** Cleanup method to remove active listeners before logout. */
  fun cleanup()
}
