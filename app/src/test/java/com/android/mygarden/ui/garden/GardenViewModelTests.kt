package com.android.mygarden.ui.garden

import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
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

  private lateinit var repo: PlantsRepository
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

  @Before
  fun setUp() = runTest {
    Dispatchers.setMain(testDispatcher)
    repo = PlantsRepositoryLocal()
    vm = GardenViewModel(plantsRepo = repo)
  }

  @After
  fun end() {
    Dispatchers.resetMain()
  }

  @Test
  fun setErrorMsgUpdatesCorrectly() {
    val errorMsg = "Error"
    vm.setErrorMsg(errorMsg)
    assertEquals(errorMsg, vm.uiState.value.errorMsg)
  }

  @Test
  fun clearErrorMsgClearsCorrectly() {
    val errorMsg = "Error"
    vm.setErrorMsg(errorMsg)
    assertEquals(errorMsg, vm.uiState.value.errorMsg)
    vm.clearErrorMsg()
    assertNull(vm.uiState.value.errorMsg)
  }

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
