package com.android.mygarden.ui.plantinfos

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.mygarden.R
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.utils.FakeActivityRepository
import com.android.mygarden.utils.FakeProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for PlantInfoViewModel.
 *
 * These tests verify the business logic of the ViewModel without UI dependencies.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PlantInfoViewModelTest {

  private lateinit var viewModel: PlantInfoViewModel
  private lateinit var repository: PlantsRepositoryLocal
  private lateinit var activityRepo: FakeActivityRepository
  private lateinit var profileRepo: FakeProfileRepository
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = PlantsRepositoryLocal()
    activityRepo = FakeActivityRepository()
    profileRepo = FakeProfileRepository()
    viewModel = PlantInfoViewModel(repository, activityRepo, profileRepo)
    context = ApplicationProvider.getApplicationContext()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // Helper function to create a test plant
  private fun createTestPlant(
      name: String = "Test Plant",
      latinName: String = "Testus Plantus",
      description: String = "A test plant description",
      healthStatus: PlantHealthStatus = PlantHealthStatus.HEALTHY,
      healthStatusDescription: String = "This plant is healthy",
      wateringFrequency: Int = 7
  ): Plant {
    return Plant(
        name = name,
        image = null,
        latinName = latinName,
        description = description,
        healthStatus = healthStatus,
        healthStatusDescription = healthStatusDescription,
        wateringFrequency = wateringFrequency)
  }

  @Test
  fun uiState_initialStateIsDefault() = runTest {
    // UI state should start with empty/default values before initialization
    val initialState = viewModel.uiState.value

    assertEquals("", initialState.name)
    assertEquals(null, initialState.image)
    assertEquals("", initialState.latinName)
    assertEquals("", initialState.description)
    assertEquals(PlantHealthStatus.UNKNOWN, initialState.healthStatus)
    assertEquals("", initialState.healthStatusDescription)
    assertEquals(0, initialState.wateringFrequency)
    assertEquals(SelectedPlantInfoTab.DESCRIPTION, initialState.selectedTab)
  }

  @Test
  fun initializeUIState_mapsPlantToUIStateCorrectly() = runTest {
    // ViewModel should correctly map Plant model to UI state
    val plant =
        createTestPlant(
            name = "Rose",
            latinName = "Rosa rubiginosa",
            description = "A beautiful flowering plant",
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "The plant is thriving",
            wateringFrequency = 3)

    viewModel.initializeUIState(plant, context.getString(R.string.loading_plant_infos))
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("Rose", uiState.name)
    assertEquals("Rosa rubiginosa", uiState.latinName)
    assertEquals("A beautiful flowering plant", uiState.description)
    assertEquals(PlantHealthStatus.HEALTHY, uiState.healthStatus)
    assertEquals("The plant is thriving", uiState.healthStatusDescription)
    assertEquals(3, uiState.wateringFrequency)
  }

  @Test
  fun initializeUIState_preservesImageReference() = runTest {
    // Image reference should be preserved during initialization
    val plant = createTestPlant()

    viewModel.initializeUIState(plant, context.getString(R.string.loading_plant_infos))
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(plant.image, uiState.image)
  }

  @Test
  fun initializeUIState_handlesAllHealthStatuses() = runTest {
    // Test all health statuses to ensure none are missed
    // Test all health statuses dynamically - automatically includes any new statuses added
    PlantHealthStatus.values().forEach { status ->
      val plant =
          createTestPlant(healthStatus = status, healthStatusDescription = status.description)
      viewModel.initializeUIState(plant, context.getString(R.string.loading_plant_infos))
      advanceUntilIdle()

      val uiState = viewModel.uiState.value
      assertEquals(status, uiState.healthStatus)
      assertEquals(status.description, uiState.healthStatusDescription)
    }
  }

  /**
   * Check that the right error is set when initializeUIState() fails to getOwnedPlant from the
   * repository and the error is cleared when clearErrorMsg() is called.
   */
  @Test
  fun initializeUIState_setErrorMessageWhenWrongPlantIdThenClearError() = runTest {
    val plant = createTestPlant()
    val id1 = "id of a stored Plant. But there is no stored plant."

    // Initialize the UIState with an OwnedPlantId but there is not plant in the repository so it
    // will throw an exception and should call setErrorMsg
    viewModel.initializeUIState(plant, context.getString(R.string.loading_plant_infos), id1)
    advanceUntilIdle()
    val errorMsg = viewModel.uiState.value.errorMsg
    // Check that the right error message has been set.
    assertEquals(R.string.error_failed_load_plant_info, errorMsg)

    // Clear the error message
    viewModel.clearErrorMsg()
    val errorMsgAfterClear = viewModel.uiState.value.errorMsg
    // Check that the error message has been cleaned
    assertNull(errorMsgAfterClear)
  }

  @Test
  fun setTab_updatesSelectedTabToDescription() = runTest {
    // Tab switching should correctly update the selected tab
    // Initialize with a plant first
    viewModel.initializeUIState(createTestPlant(), context.getString(R.string.loading_plant_infos))
    advanceUntilIdle()

    // Switch to health tab first
    viewModel.setTab(SelectedPlantInfoTab.HEALTH_STATUS)
    advanceUntilIdle()

    // Switch back to description
    viewModel.setTab(SelectedPlantInfoTab.DESCRIPTION)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(SelectedPlantInfoTab.DESCRIPTION, uiState.selectedTab)
  }

  @Test
  fun setTab_updatesSelectedTabToHealth() = runTest {
    // Verify switching to health tab works correctly
    // Initialize with a plant first
    viewModel.initializeUIState(createTestPlant(), context.getString(R.string.loading_plant_infos))
    advanceUntilIdle()

    viewModel.setTab(SelectedPlantInfoTab.HEALTH_STATUS)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(SelectedPlantInfoTab.HEALTH_STATUS, uiState.selectedTab)
  }

  @Test
  fun setTab_doesNotAffectOtherUIStateProperties() = runTest {
    // Tab switching should only change the tab, not other plant data
    val plant = createTestPlant(name = "Cactus", latinName = "Cactaceae")
    viewModel.initializeUIState(plant, context.getString(R.string.loading_plant_infos))
    advanceUntilIdle()

    viewModel.setTab(SelectedPlantInfoTab.HEALTH_STATUS)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    // Verify other properties remain unchanged
    assertEquals("Cactus", uiState.name)
    assertEquals("Cactaceae", uiState.latinName)
    assertEquals(plant.description, uiState.description)
    assertEquals(plant.healthStatus, uiState.healthStatus)
  }

  @Test
  fun savePlant_callsRepositorySaveToGarden() = runTest {
    // Saving should delegate to repository and return a valid plant ID
    val plant = createTestPlant(name = "Orchid")
    var savedPlantId: String? = null

    viewModel.savePlant(plant, onPlantSaved = { plantId -> savedPlantId = plantId })
    advanceUntilIdle()

    // Verify a valid ID was returned (format depends on repository implementation)
    assertNotNull(savedPlantId)
    assertFalse(savedPlantId!!.isEmpty())
  }

  @Test
  fun savePlant_canSaveMultiplePlants() = runTest {
    // Multiple saves should each return a unique valid ID
    val plant1 = createTestPlant(name = "Plant 1")
    val plant2 = createTestPlant(name = "Plant 2")
    val plant3 = createTestPlant(name = "Plant 3")

    val savedIds = mutableListOf<String>()

    viewModel.savePlant(plant1, onPlantSaved = { savedIds.add(it) })
    advanceUntilIdle()
    viewModel.savePlant(plant2, onPlantSaved = { savedIds.add(it) })
    advanceUntilIdle()
    viewModel.savePlant(plant3, onPlantSaved = { savedIds.add(it) })
    advanceUntilIdle()

    // Verify all plants were saved with valid unique IDs
    assertEquals(3, savedIds.size)
    savedIds.forEach { id ->
      assertNotNull(id)
      assertFalse(id.isEmpty())
    }
    // Verify all IDs are unique
    assertEquals(savedIds.size, savedIds.toSet().size)
  }

  @Test
  fun savePlant_createsActivityAddedPlant() = runTest {
    // Arrange: create a test plant
    val plant = createTestPlant(name = "Orchid", latinName = "Orchidaceae")
    var savedPlantId: String? = null

    // Ensure no activities exist before saving
    assertEquals(0, activityRepo.addedActivities.size)

    // Act: save the plant
    viewModel.savePlant(plant, onPlantSaved = { plantId -> savedPlantId = plantId })
    advanceUntilIdle()

    // Assert: verify activity was created
    assertEquals(1, activityRepo.addedActivities.size)
    val activity = activityRepo.addedActivities[0]
    assertTrue(activity is ActivityAddedPlant)

    val addedPlantActivity = activity as ActivityAddedPlant
    assertEquals("fake-uid", addedPlantActivity.userId)
    assertEquals("pseudo", addedPlantActivity.pseudo)
    assertEquals(savedPlantId, addedPlantActivity.ownedPlant.id)
    assertEquals("Orchid", addedPlantActivity.ownedPlant.plant.name)
    assertEquals("Orchidaceae", addedPlantActivity.ownedPlant.plant.latinName)
    assertNotNull(addedPlantActivity.createdAt)
  }

  @Test
  fun savePlant_doesNotCreateActivity_whenProfileIsNull() = runTest {
    // Arrange: create a profile repository that returns null
    val nullProfileRepo = FakeProfileRepository()
    val vm = PlantInfoViewModel(repository, activityRepo, nullProfileRepo)
    val plant = createTestPlant(name = "Test Plant")

    // Act: save the plant
    vm.savePlant(plant, onPlantSaved = {})
    advanceUntilIdle()

    // Assert: no activity should be created
    assertEquals(0, activityRepo.addedActivities.size)
  }

  @Test
  fun savePlant_createsMultipleActivities_forMultiplePlants() = runTest {
    // Arrange: create multiple test plants
    val plant1 = createTestPlant(name = "Rose")
    val plant2 = createTestPlant(name = "Tulip")

    // Act: save both plants
    viewModel.savePlant(plant1, onPlantSaved = {})
    advanceUntilIdle()
    viewModel.savePlant(plant2, onPlantSaved = {})
    advanceUntilIdle()

    // Assert: verify two activities were created
    assertEquals(2, activityRepo.addedActivities.size)

    val activity1 = activityRepo.addedActivities[0] as ActivityAddedPlant
    val activity2 = activityRepo.addedActivities[1] as ActivityAddedPlant

    assertEquals("Rose", activity1.ownedPlant.plant.name)
    assertEquals("Tulip", activity2.ownedPlant.plant.name)
  }

  @Test
  fun initializeUIState_canBeCalledMultipleTimes() = runTest {
    // Re-initializing should replace the previous plant data
    val plant1 = createTestPlant(name = "First Plant")
    val plant2 = createTestPlant(name = "Second Plant")

    viewModel.initializeUIState(plant1, context.getString(R.string.loading_plant_infos))
    advanceUntilIdle()
    var uiState = viewModel.uiState.value
    assertEquals("First Plant", uiState.name)

    viewModel.initializeUIState(plant2, context.getString(R.string.loading_plant_infos))
    advanceUntilIdle()
    uiState = viewModel.uiState.value
    assertEquals("Second Plant", uiState.name)
  }

  @Test
  fun showCareTips_setsDialogAndLoadsTips() = runTest {
    // Arrange: create a fake repository that returns a predictable tips string
    val fakeTips = "Water lightly every 3 days"
    val plantRepo =
        object : com.android.mygarden.model.plant.PlantsRepository by PlantsRepositoryLocal() {
          override suspend fun generateCareTips(
              latinName: String,
              healthStatus: PlantHealthStatus
          ): String {
            return fakeTips
          }
        }

    val vm = PlantInfoViewModel(plantRepo, activityRepo, profileRepo)

    // Act: call showCareTips and advance coroutines to completion
    vm.showCareTips("Testus plantus", PlantHealthStatus.HEALTHY)
    advanceUntilIdle()

    // Assert: dialog visible and tips set
    val finalState = vm.uiState.value
    assertTrue(finalState.showCareTipsDialog)
    assertEquals(fakeTips, finalState.careTips)
  }

  @Test
  fun dismissCareTips_hidesDialogButPreservesText() = runTest {
    val fakeTips = "Keep soil moist"
    val plantRepo =
        object : com.android.mygarden.model.plant.PlantsRepository by PlantsRepositoryLocal() {
          override suspend fun generateCareTips(
              latinName: String,
              healthStatus: PlantHealthStatus
          ): String {
            return fakeTips
          }
        }

    val vm = PlantInfoViewModel(plantRepo, activityRepo, profileRepo)
    vm.showCareTips("Testus plantus", PlantHealthStatus.HEALTHY)
    advanceUntilIdle()

    // ensure dialog visible and tips present
    assertTrue(vm.uiState.value.showCareTipsDialog)
    assertEquals(fakeTips, vm.uiState.value.careTips)

    // dismiss and verify dialog hidden but tips preserved
    vm.dismissCareTips()
    assertFalse(vm.uiState.value.showCareTipsDialog)
    assertEquals(fakeTips, vm.uiState.value.careTips)
  }

  @Test
  fun showCareTips_handlesRepositoryException_andShowsFallback() = runTest {
    // Arrange: repo that throws when generateCareTips is called
    val throwingRepo =
        object : com.android.mygarden.model.plant.PlantsRepository by PlantsRepositoryLocal() {
          override suspend fun generateCareTips(
              latinName: String,
              healthStatus: PlantHealthStatus
          ): String {
            throw RuntimeException("AI backend down")
          }
        }
    val vm = PlantInfoViewModel(throwingRepo, activityRepo, profileRepo)

    // Act: call showCareTips and advance the coroutine
    vm.showCareTips("Testus plantus", PlantHealthStatus.UNKNOWN)
    advanceUntilIdle()

    // Assert: fallback message from ViewModel should be set
    val finalState = vm.uiState.value
    assertTrue(finalState.showCareTipsDialog)
    assertEquals(PlantInfoViewModel.ERROR_GENERATING_TIPS, finalState.careTips)
  }

  @Test
  fun showCareTips_showsUnknownPlaceholder() = runTest {
    val plantRepo = PlantsRepositoryLocal()
    val vm = PlantInfoViewModel(plantRepo, activityRepo, profileRepo)

    // Case: latin name equals "unknown"
    vm.showCareTips("unknown", PlantHealthStatus.HEALTHY)
    advanceUntilIdle()
    var finalState = vm.uiState.value
    assertTrue(finalState.showCareTipsDialog)
    assertEquals(PlantInfoViewModel.UNKNOWN_PLANT_TIPS_PLACEHOLDER, finalState.careTips)
  }
}
