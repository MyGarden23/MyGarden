package com.android.mygarden.model.friends

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
}
