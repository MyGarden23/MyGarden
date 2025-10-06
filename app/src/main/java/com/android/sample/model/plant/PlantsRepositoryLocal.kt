package com.android.sample.model.plant

import android.media.Image
import java.sql.Timestamp

/** Represents a repository that manages Plant and OwnedPlant objects.*/
class PlantsRepositoryLocal: PlantsRepository {

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
        TODO("Not yet implemented")
    }

    override suspend fun getOwnedPlant(id: String): OwnedPlant {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFromGarden(id: String) {
        TODO("Not yet implemented")
    }

    override suspend fun editOwnedPlant(id: String, lastWatered: Timestamp) {
        TODO("Not yet implemented")
    }
}