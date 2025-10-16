package com.android.mygarden.ui.plantinfos

import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryLocal
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

/**
 * Unit tests for PlantInfoViewModel.
 *
 * These tests verify the business logic of the ViewModel without UI dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlantInfoViewModelTest_ {

  private lateinit var viewModel: PlantInfoViewModel
  private lateinit var repository: PlantsRepositoryLocal
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = PlantsRepositoryLocal()
    viewModel = PlantInfoViewModel(repository)
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
    val plant =
        createTestPlant(
            name = "Rose",
            latinName = "Rosa rubiginosa",
            description = "A beautiful flowering plant",
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "The plant is thriving",
            wateringFrequency = 3)

    viewModel.initializeUIState(plant)
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
    val plant = createTestPlant()

    viewModel.initializeUIState(plant)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(plant.image, uiState.image)
  }

  @Test
  fun initializeUIState_handlesAllHealthStatuses() = runTest {
    // Test all health statuses dynamically - automatically includes any new statuses added
    PlantHealthStatus.values().forEach { status ->
      val plant =
          createTestPlant(healthStatus = status, healthStatusDescription = status.description)
      viewModel.initializeUIState(plant)
      advanceUntilIdle()

      val uiState = viewModel.uiState.value
      assertEquals(status, uiState.healthStatus)
      assertEquals(status.description, uiState.healthStatusDescription)
    }
  }

  @Test
  fun setTab_updatesSelectedTabToDescription() = runTest {
    // Initialize with a plant first
    viewModel.initializeUIState(createTestPlant())
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
    // Initialize with a plant first
    viewModel.initializeUIState(createTestPlant())
    advanceUntilIdle()

    viewModel.setTab(SelectedPlantInfoTab.HEALTH_STATUS)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(SelectedPlantInfoTab.HEALTH_STATUS, uiState.selectedTab)
  }

  @Test
  fun setTab_doesNotAffectOtherUIStateProperties() = runTest {
    val plant = createTestPlant(name = "Cactus", latinName = "Cactaceae")
    viewModel.initializeUIState(plant)
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
    val plant = createTestPlant(name = "Orchid")

    viewModel.savePlant(plant)
    advanceUntilIdle()

    // Verify the plant was saved by checking the repository's generated ID incremented
    val nextId = repository.getNewId()
    assertEquals("1", nextId) // Should be 1 since we used 0 for the save
  }

  @Test
  fun savePlant_canSaveMultiplePlants() = runTest {
    val plant1 = createTestPlant(name = "Plant 1")
    val plant2 = createTestPlant(name = "Plant 2")
    val plant3 = createTestPlant(name = "Plant 3")

    viewModel.savePlant(plant1)
    advanceUntilIdle()
    viewModel.savePlant(plant2)
    advanceUntilIdle()
    viewModel.savePlant(plant3)
    advanceUntilIdle()

    // Verify the counter incremented 3 times by checking next ID is "3"
    assertEquals("3", repository.getNewId())
  }

  @Test
  fun initializeUIState_canBeCalledMultipleTimes() = runTest {
    val plant1 = createTestPlant(name = "First Plant")
    val plant2 = createTestPlant(name = "Second Plant")

    viewModel.initializeUIState(plant1)
    advanceUntilIdle()
    var uiState = viewModel.uiState.value
    assertEquals("First Plant", uiState.name)

    viewModel.initializeUIState(plant2)
    advanceUntilIdle()
    uiState = viewModel.uiState.value
    assertEquals("Second Plant", uiState.name)
  }
}
