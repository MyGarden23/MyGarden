package com.android.mygarden.ui.friendsRequests

import app.cash.turbine.test
import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.friends.FriendRequestStatus
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [FriendsRequestsPopupViewModel]. Using a fake [FriendRequestsRepository] and a fake
 * [UserProfileRepository] for testing.
 */
class FriendsRequestsPopupViewModelTest {

  private lateinit var fakeFriendsRepo: FakeFriendRequestsRepository
  private lateinit var fakeUserRepo: FakeUserProfileRepository
  private lateinit var vm: FriendsRequestsPopupViewModel

  /** Request used in the tests. (From Alice) */
  private val classicalFriendRequestAlice =
      FriendRequest(
          id = "req-A",
          fromUserId = "sender-A",
          toUserId = "rx",
          status = FriendRequestStatus.PENDING)

  /** Request used in the tests. (From Bob) */
  private val classicalFriendRequestBob =
      FriendRequest(
          id = "req-B",
          fromUserId = "sender-B",
          toUserId = "rx",
          status = FriendRequestStatus.PENDING)

  @Before
  fun setup() {
    fakeFriendsRepo = FakeFriendRequestsRepository()
    fakeUserRepo = FakeUserProfileRepository()

    vm =
        FriendsRequestsPopupViewModel(friendsRepo = fakeFriendsRepo, userProfileRepo = fakeUserRepo)

    fakeUserRepo.profiles["sender"] =
        UserProfile(id = "sender", pseudo = "alice", avatar = Avatar.A10)

    fakeUserRepo.profiles["bob"] = UserProfile("bob", "Bob", Avatar.A11)
  }

  @Test
  fun emits_FriendRequestUiModel_when_new_incoming_request_arrives() = runTest {
    // Setup sender profile
    vm.newRequests.test {
      // Emit request
      fakeFriendsRepo.incomingRequestsFlow.value = listOf(classicalFriendRequestAlice)

      val emitted = awaitItem()
      assertEquals("req-A", emitted.request.id)
      assertEquals("alice", emitted.senderPseudo)
    }
  }

  @Test
  fun does_not_emit_when_sender_profile_is_missing() = runTest {
    val req =
        FriendRequest(
            id = "req-2",
            fromUserId = "unknown-user",
            toUserId = "rx",
            status = FriendRequestStatus.PENDING)

    vm.newRequests.test {
      fakeFriendsRepo.incomingRequestsFlow.value = listOf(req)
      expectNoEvents()
    }
  }

  @Test
  fun marks_request_as_seen_when_emitted() = runTest {
    vm.newRequests.test {
      fakeFriendsRepo.incomingRequestsFlow.value = listOf(classicalFriendRequestAlice)

      awaitItem()
      assertTrue("req-A" in fakeFriendsRepo.markedSeen)
    }
  }

  @Test
  fun emits_multiple_incoming_requests_in_order() = runTest {
    vm.newRequests.test {
      fakeFriendsRepo.incomingRequestsFlow.value =
          listOf(classicalFriendRequestAlice, classicalFriendRequestBob)

      val first = awaitItem()
      assertEquals("req-A", first.request.id)
      assertEquals("alice", first.senderPseudo)

      val second = awaitItem()
      assertEquals("req-B", second.request.id)
      assertEquals("alice", second.senderPseudo)
    }
  }

  @Test
  fun does_not_replay_old_items() = runTest {

    // Emit before collecting
    fakeFriendsRepo.incomingRequestsFlow.value = listOf(classicalFriendRequestAlice)

    vm.newRequests.test {
      expectNoEvents() // nothing is replayed
    }
  }

  @Test
  fun does_not_emit_when_request_is_already_seen() = runTest {
    vm.newRequests.test {
      fakeFriendsRepo.incomingRequestsFlow.value = listOf(classicalFriendRequestAlice)

      expectNoEvents()

      assertTrue("seen-1" !in fakeFriendsRepo.markedSeen)
    }
  }
}
