package com.android.mygarden.ui.addFriend

import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.profile.PseudoRepository
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakePseudoRepository
import com.android.mygarden.utils.FakeUserProfileRepository
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
  fun onQueryChange_updates_query_and_clears_results() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository()
    val fakeUserProfile = FakeUserProfileRepository()
    val fakePseudo =
        TestPseudoRepository().apply {
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }
    fakeUserProfile.profiles["uid-alice"] =
        UserProfile(id = "uid-alice", pseudo = "alice", avatar = Avatar.A1)

    val vm =
        AddFriendViewModel(
            friendsRepository = fakeFriends,
            userProfileRepository = fakeUserProfile,
            pseudoRepository = fakePseudo)

    // First: perform a search to populate results
    vm.onQueryChange("al")
    vm.onSearch(onError = {})

    advanceUntilIdle()
    assertTrue(vm.uiState.value.searchResults.isNotEmpty())

    // Then: change query and ensure results are cleared
    vm.onQueryChange("newQuery")

    val state = vm.uiState.value
    assertEquals("newQuery", state.query)
    assertTrue(state.searchResults.isEmpty())

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

    // Configure a test-specific pseudo repository that returns a single match.
    val fakePseudo =
        TestPseudoRepository().apply {
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }

    // Set up the corresponding user profile for the resolved UID.
    fakeUserProfile.profiles["uid-alice"] =
        UserProfile(id = "uid-alice", pseudo = "alice", avatar = Avatar.A1)

    val vm =
        AddFriendViewModel(
            friendsRepository = fakeFriends,
            userProfileRepository = fakeUserProfile,
            pseudoRepository = fakePseudo)

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
    val vm = createViewModel(friendsRepo = fakeFriends)

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

  // --------------------------------------------------------------------
  // Helpers
  // --------------------------------------------------------------------

  /**
   * Creates an [AddFriendViewModel] with fake repositories by default.
   *
   * Callers can override individual repositories when they need specific behavior for a given test.
   */
  private fun createViewModel(
      friendsRepo: FriendsRepository = FakeFriendsRepository(),
      userProfileRepo: UserProfileRepository = FakeUserProfileRepository(),
      pseudoRepo: PseudoRepository = FakePseudoRepository()
  ): AddFriendViewModel =
      AddFriendViewModel(
          friendsRepository = friendsRepo,
          userProfileRepository = userProfileRepo,
          pseudoRepository = pseudoRepo)

  /**
   * Test-only fake implementation of [PseudoRepository] used in this test class.
   *
   * It allows configuring:
   * - [searchResults] for [searchPseudoStartingWith],
   * - [uidMap] for [getUidFromPseudo].
   *
   * This avoids changing the global [FakePseudoRepository] used in other tests.
   */
  private class TestPseudoRepository : PseudoRepository {

    /** List of pseudos returned by [searchPseudoStartingWith]. */
    var searchResults: List<String> = emptyList()

    /** Mapping from pseudo (lowercased) to UID returned by [getUidFromPseudo]. */
    val uidMap: MutableMap<String, String> = mutableMapOf()

    override suspend fun isPseudoAvailable(pseudo: String) = true

    override suspend fun savePseudo(pseudo: String, userId: String) {
      uidMap[pseudo.lowercase()] = userId
    }

    override suspend fun deletePseudo(pseudo: String) {
      uidMap.remove(pseudo.lowercase())
    }

    override suspend fun searchPseudoStartingWith(query: String): List<String> = searchResults

    override suspend fun getUidFromPseudo(pseudo: String): String? = uidMap[pseudo.lowercase()]
  }
}
