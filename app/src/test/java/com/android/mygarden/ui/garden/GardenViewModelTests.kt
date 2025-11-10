package com.android.mygarden.ui.garden

import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.ui.profile.Avatar
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class GardenViewModelTests {

  private lateinit var plantsRepo: PlantsRepository
  private lateinit var profileRepo: ProfileRepository
  private lateinit var vm: GardenViewModel
  private lateinit var ownedPlant: OwnedPlant
  val plant1 =
      Plant(
          "hello",
          null,
          "laurem ipsum",
          "beautiful plant",
          PlantHealthStatus.HEALTHY,
          "is healthy",
          2)
  private val testDispatcher = StandardTestDispatcher()

  /** Fake profile local repository used to test the viewModel/profile interactions */
  private class FakeProfileRepository(
      initialProfile: Profile? =
          Profile(
              firstName = "Test",
              lastName = "User",
              gardeningSkill = GardeningSkill.BEGINNER,
              favoritePlant = "Rose",
              country = "Switzerland",
              hasSignedIn = true,
              avatar = Avatar.A1)
  ) : ProfileRepository {

    private val flow = MutableStateFlow(initialProfile)

    override fun getCurrentUserId(): String = "fake-uid"

    override fun getProfile(): Flow<Profile?> = flow

    override suspend fun saveProfile(profile: Profile) {
      flow.value = profile
    }
  }

  private lateinit var repositoryScope: TestScope
  /** Sets up the repository and the view model and the test dispatcher to simulate the app */
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repositoryScope = TestScope(SupervisorJob() + testDispatcher)
    plantsRepo = PlantsRepositoryLocal(repositoryScope)
    profileRepo = FakeProfileRepository()
    vm = GardenViewModel(plantsRepo = plantsRepo, profileRepo = profileRepo)
  }

  /** Ensures the reset of the dispatcher at each end of test */
  @After
  fun end() {
    Dispatchers.resetMain()
  }

  /** Tests that the setErrorMessage function updates correctly the vm */
  @Test
  fun setErrorMsgUpdatesCorrectly() {
    val errorMsg = "Error"
    vm.setErrorMsg(errorMsg)
    assertEquals(errorMsg, vm.uiState.value.errorMsg)
  }

  /** Tests that the clearErrorMessage function updates correctly the vm */
  @Test
  fun clearErrorMsgClearsCorrectly() {
    val errorMsg = "Error"
    vm.setErrorMsg(errorMsg)
    assertEquals(errorMsg, vm.uiState.value.errorMsg)
    vm.clearErrorMsg()
    assertNull(vm.uiState.value.errorMsg)
  }

  /** Tests that fetchProfileInfos works correctly with a fake profile repository */
  @Test
  fun fetchProfileInfoWorksCorrectly() = runTest {
    val expected =
        Profile(
            firstName = "Test",
            lastName = "User",
            gardeningSkill = GardeningSkill.BEGINNER,
            favoritePlant = "Rose",
            country = "Switzerland",
            hasSignedIn = true,
            avatar = Avatar.A1)

    vm.refreshUIState()
    runCurrent()

    assertEquals(expected.firstName, vm.uiState.value.userName)
    assertEquals(expected.avatar, vm.uiState.value.userAvatar)
    repositoryScope.cancel()
  }

  @Test
  fun waterPlantWorksCorrectly() = runTest {
    // Last watered = 4 days ago
    val initialLastWatered = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(4))
    val saved = plantsRepo.saveToGarden(plant1, plantsRepo.getNewId(), initialLastWatered)

    runCurrent()
    vm.refreshUIState()
    runCurrent()

    val before = vm.uiState.value.plants.single()
    assertEquals(saved.id, before.id)
    assertEquals(before.plant.healthStatus, PlantHealthStatus.SEVERELY_DRY)
    assertEquals(initialLastWatered.time, before.lastWatered.time)

    vm.waterPlant(before)
    vm.refreshUIState()
    runCurrent()

    val after = vm.uiState.value.plants.single()
    assertEquals(before.id, after.id)
    assertEquals(after.plant.healthStatus, PlantHealthStatus.HEALTHY)
    assertTrue(after.lastWatered.time >= before.lastWatered.time)

    repositoryScope.cancel()
  }
}
