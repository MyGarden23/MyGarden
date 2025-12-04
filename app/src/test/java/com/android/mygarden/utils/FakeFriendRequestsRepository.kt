package com.android.mygarden.utils

import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.friends.FriendRequestsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Minimal fake implementation of [FriendRequestsRepository] for tests. */
class FakeFriendRequestsRepository(initialRequests: List<FriendRequest> = emptyList()) :
    FriendRequestsRepository {
  val incomingRequestsFlow = MutableStateFlow(initialRequests)
  var currentUserIdValue: String? = "test-user-id"

  override fun getCurrentUserId(): String? = currentUserIdValue

  override fun myRequests(): Flow<List<FriendRequest>> = MutableStateFlow(emptyList())

  override fun incomingRequests(): Flow<List<FriendRequest>> = incomingRequestsFlow

  override fun outgoingRequests(): Flow<List<FriendRequest>> = MutableStateFlow(emptyList())

  override suspend fun askFriend(targetUserId: String) {
    if (targetUserId == "boom-user-id") {
      throw IllegalStateException("boom")
    } else {
      incomingRequestsFlow.value =
          incomingRequestsFlow.value + FriendRequest(fromUserId = targetUserId)
    }
  }

  override suspend fun acceptRequest(requestId: String) {
    incomingRequestsFlow.value = incomingRequestsFlow.value.filter { it.id != requestId }
  }

  var markedSeen: MutableList<String> = mutableListOf()

  override suspend fun markRequestAsSeen(requestId: String) {
    markedSeen += requestId
  }

  override suspend fun refuseRequest(requestId: String) {
    incomingRequestsFlow.value = incomingRequestsFlow.value.filter { it.id != requestId }
  }

  override fun cleanup() {}
}
