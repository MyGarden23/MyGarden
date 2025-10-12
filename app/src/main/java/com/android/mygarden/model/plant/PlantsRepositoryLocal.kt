package com.android.mygarden.model.plant

import android.media.Image
import java.sql.Timestamp

/** Represents a repository that manages Plant and OwnedPlant objects. */
class PlantsRepositoryLocal : PlantsRepository {

  private var counter = 0
  private val ownedPlants: MutableList<OwnedPlant> = mutableListOf()

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
    return ownedPlants
  }

  override suspend fun getOwnedPlant(id: String): OwnedPlant {
    return ownedPlants.find { it.id == id }
      ?: throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
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
    val index = ownedPlants.indexOfFirst { it.id == id }
    if (index != -1) {
      ownedPlants[index] = newOwnedPlant
    } else {
      throw IllegalArgumentException("PlantsRepositoryLocal: OwnedPlant with id $id not found")
    }

  }
}
