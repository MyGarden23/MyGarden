package com.android.mygarden.model.plant

import java.sql.Timestamp

/** Represents a repository that manages Plant and OwnedPlant objects. */
class PlantsRepositoryLocal : PlantsRepository {

  private var counter = 0
  private val ownedPlants: MutableList<OwnedPlant> = mutableListOf()
  private val healthCalculator = PlantHealthCalculator()

  override fun getNewId(): String {
    return counter++.toString()
  }

  override suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant {
    val ownedPlant = OwnedPlant(id, plant, lastWatered)
    ownedPlants.add(ownedPlant)
    return ownedPlant
  }

  override suspend fun getAllOwnedPlants(): List<OwnedPlant> {
    ownedPlants.forEachIndexed { index, ownedPlant ->
      ownedPlants[index] = updatePlantHealthStatus(ownedPlant)
    }
    return ownedPlants.toList()
  }

  override suspend fun getOwnedPlant(id: String): OwnedPlant {
    val index = ownedPlants.indexOfFirst { it.id == id }
    if (index == -1) {
      throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
    }

    val updatedPlant = updatePlantHealthStatus(ownedPlants[index])
    ownedPlants[index] = updatedPlant
    return updatedPlant
  }

  override suspend fun deleteFromGarden(id: String) {
    val index = ownedPlants.indexOfFirst { it.id == id }
    if (index != -1) {
      ownedPlants.removeAt(index)
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
