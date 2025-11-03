package com.android.mygarden.model.plant

import java.sql.Timestamp

data class SerializedPlant(
    val name: String = "Unknown",
    val image: String? = null,
    val latinName: String = "Unknown",
    val description: String = "No description available",
    val healthStatus: String = "UNKNOWN",
    val healthStatusDescription: String = "No health status description available",
    val wateringFrequency: Int = 0, // in days
)

data class SerializedOwnedPlant(
    val id: String = "",
    val plant: SerializedPlant = SerializedPlant(),
    val lastWatered: Long = 0,
    val previousLastWatered: Long = 0
)

object FirestoreMapper {

  fun fromOwnedPlantToSerializedOwnedPlant(ownedPlant: OwnedPlant): SerializedOwnedPlant {
    return SerializedOwnedPlant(
        id = ownedPlant.id,
        plant = fromPlantToSerializedPlant(ownedPlant.plant),
        lastWatered = ownedPlant.lastWatered.time,
        previousLastWatered = ownedPlant.previousLastWatered?.time ?: 0L)
  }

  fun fromSerializedOwnedPlantToOwnedPlant(sOwnedPlant: SerializedOwnedPlant): OwnedPlant {
    return OwnedPlant(
        id = sOwnedPlant.id,
        plant = fromSerializedPlantToPlant(sOwnedPlant.plant),
        lastWatered = Timestamp(sOwnedPlant.lastWatered),
        previousLastWatered =
            if (sOwnedPlant.previousLastWatered != 0L) Timestamp(sOwnedPlant.previousLastWatered)
            else null)
  }

  fun fromPlantToSerializedPlant(plant: Plant): SerializedPlant {
    return SerializedPlant(
        name = plant.name,
        image = plant.image,
        latinName = plant.latinName,
        description = plant.description,
        healthStatus = plant.healthStatus.name,
        healthStatusDescription = plant.healthStatusDescription,
        wateringFrequency = plant.wateringFrequency)
  }

  fun fromSerializedPlantToPlant(sPlant: SerializedPlant): Plant {
    return Plant(
        name = sPlant.name,
        image = sPlant.image,
        latinName = sPlant.latinName,
        description = sPlant.description,
        healthStatus =
            try {
              PlantHealthStatus.valueOf(sPlant.healthStatus)
            } catch (e: IllegalArgumentException) {
              PlantHealthStatus.UNKNOWN // fallback
            },
        healthStatusDescription = sPlant.healthStatusDescription,
        wateringFrequency = sPlant.wateringFrequency)
  }
}
