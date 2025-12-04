package com.android.mygarden.ui.feed

import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.GardenActivity
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeFriendsRepository
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

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var repositoryScope: TestScope
  private lateinit var activityRepo: ActivityRepository
  private lateinit var friendsRepo: FriendsRepository
  private lateinit var friendsRequestsRepo: FriendRequestsRepository
  private lateinit var vm: FeedViewModel

  /** Sets up the correct scopes, the repository and the view model */
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repositoryScope = TestScope(SupervisorJob() + testDispatcher)
    activityRepo = FakeActivityRepository()
    friendsRepo = FakeFriendsRepository()
    friendsRequestsRepo = FakeFriendRequestsRepository()
    vm = FeedViewModel(activityRepo, friendsRepo, friendsRequestsRepo)
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
    val newVM = FeedViewModel(activityRepo, friendsRepo, friendsRequestsRepo)
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
    val vm = FeedViewModel(fakeRepo, friendsRepo, friendsRequestsRepo)
    runCurrent()
    assertEquals(listOf("fake-uid"), fakeRepo.lastFriendsList)
  }

  @Test
  fun activitiesUpdateAfterFriendListChanges() = runTest {
    val fakeActivityRepo = activityRepo as FakeActivityRepository
    val fakeFriendsRepo = friendsRepo as FakeFriendsRepository

    val vm = FeedViewModel(fakeActivityRepo, fakeFriendsRepo, friendsRequestsRepo)
    runCurrent()

    assertTrue(vm.uiState.value.activities.isEmpty())

    fakeFriendsRepo.addFriend("friend-456")
    runCurrent()

    // add a plant should emit after friends list change
    fakeActivityRepo.addActivity(addedPlantActivity)
    runCurrent()

    assertEquals(listOf(addedPlantActivity), vm.uiState.value.activities)
  }
}
