package com.android.mygarden.ui.feed

import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.ui.profile.Avatar
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

  /*---------------- FAKE PROFILE REPOSITORY TO USE FOR TESTING ----------------*/
  private class FakeProfileRepository(
      initProfile: Profile? =
          Profile(
              firstName = "Test",
              lastName = "User",
              gardeningSkill = GardeningSkill.BEGINNER,
              favoritePlant = "Rose",
              country = "Switzerland",
              hasSignedIn = true,
              avatar = Avatar.A1)
  ) : ProfileRepository {

    private val flow = MutableStateFlow(initProfile)
    val activitiesFlow = MutableStateFlow<List<GardenActivity>>(emptyList())

    override fun getCurrentUserId(): String = "fake-uid"

    override fun getProfile(): Flow<Profile?> = flow

    override suspend fun saveProfile(profile: Profile) {
      flow.value = profile
    }

    override suspend fun attachFCMToken(token: String): Boolean {
      return false
    }

    override suspend fun getFCMToken(): String? {
      return null
    }

    override fun getActivities(): Flow<List<GardenActivity>> = activitiesFlow

    override fun getActivitiesForUser(userId: String): Flow<List<GardenActivity>> {
      return emptyFlow()
    }

    override fun getFeedActivities(userIds: List<String>, limit: Int): Flow<List<GardenActivity>> {
      return emptyFlow()
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
  private lateinit var profileRepo: ProfileRepository
  private lateinit var vm: FeedViewModel

  /** Sets up the correct scopes, the repository and the view model */
  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repositoryScope = TestScope(SupervisorJob() + testDispatcher)
    profileRepo = FakeProfileRepository()
    vm = FeedViewModel(profileRepo)
  }

  /** Resets the scopes and ensures the clear of the list of activities */
  @After
  fun clean() {
    profileRepo.cleanup()
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
    profileRepo.addActivity(addedPlantActivity)
    // new instance created after the activity is added to the repository
    val newVM = FeedViewModel(profileRepo)
    runCurrent()
    assertEquals(listOf(addedPlantActivity), newVM.uiState.value.activities)
  }

  /** Tests that the list is correctly updated after a modification from the repo */
  @Test
  fun correctCollectionAfterAddedActivity() = runTest {
    profileRepo.addActivity(addedPlantActivity)
    runCurrent()
    vm.refreshUIState()
    runCurrent()
    assertEquals(listOf(addedPlantActivity), vm.uiState.value.activities)
  }
}
