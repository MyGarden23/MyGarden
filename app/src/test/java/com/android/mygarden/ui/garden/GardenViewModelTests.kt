package com.android.mygarden.ui.garden

import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository_
import com.android.mygarden.model.plant.PlantsRepositoryLocal
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

@OptIn(ExperimentalCoroutinesApi::class)
class GardenViewModelTests {

  private lateinit var repo: PlantsRepository_
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

  /** Sets up the repository and the view model and the test dispatcher to simulate the app */
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repo = PlantsRepositoryLocal()
    vm = GardenViewModel(plantsRepo = repo)
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

  /**
   * Tests that the getAllPlants function retrieves correctly the list of owned plants and updated
   * the vm
   */
  @Test
  fun getAllPlantsWorksCorrectly() = runTest {
    assertEquals(emptyList<OwnedPlant>(), vm.uiState.value.plants)
    vm.getAllPlants()
    advanceUntilIdle()
    assertEquals(emptyList<OwnedPlant>(), vm.uiState.value.plants)
    ownedPlant = repo.saveToGarden(plant1, repo.getNewId(), Timestamp(1))
    advanceUntilIdle()
    vm.getAllPlants()
    advanceUntilIdle()
    assertEquals(listOf(ownedPlant), vm.uiState.value.plants)
  }
}
