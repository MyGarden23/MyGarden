package com.android.mygarden.model.plant

import androidx.annotation.StringRes
import com.android.mygarden.R
import java.sql.Timestamp

/**
 * Represents a plant in the garden management system.
 *
 * This data class encapsulates all essential information about a plant, including its
 * identification, visual representation, health status, and care requirements.
 *
 * @property name The common name of the plant (e.g., "Rose", "Tomato")
 * @property image The visual representation of the plant
 * @property latinName The scientific/botanical name of the plant (e.g., "Rosa rubiginosa")
 * @property description A detailed text description of the plant, including care instructions
 * @property healthStatus The current health condition of the plant
 * @property healthStatusDescription A detailed description of the plant's health status
 * @property wateringFrequency How often the plant should be watered, measured in days
 */
data class Plant(
    val name: String = "Unknown",
    val image: String? = null,
    val latinName: String = "Unknown",
    val description: String = "No description available",
    val healthStatus: PlantHealthStatus = PlantHealthStatus.UNKNOWN,
    val healthStatusDescription: String = "No health status description available",
    val wateringFrequency: Int = 0, // in days
)

/**
 * Represents a plant owned by a user in their virtual garden.
 *
 * This data class encapsulates ownership information, including watering history needed to
 * accurately calculate the plant's current health status.
 *
 * @property id A unique identifier for this owned plant instance
 * @property plant The plant information (species, care requirements, etc.)
 * @property lastWatered Timestamp of when the plant was most recently watered
 * @property previousLastWatered Optional timestamp of the watering before lastWatered.
 */
data class OwnedPlant(
    val id: String,
    val plant: Plant,
    val lastWatered: Timestamp,
    val previousLastWatered: Timestamp? = null
)

/**
 * Represents the health status of a plant.
 *
 * This enum class provides a set of predefined health conditions that a plant can be in.
 *
 * @property descriptionRes Resource ID for the localized description from strings.xml
 */
enum class PlantHealthStatus(@StringRes val descriptionRes: Int) {
  SEVERELY_OVERWATERED(R.string.plant_health_severely_overwatered),
  OVERWATERED(R.string.plant_health_overwatered),
  HEALTHY(R.string.plant_health_healthy),
  SLIGHTLY_DRY(R.string.plant_health_slightly_dry),
  NEEDS_WATER(R.string.plant_health_needs_water),
  SEVERELY_DRY(R.string.plant_health_severely_dry),
  UNKNOWN(R.string.plant_health_unknown);

  /** Fallback description in English for contexts without Android Resources. Util for tests. */
  val description: String
    get() =
        when (this) {
          SEVERELY_OVERWATERED -> "Severely overwatered 🌊"
          OVERWATERED -> "Overwatered 💦"
          HEALTHY -> "The plant is healthy 🌱"
          SLIGHTLY_DRY -> "A bit dry 🍂"
          NEEDS_WATER -> "Needs water 💧"
          SEVERELY_DRY -> "Critically dry 🥀"
          UNKNOWN -> "Status unknown ❓"
        }
}
