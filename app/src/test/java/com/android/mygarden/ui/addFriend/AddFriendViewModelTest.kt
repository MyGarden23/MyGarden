package com.android.mygarden.ui.addFriend

import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.FakeAchievementsRepository
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import com.android.mygarden.utils.TestPseudoRepository
import com.android.mygarden.utils.createViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [AddFriendViewModel].
 *
 * These tests:
 * - verify that the query state is updated correctly,
 * - verify that search behavior calls onError for invalid input,
 * - verify that search populates results when repositories return data,
 * - verify that adding a friend triggers onSuccess or onError as expected.
 *
 * The tests rely on fake repository implementations to avoid any real Firebase or network calls.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AddFriendViewModelTest {

  // ---------- onQueryChange ----------

  @Test
  fun onQueryChange_updates_query_and_keep_result() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository()
    val fakeRequests = FakeFriendRequestsRepository()
    val fakeUserProfile = FakeUserProfileRepository()
    val fakeProfile = FakeProfileRepository()
    val fakeAchivements = FakeAchievementsRepository()
    val fakePseudo =
        TestPseudoRepository().apply {
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }
    fakeUserProfile.profiles["uid-alice"] =
        UserProfile(
            id = "uid-alice",
            pseudo = "alice",
            avatar = Avatar.A1,
            GardeningSkill.NOVICE.name,
            "rose")

    val vm =
        AddFriendViewModel(
            friendsRepository = fakeFriends,
            requestsRepository = fakeRequests,
            userProfileRepository = fakeUserProfile,
            pseudoRepository = fakePseudo,
            profileRepository = fakeProfile,
            achievementsRepository = fakeAchivements)

    // First: perform a search to populate results
    vm.onQueryChange("al")
    vm.onSearch(onError = {})

    advanceUntilIdle()
    assertTrue(vm.uiState.value.searchResults.isNotEmpty())

    // Then: change query and ensure results are not changed
    vm.onQueryChange("newQuery")

    val state = vm.uiState.value
    assertEquals("newQuery", state.query)
    assertTrue(vm.uiState.value.searchResults.isNotEmpty())

    Dispatchers.resetMain()
  }

  // ---------- onSearch ----------

  @Test
  fun onSearch_with_short_query_calls_onError_and_does_not_start_search() = runTest {
    val vm = createViewModel()
    vm.onQueryChange("a") // 1 char

    var errorCalled = false

    vm.onSearch(onError = { errorCalled = true })

    val state = vm.uiState.value
    assertTrue(errorCalled)
    assertFalse(state.isSearching)
    assertTrue(state.searchResults.isEmpty())
  }

  @Test
  fun onSearch_with_valid_query_populates_search_results() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeUserProfile = FakeUserProfileRepository()
    val fakeFriends = FakeFriendsRepository()
    val fakeRequests = FakeFriendRequestsRepository()
    val fakeProfile = FakeProfileRepository()
    val fakeAchievements = FakeAchievementsRepository()

    // Configure a test-specific pseudo repository that returns a single match.
    val fakePseudo =
        TestPseudoRepository().apply {
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }

    // Set up the corresponding user profile for the resolved UID.
    fakeUserProfile.profiles["uid-alice"] =
        UserProfile(
            id = "uid-alice",
            pseudo = "alice",
            avatar = Avatar.A1,
            GardeningSkill.NOVICE.name,
            "rose")

    val vm =
        AddFriendViewModel(
            friendsRepository = fakeFriends,
            requestsRepository = fakeRequests,
            userProfileRepository = fakeUserProfile,
            pseudoRepository = fakePseudo,
            profileRepository = fakeProfile,
            achievementsRepository = fakeAchievements)

    vm.onQueryChange("al")

    var errorCalled = false
    vm.onSearch(onError = { errorCalled = true })

    // Run all pending coroutines launched in viewModelScope.
    advanceUntilIdle()

    val state = vm.uiState.value
    assertFalse(errorCalled)
    assertFalse(state.isSearching)
    assertEquals(1, state.searchResults.size)
    assertEquals("uid-alice", state.searchResults[0].id)
    assertEquals("alice", state.searchResults[0].pseudo)

    Dispatchers.resetMain()
  }

  // ---------- onAdd ----------

  @Test
  fun onAdd_success_calls_onSuccess_and_adds_friend() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository()
    val fakeAchievements = FakeAchievementsRepository()
    // Initialize achievements for both users to avoid null issues
    fakeAchievements.initializeAchievementsForNewUser("fake_uid")
    fakeAchievements.initializeAchievementsForNewUser("friend-123")

    val vm = createViewModel(friendsRepo = fakeFriends, achievementsRepository = fakeAchievements)

    var successCalled = false
    var errorCalled = false

    vm.onAdd(
        userId = "friend-123",
        onError = { errorCalled = true },
        onSuccess = { successCalled = true })

    advanceUntilIdle()

    assertTrue(successCalled)
    assertFalse(errorCalled)
    assertEquals(listOf("friend-123"), fakeFriends.addedFriends)

    Dispatchers.resetMain()
  }

  @Test
  fun onAdd_failure_calls_onError_only() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository().apply { throwOnAdd = IllegalStateException("boom") }
    val vm = createViewModel(friendsRepo = fakeFriends)

    var successCalled = false
    var errorCalled = false

    vm.onAdd(
        userId = "friend-123",
        onError = { errorCalled = true },
        onSuccess = { successCalled = true })

    advanceUntilIdle()

    assertTrue(errorCalled)
    assertFalse(successCalled)

    Dispatchers.resetMain()
  }

  @Test
  fun onAsk_failure_calls_onError_only() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    val vm = createViewModel()
    var onSuccessCalled = false
    var onErrorCalled = false
    vm.onAsk(
        // this user id triggers an exception on the fake repo
        userId = "boom-user-id",
        onError = { onErrorCalled = true },
        onSuccess = { onSuccessCalled = true })
    advanceUntilIdle()

    assertTrue(onErrorCalled)
    assertFalse(onSuccessCalled)
  }

  @Test
  fun searchResult_userNotFriendAndNoRequest_relationIsAdd() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository()
    val fakeRequests = FakeFriendRequestsRepository()
    val fakeUserProfile = FakeUserProfileRepository()
    val fakeAchievements = FakeAchievementsRepository()
    val profileRepo = FakeProfileRepository()
    val fakePseudo =
        TestPseudoRepository().apply {
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }
    fakeUserProfile.profiles["uid-alice"] =
        UserProfile(
            id = "uid-alice",
            pseudo = "alice",
            avatar = Avatar.A1,
            GardeningSkill.NOVICE.name,
            "rose")

    val vm =
        AddFriendViewModel(
            friendsRepository = fakeFriends,
            requestsRepository = fakeRequests,
            userProfileRepository = fakeUserProfile,
            pseudoRepository = fakePseudo,
            profileRepository = profileRepo,
            achievementsRepository = fakeAchievements)

    vm.onQueryChange("al")
    vm.onSearch(onError = {})

    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(FriendRelation.ADD, state.relations["uid-alice"])

    Dispatchers.resetMain()
  }

  @Test
  fun searchResult_userAlreadyFriend_relationIsAdded() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository().apply { friendsFlow.value = listOf("uid-alice") }
    val fakeRequests = FakeFriendRequestsRepository()
    val fakeUserProfile = FakeUserProfileRepository()
    val fakeAchievements = FakeAchievementsRepository()
    val profileRepo = FakeProfileRepository()
    val fakePseudo =
        TestPseudoRepository().apply {
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }
    fakeUserProfile.profiles["uid-alice"] =
        UserProfile(
            id = "uid-alice",
            pseudo = "alice",
            avatar = Avatar.A1,
            GardeningSkill.NOVICE.name,
            "rose")

    val vm =
        AddFriendViewModel(
            friendsRepository = fakeFriends,
            requestsRepository = fakeRequests,
            userProfileRepository = fakeUserProfile,
            pseudoRepository = fakePseudo,
            profileRepository = profileRepo,
            achievementsRepository = fakeAchievements)

    vm.onQueryChange("al")
    vm.onSearch(onError = {})

    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(FriendRelation.ADDED, state.relations["uid-alice"])

    Dispatchers.resetMain()
  }

  @Test
  fun searchResult_outgoingRequestExists_relationIsPending() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository()
    val fakeRequests =
        FakeFriendRequestsRepository(
            initialRequests =
                listOf(FriendRequest(fromUserId = "fake-uid", toUserId = "uid-alice")))
    val fakeUserProfile = FakeUserProfileRepository()
    val fakeAchievements = FakeAchievementsRepository()
    val profileRepo = FakeProfileRepository()
    val fakePseudo =
        TestPseudoRepository().apply {
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }
    fakeUserProfile.profiles["uid-alice"] =
        UserProfile(
            id = "uid-alice",
            pseudo = "alice",
            avatar = Avatar.A1,
            GardeningSkill.NOVICE.name,
            "rose")

    val vm =
        AddFriendViewModel(
            friendsRepository = fakeFriends,
            requestsRepository = fakeRequests,
            userProfileRepository = fakeUserProfile,
            pseudoRepository = fakePseudo,
            profileRepository = profileRepo,
            achievementsRepository = fakeAchievements)

    vm.onQueryChange("al")
    vm.onSearch(onError = {})

    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(FriendRelation.PENDING, state.relations["uid-alice"])

    Dispatchers.resetMain()
  }

  @Test
  fun searchResult_incomingRequestExists_relationIsAddBack() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository()

    val fakeRequests =
        FakeFriendRequestsRepository(
            initialRequests =
                listOf(
                    FriendRequest(
                        fromUserId = "uid-alice",
                        toUserId = "fake-uid",
                    )))

    val fakeUserProfile = FakeUserProfileRepository()
    val fakeAchievements = FakeAchievementsRepository()
    val profileRepo = FakeProfileRepository()
    val fakePseudo =
        TestPseudoRepository().apply {
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }

    fakeUserProfile.profiles["uid-alice"] =
        UserProfile(
            id = "uid-alice",
            pseudo = "alice",
            avatar = Avatar.A1,
            GardeningSkill.NOVICE.name,
            "rose")

    val vm =
        AddFriendViewModel(
            friendsRepository = fakeFriends,
            requestsRepository = fakeRequests,
            userProfileRepository = fakeUserProfile,
            pseudoRepository = fakePseudo,
            profileRepository = profileRepo,
            achievementsRepository = fakeAchievements)

    vm.onQueryChange("al")
    vm.onSearch(onError = {})

    advanceUntilIdle()

    val state = vm.uiState.value
    assertEquals(FriendRelation.ADDBACK, state.relations["uid-alice"])

    Dispatchers.resetMain()
  }
}
