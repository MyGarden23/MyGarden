package com.android.mygarden.model.plant

import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Represents a repository that manages Plant and OwnedPlant objects. */
class PlantsRepositoryLocal : PlantsRepository {

  private var counter = 0
  private val ownedPlants: MutableList<OwnedPlant> = mutableListOf()
  private val healthCalculator = PlantHealthCalculator()

  private val _plantsFlow = MutableStateFlow<List<OwnedPlant>>(emptyList())
  override val plantsFlow: StateFlow<List<OwnedPlant>> = _plantsFlow

  override fun getNewId(): String {
    return counter++.toString()
  }

  override suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant {
    val ownedPlant = OwnedPlant(id, plant, lastWatered)
    ownedPlants.add(ownedPlant)
    // Update the flow value so that it emits the updated list
    _plantsFlow.value = ownedPlants.toList()
    return ownedPlant
  }

  override suspend fun getAllOwnedPlants(): List<OwnedPlant> {
    ownedPlants.forEachIndexed { index, ownedPlant ->
      ownedPlants[index] = updatePlantHealthStatus(ownedPlant)
    }
    // Update the flow value so that it emits the updated list
    _plantsFlow.value = ownedPlants.toList()
    return ownedPlants.toList()
  }

  override suspend fun getOwnedPlant(id: String): OwnedPlant {
    val index = ownedPlants.indexOfFirst { it.id == id }
    if (index == -1) {
      throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
    }

    val updatedPlant = updatePlantHealthStatus(ownedPlants[index])
    ownedPlants[index] = updatedPlant
    // Update the flow value so that it emits the updated list
    _plantsFlow.value = ownedPlants.toList()
    return updatedPlant
  }

  override suspend fun deleteFromGarden(id: String) {
    val index = ownedPlants.indexOfFirst { it.id == id }
    if (index != -1) {
      ownedPlants.removeAt(index)
      // Update the flow value so that it emits the updated list
      _plantsFlow.value = ownedPlants.toList()
    } else {
      throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
    }
  }

  override suspend fun editOwnedPlant(id: String, newOwnedPlant: OwnedPlant) {
    if (id != newOwnedPlant.id) {
      throw IllegalArgumentException(
          "PlantsRepositoryLocal: ID mismatch - parameter id '$id' does not match newOwnedPlant.id '${newOwnedPlant.id}'")
    }
    val index = ownedPlants.indexOfFirst { it.id == id }
    if (index != -1) {
      ownedPlants[index] = newOwnedPlant
      // Update the flow value so that it emits the updated list
      _plantsFlow.value = ownedPlants.toList()
    } else {
      throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
    }
  }

  override suspend fun waterPlant(id: String, wateringTime: Timestamp) {
    val ownedPlant =
        ownedPlants.find { it.id == id }
            ?: throw IllegalArgumentException(
                "PlantsRepositoryLocal: OwnedPlant with id $id not found")
    val previousWatering = ownedPlant.lastWatered
    val updatedPlant =
        ownedPlant.copy(lastWatered = wateringTime, previousLastWatered = previousWatering)
    val index = ownedPlants.indexOfFirst { it.id == id }
    ownedPlants[index] = updatedPlant
    // Update the flow value so that it emits the updated list
    _plantsFlow.value = ownedPlants.toList()
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
