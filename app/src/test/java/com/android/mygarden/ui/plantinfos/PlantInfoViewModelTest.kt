package com.android.mygarden.ui.plantinfos

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.mygarden.R
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
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var context: Context

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = PlantsRepositoryLocal()
    viewModel = PlantInfoViewModel(repository)
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
}
