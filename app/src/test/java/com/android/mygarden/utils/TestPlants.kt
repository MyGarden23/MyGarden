package com.android.mygarden.utils

import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation

/**
 * Utility object providing factory methods to easily create consistent and customizable Plant
 * instances for tests.
 */
object TestPlants {

  /**
   * Creates a `Plant` instance pre-filled with consistent test data.
   *
   * This helper is intended for unit tests to avoid duplicating boilerplate when constructing
   * `Plant` objects. Default values are provided for all fields, and the most commonly varied
   * parameters can be overridden.
   *
   * @param name The display name of the plant. Defaults to `"Test Plant"`.
   * @param latinName The botanical/latin name of the plant. Defaults to `"Testus Plantus"`.
   * @param healthStatus The health status to assign to the plant. Defaults to `HEALTHY`.
   * @param location The location category of the plant (indoor/outdoor/unknown). Defaults to
   *   `UNKNOWN`.
   * @return A fully constructed [Plant] instance suitable for use in tests.
   */
  fun createTestPlant(
      name: String = "Test Plant",
      latinName: String = "Testus Plantus",
      healthStatus: PlantHealthStatus = PlantHealthStatus.HEALTHY,
      location: PlantLocation = PlantLocation.UNKNOWN
  ): Plant {
    return Plant(
        name = name,
        image = null,
        latinName = latinName,
        description = "A test plant description",
        location = location,
        lightExposure = "A light exposure description",
        healthStatus = healthStatus,
        healthStatusDescription = healthStatus.description,
        wateringFrequency = 7)
  }

  /*-------------------------- FICTIONAL PLANTS -------------------*/
  // use one of those if you don't need specific ones
  val plant1: Plant =
      createTestPlant(
          name = "test plant 1",
          latinName = "test in latin plant 1",
          healthStatus = PlantHealthStatus.SEVERELY_OVERWATERED,
          location = PlantLocation.OUTDOOR)

  val plant2: Plant =
      createTestPlant(
          name = "test plant 2",
          latinName = "test in latin plant 2",
          healthStatus = PlantHealthStatus.SEVERELY_DRY,
          location = PlantLocation.INDOOR)

  val plant3: Plant =
      createTestPlant(
          name = "test plant 3",
          latinName = "test in latin plant 3",
          healthStatus = PlantHealthStatus.SLIGHTLY_DRY)

  val plant4: Plant =
      createTestPlant(
          name = "test plant 4",
          latinName = "test in latin plant 4",
          healthStatus = PlantHealthStatus.NEEDS_WATER)

  val healthyPlant: Plant =
      Plant(
          name = "I'm okay",
          image = null,
          latinName = "laurem ispum",
          description = "all good for now",
          healthStatus = PlantHealthStatus.HEALTHY,
          wateringFrequency = 1)

  val almostThirstyPlant: Plant =
      Plant(
          name = "Water?",
          image = null,
          latinName = "laurem ipsum",
          description = "edge plant that will soon be thirsty",
          healthStatus = PlantHealthStatus.UNKNOWN,
          wateringFrequency = 1)

  val healthyAloeVera: Plant =
      Plant(
          name = "Aloe Vera",
          latinName = "Aloe barbadensis",
          wateringFrequency = 14,
          healthStatus = PlantHealthStatus.HEALTHY)

  val dryCactus: Plant =
      Plant(
          name = "Cactus",
          latinName = "Cactaceae",
          wateringFrequency = 7,
          healthStatus = PlantHealthStatus.NEEDS_WATER)

  val healthyBamboo: Plant =
      Plant(
          name = "Bamboo",
          latinName = "Bambusoideae",
          wateringFrequency = 5,
          healthStatus = PlantHealthStatus.HEALTHY)

  val samplePlant1: Plant =
      Plant(
          name = "hello",
          image = null,
          latinName = "laurem ipsum",
          description = "beautiful plant",
          location = PlantLocation.INDOOR,
          lightExposure = "Direct light",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "is healthy",
          wateringFrequency = 10)

  val samplePlant2: Plant =
      Plant(
          name = "world",
          image = null,
          latinName = "laurem ipsum",
          description = "even more beautiful plant",
          location = PlantLocation.INDOOR,
          lightExposure = "Undirect light",
          healthStatus = PlantHealthStatus.NEEDS_WATER,
          healthStatusDescription = "is thirsty",
          wateringFrequency = 10)

  val samplePlant3: Plant =
      Plant(
          name = "Poseidon",
          image = null,
          latinName = "laurem ipsum",
          description = "water ++ plant",
          location = PlantLocation.OUTDOOR,
          lightExposure = "Morning light",
          healthStatus = PlantHealthStatus.OVERWATERED,
          healthStatusDescription = "is full",
          wateringFrequency = 10)

  val samplePlant4: Plant =
      Plant(
          name = "Anonymous",
          image = null,
          latinName = "laurem ipsum",
          description = "who is this guy",
          location = PlantLocation.INDOOR,
          lightExposure = "Afternoon light",
          healthStatus = PlantHealthStatus.UNKNOWN,
          healthStatusDescription = "is ?",
          wateringFrequency = 10)

  val plantInfoPlant: Plant =
      Plant(
          name = "test_plant",
          image = null,
          latinName = "testinus_plantus",
          description = "This is a test plant.",
          location = PlantLocation.INDOOR,
          lightExposure = "Test light exposure",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "This plant is healthy.",
          wateringFrequency = 1,
      )

  val popUpThirstyPlant: Plant =
      Plant(
          name = "WATER!",
          image = null,
          latinName = "laurem ipsum",
          description = "already thirsty plant!",
          healthStatus = PlantHealthStatus.NEEDS_WATER,
          healthStatusDescription = "already ?!",
          wateringFrequency = 1)

  val popUpAlmostThirstyPlant: Plant =
      Plant(
          name = "Water?",
          image = null,
          latinName = "laurem ipsum",
          description = "edge plant that will soon be thirsty",
          healthStatus = PlantHealthStatus.UNKNOWN,
          healthStatusDescription = "we don't care about the initial, will be recomputed soon",
          wateringFrequency = 1)

  val gardenActivityPlant: Plant =
      Plant(
          name = "Test Rose",
          latinName = "Rosa test",
          description = "A test rose",
          location = PlantLocation.OUTDOOR,
          lightExposure = "Full sun",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "Healthy",
          wateringFrequency = 3,
          isRecognized = true,
          image = "https://example.com/rose.jpg")

  val plantToSerialize: Plant =
      Plant(
          name = "Watered Plant",
          latinName = "Aqua planta",
          description = "A plant used for watering test",
          location = PlantLocation.INDOOR,
          lightExposure = "Partial shade",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "Hydrated",
          wateringFrequency = 5,
          isRecognized = true,
          image = "https://example.com/watered.jpg")

  val recognizedPlant: Plant =
      Plant(
          name = "Rose",
          image = null,
          latinName = "Rosalia",
          location = PlantLocation.OUTDOOR,
          lightExposure = "Light at least 6 hours a day.",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "This is a good description.",
          wateringFrequency = 3,
          isRecognized = true)

  val orchid: Plant =
      Plant(
          name = "Orchid",
          image = null,
          latinName = "Orchidaceae",
          description = "An elegant flowering plant",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "Perfect condition",
          wateringFrequency = 10)
}
