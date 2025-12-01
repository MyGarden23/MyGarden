package com.android.mygarden.utils

import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.friends.FriendRequestsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Minimal fake implementation of [FriendRequestsRepository] for tests. */
class FakeFriendRequestsRepository(initialRequests: List<FriendRequest> = emptyList()) :
    FriendRequestsRepository {
  val flow = MutableStateFlow(initialRequests)
  var currentUserIdValue: String? = "test-user-id"

  override fun getCurrentUserId(): String? = currentUserIdValue

  override fun myRequests(): Flow<List<FriendRequest>> = MutableStateFlow(emptyList())

  override fun incomingRequests(): Flow<List<FriendRequest>> = flow

  override fun outgoingRequests(): Flow<List<FriendRequest>> = MutableStateFlow(emptyList())

  override suspend fun askFriend(targetUserId: String) {}

  override suspend fun acceptRequest(requestId: String) {
    flow.value = flow.value.filter { it.fromUserId != requestId }
  }

  override suspend fun refuseRequest(requestId: String) {
    flow.value = flow.value.filter { it.fromUserId != requestId }
  }

  override fun cleanup() {}
}
