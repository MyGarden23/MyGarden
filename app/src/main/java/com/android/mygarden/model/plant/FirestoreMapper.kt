package com.android.mygarden.model.plant

import java.sql.Timestamp

/**
 * Represent a serialized plant compatible for firestore.
 *
 * This data class encapsulates all essential information about a plant, including its
 * identification, visual representation, health status, best conditions to grow and care
 * requirements.
 *
 * @property name The common name of the plant (e.g., "Rose", "Tomato").
 * @property image The visual representation of the plant, in String? because it is an URL to find
 *   the actual image.
 * @property latinName The scientific/botanical name of the plant (e.g., "Rosa rubiginosa").
 * @property description A detailed text description of the plant, including care instructions.
 * @property location The best location for the plant to grow, indoor or outdoor.
 * @property lightExposure A description of the ideal light exposure for this plant.
 * @property healthStatus The current health condition of the plant.
 * @property healthStatusDescription A detailed description of the plant's health status.
 * @property wateringFrequency How often the plant should be watered, measured in days.
 * @property recognized A boolean to tell if the plant was recognised by the API or not.
 */
data class SerializedPlant(
    val name: String = "Unknown",
    val image: String? = null,
    val latinName: String = "Unknown",
    val description: String = "No description available",
    val location: String = "UNKNOWN",
    val lightExposure: String = "Unknown",
    val healthStatus: String = "UNKNOWN",
    val healthStatusDescription: String = "No health status description available",
    val wateringFrequency: Int = 0, // in days,
    val recognized: Boolean = false,
)

/**
 * Represents a serialized plant owned by a user in their virtual garden compatible for firestore.
 *
 * This data class encapsulates ownership information, including watering history needed to
 * accurately calculate the plant's current health status.
 *
 * @property id A unique identifier for this owned plant instance.
 * @property plant The plant information (species, care requirements, etc.).
 * @property lastWatered Timestamp of when the plant was most recently watered.
 * @property previousLastWatered Optional timestamp of the watering before lastWatered.
 */
data class SerializedOwnedPlant(
    val id: String = "",
    val plant: SerializedPlant = SerializedPlant(),
    val lastWatered: Long = 0,
    val previousLastWatered: Long = 0,
    val dateOfCreation: Long = 0
)

/**
 * Utility object providing mapping functions between the classes in local memory [Plant],
 * [OwnedPlant] and their Firestore-serializable representations [SerializedPlant],
 * [SerializedOwnedPlant].
 *
 * These conversion functions are needed because [Plant] and [OwnedPlant] use types that Firestore
 * cannot store.
 */
object FirestoreMapper {

  /**
   * Converts an [OwnedPlant] instance into its Firestore-compatible [SerializedOwnedPlant]
   * representation.
   *
   * @param ownedPlant The owned plant instance in local.
   * @return A [SerializedOwnedPlant] ready to be stored in Firestore.
   */
  fun fromOwnedPlantToSerializedOwnedPlant(ownedPlant: OwnedPlant): SerializedOwnedPlant {
    return SerializedOwnedPlant(
        id = ownedPlant.id,
        plant = fromPlantToSerializedPlant(ownedPlant.plant),
        lastWatered = ownedPlant.lastWatered.time,
        previousLastWatered = ownedPlant.previousLastWatered?.time ?: 0L,
        dateOfCreation = ownedPlant.dateOfCreation.time
    )
  }

  /**
   * Converts a Firestore-compatible [SerializedOwnedPlant] back into an [OwnedPlant].
   *
   * @param sOwnedPlant The serialized representation retrieved from Firestore.
   * @return The reconstructed [OwnedPlant].
   */
  fun fromSerializedOwnedPlantToOwnedPlant(sOwnedPlant: SerializedOwnedPlant): OwnedPlant {
    return OwnedPlant(
        id = sOwnedPlant.id,
        plant = fromSerializedPlantToPlant(sOwnedPlant.plant),
        lastWatered = Timestamp(sOwnedPlant.lastWatered),
        previousLastWatered =
            if (sOwnedPlant.previousLastWatered != 0L) Timestamp(sOwnedPlant.previousLastWatered)
            else null,
        dateOfCreation = Timestamp(sOwnedPlant.dateOfCreation)
    )
  }

  /**
   * Converts a [Plant] into its Firestore-compatible [SerializedPlant] form.
   *
   * @param plant The plant instance in local.
   * @return A [SerializedPlant] Firestore-compatible.
   */
  fun fromPlantToSerializedPlant(plant: Plant): SerializedPlant {
    return SerializedPlant(
        name = plant.name,
        image = plant.image,
        latinName = plant.latinName,
        description = plant.description,
        location = plant.location.name,
        lightExposure = plant.lightExposure,
        healthStatus = plant.healthStatus.name,
        healthStatusDescription = plant.healthStatusDescription,
        wateringFrequency = plant.wateringFrequency,
        recognized = plant.isRecognized)
  }

  /**
   * Converts a Firestore-compatible [SerializedPlant] back into a [Plant].
   *
   * This function handles invalid or unknown enum values safely by falling back to
   * PlantLocation.UNKNOWN or PlantHealthStatus.UNKNOWN.
   *
   * @param sPlant The serialized plant data retrieved from Firestore.
   * @return The corresponding [Plant].
   */
  fun fromSerializedPlantToPlant(sPlant: SerializedPlant): Plant {
    return Plant(
        name = sPlant.name,
        image = sPlant.image,
        latinName = sPlant.latinName,
        description = sPlant.description,
        location =
            try {
              PlantLocation.valueOf(sPlant.location)
            } catch (e: IllegalArgumentException) {
              PlantLocation.UNKNOWN // fallback for invalid enum values
            },
        lightExposure = sPlant.lightExposure,
        healthStatus =
            try {
              PlantHealthStatus.valueOf(sPlant.healthStatus)
            } catch (e: IllegalArgumentException) {
              PlantHealthStatus.UNKNOWN // fallback for invalid enum values
            },
        healthStatusDescription = sPlant.healthStatusDescription,
        wateringFrequency = sPlant.wateringFrequency,
        isRecognized = sPlant.recognized)
  }
}
