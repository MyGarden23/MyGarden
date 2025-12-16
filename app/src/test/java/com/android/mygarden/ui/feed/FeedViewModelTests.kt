package com.android.mygarden.ui.feed

import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.GardenActivity
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.navigation.NavigationActions
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.*
import java.sql.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTests {
  /*--------------- FICTIONAL PLANTS / ACTIVITIES --------------*/
  val plant1 =
      Plant(
          "hello",
          null,
          "laurem ipsum",
          "beautiful plant",
          PlantLocation.INDOOR,
          "Direct light",
          PlantHealthStatus.HEALTHY,
          "is healthy",
          10)

  val ownedPlant1 = OwnedPlant("id1", plant1, Timestamp(System.currentTimeMillis()))

  val addedPlantActivity =
      ActivityAddedPlant("uid", "gregory", Timestamp(System.currentTimeMillis()), ownedPlant1)

  val userProfile1 =
      UserProfile(
          id = "uid",
          pseudo = "gregory",
          avatar = Avatar.A1,
          gardeningSkill = "Beginner",
          favoritePlant = "Rose")

  val userProfile2 =
      UserProfile(
          id = "friend-123",
          pseudo = "friendPseudo",
          avatar = Avatar.A2,
          gardeningSkill = "Expert",
          favoritePlant = "Cactus")

  /*---------------- FAKE ACTIVITY REPOSITORY TO USE FOR TESTING ----------------*/
  private class FakeActivityRepository : ActivityRepository {

    val activitiesFlow = MutableStateFlow<List<GardenActivity>>(emptyList())
    var lastFriendsList: List<String>? = null

    override fun getCurrentUserId(): String = "fake-uid"

    override fun getActivities(): Flow<List<GardenActivity>> = activitiesFlow

    override fun getActivitiesForUser(userId: String): Flow<List<GardenActivity>> {
      return emptyFlow()
    }

    override fun getFeedActivities(userIds: List<String>, limit: Int): Flow<List<GardenActivity>> {
      lastFriendsList = userIds
      return activitiesFlow
    }

    override suspend fun addActivity(activity: GardenActivity) {
      val currentFlowValue = activitiesFlow.value
      activitiesFlow.value = currentFlowValue + activity
    }

    override fun cleanup() {
      activitiesFlow.value = emptyList()
    }
  }

  /*---------------- ENHANCED FAKE PROFILE REPOSITORY ----------------*/
  private class EnhancedFakeProfileRepository(private val currentUserPseudo: String = "gregory") :
      ProfileRepository {
    override fun getCurrentUserId(): String? = "fake-uid"

    override fun getProfile() = emptyFlow<Profile?>()

    override suspend fun saveProfile(profile: Profile) {}

    override suspend fun attachFCMToken(token: String): Boolean = false

    override suspend fun getFCMToken(): String? = null

    override suspend fun isCurrentUserPseudo(pseudo: String): Boolean {
      return pseudo == currentUserPseudo
    }

    override fun cleanup() {}
  }

  /*---------------- ENHANCED FAKE USER PROFILE REPOSITORY ----------------*/
  private class EnhancedFakeUserProfileRepository : UserProfileRepository {
    val profiles: MutableMap<String, UserProfile> = mutableMapOf()

    override suspend fun getUserProfile(userId: String): UserProfile? = profiles[userId]
  }

  /*---------------- ENHANCED FAKE FRIEND REQUESTS REPOSITORY ----------------*/
  private class EnhancedFakeFriendRequestsRepository : FriendRequestsRepository {
    val incomingRequestsFlow = MutableStateFlow<List<FriendRequest>>(emptyList())
    val outgoingRequestsSet = mutableSetOf<String>()
    var lastAskedFriendId: String? = null
    var currentUserIdValue: String? = "fake-uid"

    override fun getCurrentUserId(): String? = currentUserIdValue

    override fun myRequests(): Flow<List<FriendRequest>> = MutableStateFlow(emptyList())

    override fun incomingRequests(): Flow<List<FriendRequest>> = incomingRequestsFlow

    override fun outgoingRequests(): Flow<List<FriendRequest>> = MutableStateFlow(emptyList())

    override suspend fun isInIncomingRequests(targetUserId: String): Boolean {
      val requests = incomingRequestsFlow.value
      return requests.any { it.fromUserId == targetUserId }
    }

    override suspend fun isInOutgoingRequests(targetUserId: String): Boolean {
      return outgoingRequestsSet.contains(targetUserId)
    }

    override suspend fun askFriend(targetUserId: String) {
      lastAskedFriendId = targetUserId
      outgoingRequestsSet.add(targetUserId)
      val currentUserId = getCurrentUserId() ?: "fake-uid"
      incomingRequestsFlow.value =
          incomingRequestsFlow.value +
              FriendRequest(fromUserId = currentUserId, toUserId = targetUserId)
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

    override suspend fun deleteRequest(requestId: String) {
      incomingRequestsFlow.value = incomingRequestsFlow.value.filter { it.fromUserId != requestId }
    }

    override fun cleanup() {}
  }

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var repositoryScope: TestScope
  private lateinit var activityRepo: ActivityRepository
  private lateinit var friendsRepo: FriendsRepository
  private lateinit var friendsRequestsRepo: EnhancedFakeFriendRequestsRepository
  private lateinit var userProfileRepo: EnhancedFakeUserProfileRepository
  private lateinit var profileRepo: EnhancedFakeProfileRepository
  private lateinit var vm: FeedViewModel

  /** Sets up the correct scopes, the repository and the view model */
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repositoryScope = TestScope(SupervisorJob() + testDispatcher)
    activityRepo = FakeActivityRepository()
    friendsRepo = FakeFriendsRepository()
    friendsRequestsRepo = EnhancedFakeFriendRequestsRepository()
    userProfileRepo = EnhancedFakeUserProfileRepository()
    profileRepo = EnhancedFakeProfileRepository()
    UserProfileRepositoryProvider.repository = userProfileRepo
    ProfileRepositoryProvider.repository = profileRepo
    vm = FeedViewModel(activityRepo, friendsRepo, friendsRequestsRepo, userProfileRepo, profileRepo)
  }

  /** Resets the scopes and ensures the clear of the list of activities */
  @After
  fun clean() {
    activityRepo.cleanup()
    Dispatchers.resetMain()
  }

  /** Tests that initially a empty list is collected */
  @Test
  fun initialEmptyCorrectlyCollected() {
    assertEquals(emptyList<GardenActivity>(), vm.uiState.value.activities)
  }

  /** Tests that initially the correct list (non empty) is collected */
  @Test
  fun initialNonEmptyCorrectlyCollected() = runTest {
    activityRepo.addActivity(addedPlantActivity)
    // new instance created after the activity is added to the repository
    val newVM =
        FeedViewModel(activityRepo, friendsRepo, friendsRequestsRepo, userProfileRepo, profileRepo)
    runCurrent()
    assertEquals(listOf(addedPlantActivity), newVM.uiState.value.activities)
  }

  /** Tests that the list is correctly updated after a modification from the repo */
  @Test
  fun correctCollectionAfterAddedActivity() = runTest {
    activityRepo.addActivity(addedPlantActivity)
    runCurrent()
    vm.refreshUIState()
    runCurrent()
    assertEquals(listOf(addedPlantActivity), vm.uiState.value.activities)
  }

  @Test
  fun feedUsesCurrentUserOnlyWhenNoFriends() = runTest {
    val fakeRepo = activityRepo as FakeActivityRepository
    FeedViewModel(fakeRepo, friendsRepo, friendsRequestsRepo, userProfileRepo, profileRepo)
    runCurrent()
    assertEquals(listOf("fake-uid"), fakeRepo.lastFriendsList)
  }

  @Test
  fun activitiesUpdateAfterFriendListChanges() = runTest {
    val fakeActivityRepo = activityRepo as FakeActivityRepository
    val fakeFriendsRepo = friendsRepo as FakeFriendsRepository

    val vm =
        FeedViewModel(
            fakeActivityRepo, fakeFriendsRepo, friendsRequestsRepo, userProfileRepo, profileRepo)
    runCurrent()

    assertTrue(vm.uiState.value.activities.isEmpty())

    fakeFriendsRepo.addFriend("friend-456")
    runCurrent()

    // add a plant should emit after friends list change
    fakeActivityRepo.addActivity(addedPlantActivity)
    runCurrent()

    assertEquals(listOf(addedPlantActivity), vm.uiState.value.activities)
  }

  /*---------------- NEW TESTS FOR WATCHING FRIENDS ACTIVITY ----------------*/

  @Test
  fun setWatchedFriends_updatesWatchedUsers() = runTest {
    userProfileRepo.profiles["uid"] = userProfile1
    userProfileRepo.profiles["friend-123"] = userProfile2

    vm.setWatchedFriends(userProfile1, userProfile2)

    assertEquals(userProfile1, vm.uiState.value.watchedUser1)
    assertEquals(userProfile2, vm.uiState.value.watchedUser2)
  }

  @Test
  fun setWatchedFriends_setsRelationToSelfForCurrentUser() = runTest {
    userProfileRepo.profiles["uid"] = userProfile1
    profileRepo = EnhancedFakeProfileRepository(currentUserPseudo = "gregory")
    ProfileRepositoryProvider.repository = profileRepo
    val testVm =
        FeedViewModel(activityRepo, friendsRepo, friendsRequestsRepo, userProfileRepo, profileRepo)

    testVm.setWatchedFriends(userProfile1, null)

    assertEquals(RelationWithWatchedUser.SELF, testVm.uiState.value.relationWithWatchedUser1)
  }

  @Test
  fun setWatchedFriends_setsRelationToFriendWhenIsFriend() = runTest {
    val fakeFriendsRepo = friendsRepo as FakeFriendsRepository
    fakeFriendsRepo.addFriend("friend-123")
    runCurrent()

    vm.setWatchedFriends(userProfile2, null)

    assertEquals(RelationWithWatchedUser.FRIEND, vm.uiState.value.relationWithWatchedUser1)
  }

  @Test
  fun setWatchedFriends_setsRelationToRequestSentWhenInOutgoingRequests() = runTest {
    friendsRequestsRepo.outgoingRequestsSet.add("friend-123")

    vm.setWatchedFriends(userProfile2, null)

    assertEquals(RelationWithWatchedUser.REQUEST_SENT, vm.uiState.value.relationWithWatchedUser1)
  }

  @Test
  fun setWatchedFriends_setsRelationToNotFriendWhenNoRelation() = runTest {
    vm.setWatchedFriends(userProfile2, null)

    assertEquals(RelationWithWatchedUser.NOT_FRIEND, vm.uiState.value.relationWithWatchedUser1)
  }

  @Test
  fun resetWatchedFriends_clearsAllWatchedData() {
    vm.setIsWatchingFriendsActivity(true)
    vm.resetWatchedFriends(userProfile1, userProfile2)

    assertNull(vm.uiState.value.watchedUser1)
    assertNull(vm.uiState.value.watchedUser2)
    assertEquals(RelationWithWatchedUser.SELF, vm.uiState.value.relationWithWatchedUser1)
    assertEquals(RelationWithWatchedUser.SELF, vm.uiState.value.relationWithWatchedUser2)
  }

  /*---------------- TESTS FOR ACTIVITY CLICK HANDLERS ----------------*/

  @Test
  fun handleFriendActivityClick_navigatesToFriendGarden() {
    val mockNavigationActions = mock(NavigationActions::class.java)
    val testVm =
        FeedViewModel(
            activityRepo,
            friendsRepo,
            friendsRequestsRepo,
            userProfileRepo,
            profileRepo,
            mockNavigationActions)

    testVm.handleFriendActivityClick("friend-123")

    verify(mockNavigationActions).navTo(Screen.FriendGarden("friend-123"))
  }

  @Test
  fun handleNotFriendActivityClick_sendsRequestAndUpdatesWatchedFriends() = runTest {
    userProfileRepo.profiles["friend-123"] = userProfile2
    vm.updateWatchedFriends(userProfile2, null)
    runCurrent()

    vm.handleNotFriendActivityClick("friend-123")
    runCurrent()

    assertEquals("friend-123", friendsRequestsRepo.lastAskedFriendId)
    // Relation should be updated after sending request
    assertTrue(friendsRequestsRepo.outgoingRequestsSet.contains("friend-123"))
  }

  @Test
  fun handleSelfActivityClick_navigatesToGarden() {
    val mockNavigationActions = mock(NavigationActions::class.java)
    val testVm =
        FeedViewModel(
            activityRepo,
            friendsRepo,
            friendsRequestsRepo,
            userProfileRepo,
            profileRepo,
            mockNavigationActions)

    testVm.handleSelfActivityClick()

    verify(mockNavigationActions).navTo(Screen.Garden)
  }
}
