package com.android.mygarden.model.plant

import java.sql.Timestamp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Represents a repository that manages Plant and OwnedPlant objects. */
class PlantsRepositoryLocal(
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : PlantsRepositoryBase() {

  private var counter = 0
  private val healthCalculator = PlantHealthCalculator()

  override val tickDelay: Duration = 2.seconds

  private val _plants = MutableStateFlow<List<OwnedPlant>>(emptyList())

  /**
   * This flow updates the plant health status of each plant either when
   * 1) the list of plants is updated
   * 2) a tick is emitted then emit the updated list ; to be collected by the pop-up VM
   */
  override val plantsFlow: StateFlow<List<OwnedPlant>> =
      combine(_plants, ticks) { plants, time -> plants.map { updatePlantHealthStatus(it) } }
          .distinctUntilChanged()
          .stateIn(
              scope,
              SharingStarted.WhileSubscribed(plantsFlowTimeoutWhenNoSubscribers),
              emptyList())

  override fun getNewId(): String {
    return counter++.toString()
  }

  override suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant {
    val ownedPlant = OwnedPlant(id, plant, lastWatered)
    _plants.update { prev -> prev + ownedPlant }
    return ownedPlant
  }

  override suspend fun getAllOwnedPlants(): List<OwnedPlant> {
    // ensures flow emission when called
    _plants.update { plants -> plants.map { updatePlantHealthStatus(it) } }
    return _plants.value.toList()
  }

  override suspend fun getOwnedPlant(id: String): OwnedPlant {
    val ownedPlant = _plants.value.firstOrNull { it.id == id }
    if (ownedPlant == null) {
      throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
    } else {
        //Update the status of the plant because we need a refresh the status
      return updatePlantHealthStatus(ownedPlant)
    }
  }

  override suspend fun deleteFromGarden(id: String) {
    val previousListSize = _plants.value.size
    _plants.update { plants -> plants.filterNot { it.id == id } }
    if (previousListSize == _plants.value.size) {
      throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
    }
  }

  override suspend fun editOwnedPlant(id: String, newOwnedPlant: OwnedPlant) {
    if (id != newOwnedPlant.id) {
      throw IllegalArgumentException(
          "PlantsRepositoryLocal: ID mismatch - parameter id '$id' does not match newOwnedPlant.id '${newOwnedPlant.id}'")
    }
    var found = false

    _plants.update { plants ->
      plants.map {
        if (it.id == id) {
          found = true
          newOwnedPlant
        } else it
      }
    }

    if (!found) {
      throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
    }
  }

  override suspend fun waterPlant(id: String, wateringTime: Timestamp) {
    val ownedPlant =
        _plants.value.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException(
                "PlantsRepositoryLocal: OwnedPlant with id $id not found")
    val previousWatering = ownedPlant.lastWatered
    val updatedPlant =
        ownedPlant.copy(lastWatered = wateringTime, previousLastWatered = previousWatering)
    _plants.update { plants -> plants.map { if (it.id == id) updatedPlant else it } }
  }

  /**
   * Updates the health status of a plant based on current watering cycle.
   *
   * @param ownedPlant The plant to update
   * @return A copy of the plant with updated health status
   */
  private fun updatePlantHealthStatus(ownedPlant: OwnedPlant): OwnedPlant {
    val calculatedStatus =
        healthCalculator.calculateHealthStatus(
            lastWatered = ownedPlant.lastWatered,
            wateringFrequency = ownedPlant.plant.wateringFrequency,
            previousLastWatered = ownedPlant.previousLastWatered)
    val updatedPlant = ownedPlant.plant.copy(healthStatus = calculatedStatus)
    return ownedPlant.copy(plant = updatedPlant)
  }
}
