package com.android.mygarden.ui.friendList

import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.FakeAchievementsRepository
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FriendListViewModelTest {

  @Test
  fun getFriends_populates_uiState_with_friend_profiles() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository()
    val fakeUserProfiles = FakeUserProfileRepository()
    val fakeAchievements = FakeAchievementsRepository()
    val fakeRequest = FakeFriendRequestsRepository()

    // Mock FirebaseAuth.currentUser
    val auth: FirebaseAuth = mock()
    val user: FirebaseUser = mock()
    whenever(auth.currentUser).thenReturn(user)
    whenever(user.uid).thenReturn("current-user-id")

    // Configure FakeFriendsRepository: feed friend UIDs via the flow
    fakeFriends.friendsFlow.value = listOf("uid-alice", "uid-bob")

    // Configure FakeUserProfileRepository: map UID -> UserProfile
    fakeUserProfiles.profiles["uid-alice"] =
        UserProfile("uid-alice", "Alice", Avatar.A1, "Beginner", "Rose")
    fakeUserProfiles.profiles["uid-bob"] =
        UserProfile("uid-bob", "Bob", Avatar.A2, "Expert", "Cactus")

    val vm =
        FriendListViewModel(
            friendsRepository = fakeFriends,
            userProfileRepository = fakeUserProfiles,
            requestRepo = fakeRequest,
            auth = auth,
            achievementsRepo = fakeAchievements)

    var onErrorCalled = false
    vm.getFriends(onError = { onErrorCalled = true })

    advanceUntilIdle()

    val state = vm.uiState.value
    assertTrue(!onErrorCalled)
    assertEquals(2, state.friends.size)
    assertEquals("Alice", state.friends[0].pseudo)
    assertEquals("Bob", state.friends[1].pseudo)

    Dispatchers.resetMain()
  }

  @Test
  fun getFriends_calls_onError_when_repository_throws() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    // Small local fake that throws on getFriends
    val throwingFriendsRepo =
        object : com.android.mygarden.model.friends.FriendsRepository {
          override suspend fun getFriends(userId: String): List<String> {
            throw IllegalStateException("boom")
          }

          override fun friendsFlow(userId: String) = FakeFriendsRepository().friendsFlow

          override suspend fun addFriend(friendUserId: String) = Unit

          override suspend fun deleteFriend(friendUserId: String) {
            /* doesn't do anything yet*/
          }
        }

    val fakeUserProfiles = FakeUserProfileRepository()
    val fakeAchievements = FakeAchievementsRepository()
    val fakeRequest = FakeFriendRequestsRepository()

    val auth: FirebaseAuth = mock()
    val user: FirebaseUser = mock()
    whenever(auth.currentUser).thenReturn(user)
    whenever(user.uid).thenReturn("current-user-id")

    val vm =
        FriendListViewModel(
            friendsRepository = throwingFriendsRepo,
            userProfileRepository = fakeUserProfiles,
            requestRepo = fakeRequest,
            auth = auth,
            achievementsRepo = fakeAchievements)

    var onErrorCalled = false
    vm.getFriends(onError = { onErrorCalled = true })

    advanceUntilIdle()

    assertTrue(onErrorCalled)

    Dispatchers.resetMain()
  }

  @Test
  fun deleteFriendChangesTheList() = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)

    val fakeFriends = FakeFriendsRepository()
    val fakeUserProfiles = FakeUserProfileRepository()
    val fakeAchievements = FakeAchievementsRepository()
    val fakeRequest = FakeFriendRequestsRepository()

    // initialize achievements so view model doesn't throw on delete
    runBlocking {
      fakeAchievements.initializeAchievementsForNewUser("fake-uid")
      fakeAchievements.initializeAchievementsForNewUser("uid-alice")
      fakeAchievements.initializeAchievementsForNewUser("uid-bob")

      fakeAchievements.updateAchievementValue("fake-uid", AchievementType.FRIENDS_NUMBER, 1)
      fakeAchievements.updateAchievementValue("uid-alice", AchievementType.FRIENDS_NUMBER, 1)
      fakeAchievements.updateAchievementValue("uid-bob", AchievementType.FRIENDS_NUMBER, 1)
    }

    // Mock FirebaseAuth.currentUser
    val auth: FirebaseAuth = mock()
    val user: FirebaseUser = mock()
    whenever(auth.currentUser).thenReturn(user)
    whenever(user.uid).thenReturn("current-user-id")

    // Configure FakeFriendsRepository: feed friend UIDs via the flow
    fakeFriends.friendsFlow.value = listOf("uid-alice", "uid-bob")

    // Configure FakeUserProfileRepository: map UID -> UserProfile
    fakeUserProfiles.profiles["uid-alice"] =
        UserProfile("uid-alice", "Alice", Avatar.A1, "Beginner", "Rose")
    fakeUserProfiles.profiles["uid-bob"] =
        UserProfile("uid-bob", "Bob", Avatar.A2, "Expert", "Cactus")

    val vm =
        FriendListViewModel(
            friendsRepository = fakeFriends,
            userProfileRepository = fakeUserProfiles,
            requestRepo = fakeRequest,
            auth = auth,
            achievementsRepo = fakeAchievements)

    vm.getFriends {}
    advanceUntilIdle()

    assertEquals(2, vm.uiState.value.friends.size)

    vm.deleteFriend(vm.uiState.value.friends[0])
    advanceUntilIdle()

    assertEquals(1, vm.uiState.value.friends.size)
    assertEquals("Bob", vm.uiState.value.friends[0].pseudo)

    Dispatchers.resetMain()
  }
}
