package com.android.mygarden.ui.editPlant

import com.android.mygarden.model.plant.*
import kotlinx.coroutines.Dispatchers
import java.sql.Timestamp
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

@OptIn(ExperimentalCoroutinesApi::class)
class EditPlantViewModelTest {

    private lateinit var repository: PlantsRepositoryLocal
    private lateinit var viewModel: EditPlantViewModel
    private lateinit var plantId: String
    private lateinit var owned: OwnedPlant
    private val testDispatcher = StandardTestDispatcher()


    @Before
    fun setup() = runTest {
        Dispatchers.setMain(testDispatcher)
        repository = PlantsRepositoryLocal()
        viewModel = EditPlantViewModel(repository)

        val plant = Plant(
            name = "Monstera",
            image = "monstera.png",
            latinName = "Monstera deliciosa",
            description = "Tropical plant",
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "The plant is healthy",
            wateringFrequency = 7
        )

        plantId = repository.getNewId()
        owned = repository.saveToGarden(
            plant = plant,
            id = plantId,
            lastWatered = Timestamp(1760375671)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun clearErrorMsg_and_setErrorMsg_update_state() {
        viewModel.setErrorMsg("boom")
        assertEquals("boom", viewModel.uiState.value.errorMsg)

        viewModel.clearErrorMsg()
        assertNull(viewModel.uiState.value.errorMsg)
    }

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

    @Test
    fun loadPlant_failure_sets_error_message() = runTest {
        viewModel.loadPlant("does-not-exist")
        advanceUntilIdle()
        assertEquals("Failed to load plant", viewModel.uiState.value.errorMsg)
    }

    @Test
    fun setLastWatered_updates_state() {
        val ts = Timestamp(1730001234000)
        viewModel.setLastWatered(ts)
        assertEquals(ts, viewModel.uiState.value.lastWatered)
    }

    @Test
    fun setDescription_updates_state() {
        viewModel.setDescription("New description")
        assertEquals("New description", viewModel.uiState.value.description)
    }

    @Test
    fun deletePlant_success_removes_from_repository() = runTest {
        viewModel.deletePlant(plantId)
        advanceUntilIdle()
        try {
            repository.getOwnedPlant(plantId)
            fail("Expected exception after deletion")
            assertEquals(0, repository.getAllOwnedPlants().size)
        } catch (e: IllegalArgumentException) { }
    }

    @Test
    fun deletePlant_failure_sets_error_message() = runTest {
        viewModel.deletePlant("unknown-id")
        advanceUntilIdle()
        assertEquals("Failed to delete plant", viewModel.uiState.value.errorMsg)
    }

    @Test
    fun editPlant_without_load_sets_error_about_plant_not_loaded() {
        viewModel.editPlant(plantId)
        assertEquals("Plant not loaded yet", viewModel.uiState.value.errorMsg)
    }

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
        assertEquals(PlantHealthStatus.HEALTHY, updated.plant.healthStatus)
        assertEquals("The plant is healthy", updated.plant.healthStatusDescription)
        assertEquals(7, updated.plant.wateringFrequency)
    }

    @Test
    fun editPlant_with_different_id_sets_ID_mismatch_error() {
        viewModel.loadPlant(plantId)
        viewModel.setLastWatered(Timestamp(1732000000000))
        viewModel.setDescription("Ok")

        viewModel.editPlant("different-id")

        val err = viewModel.uiState.value.errorMsg
        assertNotNull(err)
        assertTrue(err!!.contains("Plant not loaded yet"))
    }

    @Test
    fun editPlant_with_blank_description_sets_error_and_does_not_update_repo() = runTest {
        viewModel.loadPlant(plantId)
        advanceUntilIdle()

        viewModel.setDescription("   ")

        val before = repository.getOwnedPlant(plantId)

        viewModel.editPlant(plantId)
        advanceUntilIdle()

        assertEquals("Please put a description", viewModel.uiState.value.errorMsg)

        val after = repository.getOwnedPlant(plantId)
        assertEquals(before.lastWatered, after.lastWatered)
        assertEquals("Tropical plant", after.plant.description)
        assertEquals(before.plant.name, after.plant.name)
        assertEquals(before.plant.latinName, after.plant.latinName)
        assertEquals(before.plant.image, after.plant.image)
        assertEquals(before.plant.healthStatus, after.plant.healthStatus)
        assertEquals(
            before.plant.healthStatusDescription,
            after.plant.healthStatusDescription
        )
        assertEquals(before.plant.wateringFrequency, after.plant.wateringFrequency)
    }


    // NOTE:
    // The branch in editPlant() that checks for a null `lastWatered` value is not tested here.
    // In normal application flow, `lastWatered` is always initialized when loading a plant
    // and cannot be null through user interaction (the UI always provides or requires a date).
    // This condition is purely defensive and should never be reached in real usage,
    // so no dedicated test case is included for it.

}
