package com.android.mygarden.ui.editPlant

import com.android.mygarden.R
import com.android.mygarden.model.plant.*
import java.sql.Timestamp
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

/** Tests for [EditPlantViewModel]. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditPlantViewModelTest {

  private lateinit var repository: PlantsRepositoryLocal
  private lateinit var viewModel: EditPlantViewModel
  private lateinit var plantId: String
  private lateinit var owned: OwnedPlant
  private val testDispatcher = StandardTestDispatcher()

  /** Set up the test environment. */
  @Before
  fun setup() = runTest {
    Dispatchers.setMain(testDispatcher)
    repository = PlantsRepositoryLocal()
    viewModel = EditPlantViewModel(repository)

    val plant =
        Plant(
            name = "Monstera",
            image = "monstera.png",
            latinName = "Monstera deliciosa",
            description = "Tropical plant",
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "The plant is healthy",
            wateringFrequency = 7)

    plantId = repository.getNewId()
    owned =
        repository.saveToGarden(plant = plant, id = plantId, lastWatered = Timestamp(1760375671))
  }

  /** Tear down the test environment. */
  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /** Test clearing and setting error messages. */
  @Test
  fun clearErrorMsg_and_setErrorMsg_update_state() {
    viewModel.setErrorMsg(R.string.error_failed_load_plant_edit)
    assertEquals(R.string.error_failed_load_plant_edit, viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)
  }

  /** Test loading a plant from the repository. */
  @Test
  fun loadPlant_success() = runTest {
    viewModel.loadPlant(plantId)
    advanceUntilIdle()

    val ui = viewModel.uiState.value
    assertEquals("Monstera", ui.name)
    assertEquals("Monstera deliciosa", ui.latinName)
    assertEquals("Tropical plant", ui.description)
    assertEquals("monstera.png", ui.image)
    assertEquals(owned.lastWatered, ui.lastWatered)
    assertNull(ui.errorMsg)
  }

  /** Test loading a plant that does not exist in the repository. */
  @Test
  fun loadPlant_failure_sets_error_message() = runTest {
    viewModel.loadPlant("does-not-exist")
    advanceUntilIdle()
    assertEquals(R.string.error_failed_load_plant_edit, viewModel.uiState.value.errorMsg)
  }

  /** Test setting the last watered date. */
  @Test
  fun setLastWatered_updates_state() {
    val ts = Timestamp(1730001234000)
    viewModel.setLastWatered(ts)
    assertEquals(ts, viewModel.uiState.value.lastWatered)
  }

  /** Test setting the description. */
  @Test
  fun setDescription_updates_state() {
    viewModel.setDescription("New description")
    assertEquals("New description", viewModel.uiState.value.description)
  }

  /** Test setting the location with arbitrary value. */
  @Test
  fun setLocation_updates_state() {
    for (loc in PlantLocation.entries) {
      viewModel.setLocation(loc)
      assertEquals(loc, viewModel.uiState.value.location)
    }
  }

  /** Test setting the light exposure. */
  @Test
  fun setLightExposure_updates_state() {
    viewModel.setLightExposure("New light exposure")
    assertEquals("New light exposure", viewModel.uiState.value.lightExposure)
  }

  /** Test deleting a plant from the repository. */
  @Test
  fun deletePlant_success_removes_from_repository() = runTest {
    viewModel.deletePlant(plantId)
    advanceUntilIdle()
    try {
      repository.getOwnedPlant(plantId)
      fail("Expected exception after deletion")
      assertEquals(0, repository.getAllOwnedPlants().size)
    } catch (e: IllegalArgumentException) {}
  }

  /** Test deleting a plant that does not exist in the repository. */
  @Test
  fun deletePlant_failure_sets_error_message() = runTest {
    viewModel.deletePlant("unknown-id")
    advanceUntilIdle()
    assertEquals(R.string.error_failed_delete_plant_edit, viewModel.uiState.value.errorMsg)
  }

  /** Test editing a plant in the repository without loading it first. */
  @Test
  fun editPlant_without_load_sets_error_about_plant_not_loaded() {
    viewModel.editPlant(plantId)
    assertEquals(R.string.error_plant_not_loaded, viewModel.uiState.value.errorMsg)
  }

  /** Test editing a plant in the repository. */
  @Test
  fun editPlant_success_updates_repository_with_new_description_and_lastWatered() = runTest {
    viewModel.loadPlant(plantId)
    advanceUntilIdle()

    val newTs = Timestamp(1731000000000)
    viewModel.setLastWatered(newTs)
    viewModel.setDescription("Updated desc")

    viewModel.editPlant(plantId)
    advanceUntilIdle()

    val updated = repository.getOwnedPlant(plantId)
    assertEquals(newTs, updated.lastWatered)
    assertEquals("Updated desc", updated.plant.description)
    assertEquals("Monstera", updated.plant.name)
    assertEquals("Monstera deliciosa", updated.plant.latinName)
    assertEquals("monstera.png", updated.plant.image)
    // assertEquals(PlantHealthStatus.HEALTHY, updated.plant.healthStatus)
    assertEquals("The plant is healthy", updated.plant.healthStatusDescription)
    assertEquals(7, updated.plant.wateringFrequency)
  }

  /** Test editing a plant in the repository with a different ID than the one loaded. */
  @Test
  fun editPlant_with_different_id_sets_ID_mismatch_error() {
    viewModel.loadPlant(plantId)
    viewModel.setLastWatered(Timestamp(1732000000000))
    viewModel.setDescription("Ok")

    viewModel.editPlant("different-id")

    val err = viewModel.uiState.value.errorMsg
    assertNotNull(err)
    assertEquals(err, R.string.error_plant_not_loaded)
  }

  /** Test editing a plant in the repository with a blank description. */
  @Test
  fun editPlant_with_blank_description_sets_error_and_does_not_update_repo() = runTest {
    viewModel.loadPlant(plantId)
    advanceUntilIdle()

    viewModel.setDescription("   ")

    val before = repository.getOwnedPlant(plantId)

    viewModel.editPlant(plantId)
    advanceUntilIdle()

    assertEquals(R.string.error_description_blank, viewModel.uiState.value.errorMsg)

    val after = repository.getOwnedPlant(plantId)
    assertEquals(before.lastWatered, after.lastWatered)
    assertEquals("Tropical plant", after.plant.description)
    assertEquals(before.plant.name, after.plant.name)
    assertEquals(before.plant.latinName, after.plant.latinName)
    assertEquals(before.plant.image, after.plant.image)
    assertEquals(before.plant.healthStatus, after.plant.healthStatus)
    assertEquals(before.plant.healthStatusDescription, after.plant.healthStatusDescription)
    assertEquals(before.plant.wateringFrequency, after.plant.wateringFrequency)
  }

  // NOTE:
  // The branch in editPlant() that checks for a null `lastWatered` value is not tested here.
  // In normal application flow, `lastWatered` is always initialized when loading a plant
  // and cannot be null through user interaction (the UI always provides or requires a date).
  // This condition is purely defensive and should never be reached in real usage,
  // so no dedicated test case is included for it.

}
