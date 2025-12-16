package com.android.mygarden.ui.garden

import androidx.test.core.app.ApplicationProvider
import com.android.mygarden.R
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.FakeActivityRepository
import com.android.mygarden.utils.FakeProfileRepository
import com.google.firebase.FirebaseApp
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GardenViewModelTests {

  private lateinit var plantsRepo: PlantsRepository
  private lateinit var profileRepo: ProfileRepository
  private lateinit var activityRepo: FakeActivityRepository
  private lateinit var vm: GardenViewModel
  val plant1 =
      Plant(
          "hello",
          null,
          "laurem ipsum",
          "beautiful plant",
          PlantLocation.INDOOR,
          "Test light exposure",
          PlantHealthStatus.HEALTHY,
          "is healthy",
          2)
  private val testDispatcher = StandardTestDispatcher()

  private lateinit var repositoryScope: TestScope
  /** Sets up the repository and the view model and the test dispatcher to simulate the app */
  @Before
  fun setUp() {
    // Initialize Firebase for tests
    if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
      FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }
    Dispatchers.setMain(testDispatcher)
    repositoryScope = TestScope(SupervisorJob() + testDispatcher)
    plantsRepo = PlantsRepositoryLocal(repositoryScope)
    profileRepo =
        FakeProfileRepository(
            Profile(
                firstName = "Test",
                lastName = "User",
                pseudo = "pseudo",
                gardeningSkill = GardeningSkill.BEGINNER,
                favoritePlant = "Rose",
                country = "Switzerland",
                hasSignedIn = true,
                avatar = Avatar.A1))
    activityRepo = FakeActivityRepository()
    vm =
        GardenViewModel(
            plantsRepo = plantsRepo, profileRepo = profileRepo, activityRepo = activityRepo)
  }

  /** Ensures the reset of the dispatcher at each end of test */
  @After
  fun end() {
    Dispatchers.resetMain()
  }

  /** Tests that the setErrorMessage function updates correctly the vm */
  @Test
  fun setErrorMsgUpdatesCorrectly() {
    vm.setErrorMsg(R.string.error_fetch_all_plants_garden)
    assertEquals(R.string.error_fetch_all_plants_garden, vm.uiState.value.errorMsg)
  }

  /** Tests that the clearErrorMessage function updates correctly the vm */
  @Test
  fun clearErrorMsgClearsCorrectly() {
    vm.setErrorMsg(R.string.error_fetch_all_plants_garden)
    assertEquals(R.string.error_fetch_all_plants_garden, vm.uiState.value.errorMsg)
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
            pseudo = "pseudo",
            gardeningSkill = GardeningSkill.BEGINNER,
            favoritePlant = "Rose",
            country = "Switzerland",
            hasSignedIn = true,
            avatar = Avatar.A1)

    vm.refreshUIState()
    runCurrent()

    assertEquals(expected.pseudo, vm.uiState.value.userName)
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

  @Test
  fun waterPlantCreatesActivity() = runTest {
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

    // Ensure no activities exist before watering
    assertEquals(0, activityRepo.addedActivities.size)

    vm.waterPlant(before)
    runCurrent()

    // Verify activity was created
    assertEquals(1, activityRepo.addedActivities.size)
    val activity = activityRepo.addedActivities[0]
    assertTrue(activity is ActivityWaterPlant)

    val waterActivity = activity as ActivityWaterPlant
    assertEquals("fake-uid", waterActivity.userId)
    assertEquals("pseudo", waterActivity.pseudo)
    assertEquals(before.id, waterActivity.ownedPlant.id)
    assertEquals(before.plant.name, waterActivity.ownedPlant.plant.name)

    repositoryScope.cancel()
  }

  /** Tests that sorting by plant name works correctly */
  @Test
  fun sortingByPlantNameWorks() = runTest {
    // Create plants with different names
    val plantA =
        Plant(
            "Apple",
            null,
            "Malus",
            "A fruit",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "Good",
            3)
    val plantC =
        Plant(
            "Cherry",
            null,
            "Prunus",
            "A fruit",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "Good",
            3)
    val plantB =
        Plant(
            "Banana",
            null,
            "Musa",
            "A fruit",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "Good",
            3)

    // Add plants in non-alphabetical order

    plantsRepo.saveToGarden(plantA, plantsRepo.getNewId(), Timestamp(System.currentTimeMillis()))
    plantsRepo.saveToGarden(plantC, plantsRepo.getNewId(), Timestamp(System.currentTimeMillis()))
    plantsRepo.saveToGarden(plantB, plantsRepo.getNewId(), Timestamp(System.currentTimeMillis()))
    runCurrent()

    vm.refreshUIState()
    runCurrent()

    // Set sort option to plant name (default)
    vm.setSortOption(SortOption.PLANT_NAME)
    runCurrent()

    // Verify plants are sorted alphabetically by name
    val sortedPlants = vm.uiState.value.filteredAndSortedPlants
    assertEquals(3, sortedPlants.size)
    assertEquals("Apple", sortedPlants[0].plant.name)
    assertEquals("Banana", sortedPlants[1].plant.name)
    assertEquals("Cherry", sortedPlants[2].plant.name)
    repositoryScope.cancel()
  }

  /** Tests that filtering by dry plants works correctly */
  @Test
  fun filteringByDryPlantsWorks() = runTest {
    // Create plants with different health statuses
    val healthyPlant =
        Plant(
            "Healthy",
            null,
            "H",
            "Healthy",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "Good",
            2)
    val dryPlant =
        Plant(
            "Dry",
            null,
            "D",
            "Needs water",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.NEEDS_WATER,
            "Thirsty",
            2)
    val severelyDryPlant =
        Plant(
            "VeryDry",
            null,
            "VD",
            "Very dry",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.SEVERELY_DRY,
            "Very thirsty",
            2)
    val overwateredPlant =
        Plant(
            "Wet",
            null,
            "W",
            "Too much",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.OVERWATERED,
            "Wet",
            7)

    // Add plants
    plantsRepo.saveToGarden(
        healthyPlant, plantsRepo.getNewId(), Timestamp(System.currentTimeMillis()))
    plantsRepo.saveToGarden(
        dryPlant,
        plantsRepo.getNewId(),
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))
    plantsRepo.saveToGarden(
        severelyDryPlant,
        plantsRepo.getNewId(),
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)))
    plantsRepo.saveToGarden(
        overwateredPlant,
        plantsRepo.getNewId(),
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))
    runCurrent()

    vm.refreshUIState()
    runCurrent()

    // Apply dry plants filter
    vm.setFilterOption(FilterOption.DRY_PLANTS)
    runCurrent()

    // Verify only dry plants are shown
    val filteredPlants = vm.uiState.value.filteredAndSortedPlants
    assertEquals(2, filteredPlants.size)
    assertTrue(filteredPlants.any { it.plant.name == "Dry" })
    assertTrue(filteredPlants.any { it.plant.name == "VeryDry" })
    repositoryScope.cancel()
  }

  /** Tests that filtering by healthy plants works correctly */
  @Test
  fun filteringByHealthyPlantsWorks() = runTest {
    // Create plants with different health statuses
    val healthyPlant1 =
        Plant(
            "Healthy1",
            null,
            "H1",
            "Healthy",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "Good",
            5)
    val healthyPlant2 =
        Plant(
            "Healthy2",
            null,
            "H2",
            "Healthy",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "Good",
            5)
    val dryPlant =
        Plant(
            "Dry",
            null,
            "D",
            "Needs water",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.NEEDS_WATER,
            "Thirsty",
            2)

    // Add plants
    plantsRepo.saveToGarden(
        healthyPlant1, plantsRepo.getNewId(), Timestamp(System.currentTimeMillis()))
    plantsRepo.saveToGarden(
        healthyPlant2, plantsRepo.getNewId(), Timestamp(System.currentTimeMillis()))
    plantsRepo.saveToGarden(
        dryPlant,
        plantsRepo.getNewId(),
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))
    runCurrent()

    vm.refreshUIState()
    runCurrent()

    // Apply healthy plants filter
    vm.setFilterOption(FilterOption.HEALTHY_ONLY)
    runCurrent()

    // Verify only healthy plants are shown
    val filteredPlants = vm.uiState.value.filteredAndSortedPlants
    assertEquals(2, filteredPlants.size)
    assertTrue(filteredPlants.all { it.plant.healthStatus == PlantHealthStatus.HEALTHY })
    repositoryScope.cancel()
  }

  @Test
  fun sortByLatinNameWorks() = runTest {
    // Sort alphabetically by latin name
    plantsRepo.saveToGarden(
        Plant(
            "A",
            null,
            "Zebra",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            2),
        "1",
        Timestamp(0))
    plantsRepo.saveToGarden(
        Plant(
            "B",
            null,
            "Alpha",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            2),
        "2",
        Timestamp(0))
    runCurrent()
    vm.refreshUIState()
    vm.setSortOption(SortOption.LATIN_NAME)
    runCurrent()

    val sorted = vm.uiState.value.filteredAndSortedPlants
    assertEquals("Alpha", sorted[0].plant.latinName)
    assertEquals("Zebra", sorted[1].plant.latinName)
    repositoryScope.cancel()
  }

  @Test
  fun sortByLastWateredAscWorks() = runTest {
    // Oldest watered first
    plantsRepo.saveToGarden(
        Plant(
            "New",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            2),
        "1",
        Timestamp(100))
    plantsRepo.saveToGarden(
        Plant(
            "Old",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            2),
        "2",
        Timestamp(50))
    runCurrent()
    vm.refreshUIState()
    vm.setSortOption(SortOption.LAST_WATERED_ASC)
    runCurrent()

    val sorted = vm.uiState.value.filteredAndSortedPlants
    assertEquals("Old", sorted[0].plant.name)
    assertEquals("New", sorted[1].plant.name)
    repositoryScope.cancel()
  }

  @Test
  fun sortByLastWateredDescWorks() = runTest {
    // Most recent watered first
    plantsRepo.saveToGarden(
        Plant(
            "New",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            2),
        "1",
        Timestamp(100))
    plantsRepo.saveToGarden(
        Plant(
            "Old",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            2),
        "2",
        Timestamp(50))
    runCurrent()
    vm.refreshUIState()
    vm.setSortOption(SortOption.LAST_WATERED_DESC)
    runCurrent()

    val sorted = vm.uiState.value.filteredAndSortedPlants
    assertEquals("New", sorted[0].plant.name)
    assertEquals("Old", sorted[1].plant.name)
    repositoryScope.cancel()
  }

  @Test
  fun filterOverwateredWorks() = runTest {
    // Show only overwatered plants - use 7 day frequency for more reliable timing
    plantsRepo.saveToGarden(
        Plant(
            "Wet",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.OVERWATERED,
            "",
            7),
        "1",
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))
    plantsRepo.saveToGarden(
        Plant(
            "VeryWet",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.SEVERELY_OVERWATERED,
            "",
            15),
        "2",
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))

    plantsRepo.saveToGarden(
        Plant(
            "Ok",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            2),
        "3",
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))

    plantsRepo.waterPlant("1", Timestamp(System.currentTimeMillis()))
    plantsRepo.waterPlant("2", Timestamp(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(12)))

    runCurrent()
    vm.refreshUIState()
    runCurrent()

    vm.setFilterOption(FilterOption.OVERWATERED_ONLY)
    runCurrent()

    assertEquals(2, vm.uiState.value.filteredAndSortedPlants.size)
    repositoryScope.cancel()
  }

  @Test
  fun filterCriticalWorks() = runTest {
    // Show only severely dry
    plantsRepo.saveToGarden(
        Plant(
            "Critical1",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.SEVERELY_DRY,
            "",
            2),
        "1",
        Timestamp(0))
    plantsRepo.saveToGarden(
        Plant(
            "Ok",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            2),
        "3",
        Timestamp(System.currentTimeMillis()))
    runCurrent()
    vm.refreshUIState()
    vm.setFilterOption(FilterOption.CRITICAL_ONLY)
    runCurrent()

    assertEquals(1, vm.uiState.value.filteredAndSortedPlants.size)
    repositoryScope.cancel()
  }

  @Test
  fun sortAndFilterCombinedWorks() = runTest {
    // Filter dry plants AND sort by name
    plantsRepo.saveToGarden(
        Plant(
            "Z-Dry",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.SLIGHTLY_DRY,
            "",
            7),
        "1",
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(5)))
    plantsRepo.saveToGarden(
        Plant(
            "A-Dry",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.SEVERELY_DRY,
            "",
            2),
        "2",
        Timestamp(0))
    plantsRepo.saveToGarden(
        Plant(
            "Healthy",
            null,
            "",
            "",
            PlantLocation.INDOOR,
            "Test light exposure",
            PlantHealthStatus.HEALTHY,
            "",
            7),
        "3",
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)))
    runCurrent()
    vm.refreshUIState()
    runCurrent()
    vm.setFilterOption(FilterOption.DRY_PLANTS)
    vm.setSortOption(SortOption.PLANT_NAME)
    runCurrent()

    val result = vm.uiState.value.filteredAndSortedPlants
    assertEquals(2, result.size)
    assertEquals("A-Dry", result[0].plant.name)
    assertEquals("Z-Dry", result[1].plant.name)
    repositoryScope.cancel()
  }
}
