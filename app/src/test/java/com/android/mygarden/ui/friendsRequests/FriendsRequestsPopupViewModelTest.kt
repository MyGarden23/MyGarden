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

  @Before
  fun setup() {
    fakeFriendsRepo = FakeFriendRequestsRepository()
    fakeUserRepo = FakeUserProfileRepository()

    vm =
        FriendsRequestsPopupViewModel(friendsRepo = fakeFriendsRepo, userProfileRepo = fakeUserRepo)
  }

  @Test
  fun emits_FriendRequestUiModel_when_new_incoming_request_arrives() = runTest {
    // Setup sender profile
    fakeUserRepo.profiles["sender-1"] =
        UserProfile(id = "sender-1", pseudo = "Alice", avatar = Avatar.A10)

    // Create the request to send
    val req =
        FriendRequest(
            id = "req-1",
            fromUserId = "sender-1",
            toUserId = "rx",
            status = FriendRequestStatus.PENDING)

    vm.newRequests.test {
      // Emit request
      fakeFriendsRepo.incomingRequestsFlow.value = listOf(req)

      val emitted = awaitItem()
      assertEquals("req-1", emitted.request.id)
      assertEquals("Alice", emitted.senderPseudo)
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
    fakeUserRepo.profiles["bob"] = UserProfile("bob", "Bob", Avatar.A11)

    val req = FriendRequest(id = "req-3", fromUserId = "bob", toUserId = "rx")

    vm.newRequests.test {
      fakeFriendsRepo.incomingRequestsFlow.value = listOf(req)

      awaitItem()
      assertTrue("req-3" in fakeFriendsRepo.markedSeen)
    }
  }

  @Test
  fun emits_multiple_incoming_requests_in_order() = runTest {
    fakeUserRepo.profiles["u1"] = UserProfile("u1", "One", Avatar.A11)
    fakeUserRepo.profiles["u2"] = UserProfile("u2", "Two", Avatar.A11)

    val req1 = FriendRequest(id = "A", fromUserId = "u1", toUserId = "rx")
    val req2 = FriendRequest(id = "B", fromUserId = "u2", toUserId = "rx")

    vm.newRequests.test {
      fakeFriendsRepo.incomingRequestsFlow.value = listOf(req1, req2)

      val first = awaitItem()
      assertEquals("A", first.request.id)
      assertEquals("One", first.senderPseudo)

      val second = awaitItem()
      assertEquals("B", second.request.id)
      assertEquals("Two", second.senderPseudo)
    }
  }

  @Test
  fun does_not_replay_old_items() = runTest {
    fakeUserRepo.profiles["sender"] = UserProfile("sender", "Pseudo", Avatar.A11)

    // Emit before collecting
    fakeFriendsRepo.incomingRequestsFlow.value =
        listOf(FriendRequest(id = "old", fromUserId = "sender", toUserId = "rx"))

    vm.newRequests.test {
      expectNoEvents() // nothing is replayed
    }
  }

  @Test
  fun does_not_emit_when_request_is_already_seen() = runTest {
    fakeUserRepo.profiles["sender"] = UserProfile("sender", "Alice", Avatar.A11)

    val alreadySeen =
        FriendRequest(id = "seen-1", fromUserId = "sender", toUserId = "rx", seenByReceiver = true)

    vm.newRequests.test {
      fakeFriendsRepo.incomingRequestsFlow.value = listOf(alreadySeen)

      expectNoEvents()

      assertTrue("seen-1" !in fakeFriendsRepo.markedSeen)
    }
  }
}
