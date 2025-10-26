package com.android.mygarden.model.plant

import android.media.Image
import java.sql.Timestamp

/** Represents a repository that manages Plant and OwnedPlant objects. */
class PlantsRepositoryLocal : PlantsRepository {

  private var counter = 0
  private val ownedPlants: MutableList<OwnedPlant> = mutableListOf()
  private val healthCalculator = PlantHealthCalculator()

  override fun getNewId(): String {
    return counter++.toString()
  }

  override suspend fun identifyPlant(image: Image): Plant {
    TODO("Not yet implemented")
  }

  override suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant {
    val ownedPlant = OwnedPlant(id, plant, lastWatered)
    ownedPlants.add(ownedPlant)
    return ownedPlant
  }

  override suspend fun getAllOwnedPlants(): List<OwnedPlant> {
    return ownedPlants.map { updatePlantHealthStatus(it) }
  }

  override suspend fun getOwnedPlant(id: String): OwnedPlant {
    val ownedPlant = getRawOwnedPlant(id)
    return updatePlantHealthStatus(ownedPlant)
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
    val ownedPlant = getRawOwnedPlant(id)
    val previousWatering = ownedPlant.lastWatered
    val updatedPlant =
        ownedPlant.copy(lastWatered = wateringTime, previousLastWatered = previousWatering)
    val index = ownedPlants.indexOfFirst { it.id == id }
    ownedPlants[index] = updatedPlant
  }

  /**
   * Retrieves an owned plant from the internal list without calculating health status. Used
   * internally for operations that don't need the calculated status.
   *
   * @param id The unique identifier of the plant to retrieve
   * @return The OwnedPlant object as stored (without health status calculation)
   * @throws IllegalArgumentException if plant with given id is not found
   */
  private fun getRawOwnedPlant(id: String): OwnedPlant {
    return ownedPlants.find { it.id == id }
        ?: throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
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
