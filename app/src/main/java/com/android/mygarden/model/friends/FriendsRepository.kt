package com.android.mygarden.model.friends

import kotlinx.coroutines.flow.Flow

interface FriendsRepository {

  /** Returns the list of friends for the given user. */
  suspend fun getFriends(userId: String): List<String>

  /**
   * Adds a friend relationship between currentUserId and friendUserId. Typically implemented as a
   * symmetric friendship (both users see each other as friends).
   *
   * @throws IllegalArgumentException if currentUserId == friendUserId
   */
  suspend fun addFriend(friendUserId: String)

  /**
   * Checks whether the given user ID is in the current in-memory friends list.
   *
   * @param friendUserId The UID to check.
   */
  suspend fun isFriend(friendUserId: String): Boolean

  /** Flow of friends */
  fun friendsFlow(userId: String): Flow<List<String>>
}
