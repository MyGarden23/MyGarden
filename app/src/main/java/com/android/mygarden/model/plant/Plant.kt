package com.android.mygarden.model.plant

import java.sql.Timestamp

/**
 * Represents a plant in the garden management system.
 *
 * This data class encapsulates all essential information about a plant, including its
 * identification, visual representation, health status, and care requirements.
 *
 * @param healthStatusDescription A detailed description of the plant's health status
 * @property name The common name of the plant (e.g., "Rose", "Tomato")
 * @property image The visual representation of the plant
 * @property latinName The scientific/botanical name of the plant (e.g., "Rosa rubiginosa")
 * @property description A detailed text description of the plant, including care instructions
 * @property healthStatus The current health condition of the plant
 * @property wateringFrequency How often the plant should be watered, measured in days
 */
data class Plant(
    val name: String,
    val image: String?,
    val latinName: String,
    val description: String,
    val healthStatus: PlantHealthStatus,
    val healthStatusDescription: String,
    val wateringFrequency: Int, // in days
)

/**
 * Represents a plant owned by a user in their virtual garden.
 *
 * This data class extends the basic Plant information with tracking data specific to the user's
 * care routine, such as watering history.
 *
 * @property id A unique identifier for this owned plant instance
 * @property plant The base plant information (species, care requirements, etc.)
 * @property lastWatered The timestamp of when the plant was last watered
 */
data class OwnedPlant(
    val id: String,
    val plant: Plant,
    val lastWatered: Timestamp,
)

/**
 * Represents the health status of a plant.
 *
 * This enum class provides a set of predefined health conditions that a plant can be in.
 */
enum class PlantHealthStatus(val description: String) { // None exhaustive list of status
  HEALTHY("The plant is healthy 🌱"),
  NEEDS_WATER("The plant needs water 💧"),
  OVERWATERED("The plant is overwatered 🌊"),
  UNKNOWN("The plant’s health status is unknown ❓")
}
