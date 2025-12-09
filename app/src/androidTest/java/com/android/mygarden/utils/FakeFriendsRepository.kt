package com.android.mygarden.utils

import com.android.mygarden.model.friends.FriendsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A lightweight in-memory fake implementation of [FriendsRepository] for unit tests.
 *
 * This fake simulates friend additions without using Firebase. It records all added friend IDs in
 * [addedFriends] so tests can assert that the ViewModel called the repository correctly.
 *
 * You can also simulate failures by assigning an exception to [throwOnAdd]. For example:
 * ```
 * val fake = FakeFriendsRepository().apply { throwOnAdd = IllegalStateException("boom") }
 * ```
 */
class FakeFriendsRepository : FriendsRepository {

  /** Records every friend ID added through [addFriend]. */
  val addedFriends: MutableList<String> = mutableListOf()
  val friendsFlow = MutableStateFlow<List<String>>(emptyList())

  /** If non-null, this exception will be thrown when [addFriend] is called. */
  var throwOnAdd: Throwable? = null

  /**
   * Returns an empty list. Friend lists are not needed in current ViewModel tests, but this can be
   * expanded when needed.
   */
  override suspend fun getFriends(userId: String): List<String> = friendsFlow.value

  /**
   * Simulates adding a friend. Records the friend ID unless [throwOnAdd] is set, in which case
   * throws the exception to simulate Firebase failures.
   */
  override suspend fun addFriend(friendUserId: String) {
    throwOnAdd?.let { throw it }
    addedFriends += friendUserId
    friendsFlow.value = friendsFlow.value + friendUserId
  }

  override fun friendsFlow(userId: String): Flow<List<String>> = friendsFlow

  override suspend fun isFriend(friendUserId: String): Boolean {
    return friendsFlow.value.contains(friendUserId)
  }
}
