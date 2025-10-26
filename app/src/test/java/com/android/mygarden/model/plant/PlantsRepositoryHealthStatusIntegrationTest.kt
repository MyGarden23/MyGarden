package com.android.mygarden.model.plant

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for PlantsRepository with PlantHealthCalculator.
 *
 * These tests verify that:
 * 1. getOwnedPlant() calculates health status dynamically
 * 2. getAllOwnedPlants() calculates health status for all plants
 * 3. waterPlant() updates lastWatered correctly
 */
class PlantsRepositoryHealthStatusIntegrationTest {

  private lateinit var repository: PlantsRepositoryLocal

  @Before
  fun setup() {
    repository = PlantsRepositoryLocal()
  }

  // Helper to create timestamp X days ago
  private fun daysAgo(days: Double): Timestamp {
    val millisAgo = (days * TimeUnit.DAYS.toMillis(1)).toLong()
    return Timestamp(System.currentTimeMillis() - millisAgo)
  }

  // Helper to create a test plant
  private fun createTestPlant(
      name: String = "Test Plant",
      wateringFrequency: Int = 7,
      healthStatus: PlantHealthStatus = PlantHealthStatus.HEALTHY
  ): Plant {
    return Plant(
        name = name,
        image = null,
        latinName = "Testus Plantus",
        description = "A test plant",
        healthStatus = healthStatus,
        healthStatusDescription = "Test description",
        wateringFrequency = wateringFrequency)
  }

  // Tests for getOwnedPlant()

  @Test
  fun getOwnedPlant_calculatesHealthyStatus_whenInOptimalRange() = runTest {
    val plant = createTestPlant(wateringFrequency = 7)
    val lastWatered = daysAgo(3.5) // 50% of 7-day cycle

    repository.saveToGarden(plant, "plant-1", lastWatered)
    val retrieved = repository.getOwnedPlant("plant-1")

    assertEquals(PlantHealthStatus.HEALTHY, retrieved.plant.healthStatus)
  }

  @Test
  fun getOwnedPlant_calculatesOverwateredStatus_whenWateredRecently() = runTest {
    val plant = createTestPlant(wateringFrequency = 10)
    val lastWatered = daysAgo(1.5) // 15% of 10-day cycle

    repository.saveToGarden(plant, "plant-1", lastWatered)
    val retrieved = repository.getOwnedPlant("plant-1")

    assertEquals(PlantHealthStatus.OVERWATERED, retrieved.plant.healthStatus)
  }

  @Test
  fun getOwnedPlant_calculatesSlightlyDryStatus_whenApproachingWateringTime() = runTest {
    val plant = createTestPlant(wateringFrequency = 7)
    val lastWatered = daysAgo(5.5) // ~79% of 7-day cycle

    repository.saveToGarden(plant, "plant-1", lastWatered)
    val retrieved = repository.getOwnedPlant("plant-1")

    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, retrieved.plant.healthStatus)
  }

  @Test
  fun getOwnedPlant_calculatesNeedsWaterStatus_whenOverdue() = runTest {
    val plant = createTestPlant(wateringFrequency = 7)
    val lastWatered = daysAgo(8.0) // ~114% of 7-day cycle

    repository.saveToGarden(plant, "plant-1", lastWatered)
    val retrieved = repository.getOwnedPlant("plant-1")

    assertEquals(PlantHealthStatus.NEEDS_WATER, retrieved.plant.healthStatus)
  }

  @Test
  fun getOwnedPlant_calculatesSeverelyDryStatus_whenCritical() = runTest {
    val plant = createTestPlant(wateringFrequency = 5)
    val lastWatered = daysAgo(10.0) // 200% of 5-day cycle

    repository.saveToGarden(plant, "plant-1", lastWatered)
    val retrieved = repository.getOwnedPlant("plant-1")

    assertEquals(PlantHealthStatus.SEVERELY_DRY, retrieved.plant.healthStatus)
  }

  // Tests for getAllOwnedPlants()

  @Test
  fun getAllOwnedPlants_calculatesStatusForAllPlants() = runTest {
    // Add 3 plants with different statuses
    val plant1 = createTestPlant(name = "Plant1", wateringFrequency = 7)
    val plant2 = createTestPlant(name = "Plant2", wateringFrequency = 7)
    val plant3 = createTestPlant(name = "Plant3", wateringFrequency = 7)

    repository.saveToGarden(plant1, "id-1", daysAgo(3.5)) // HEALTHY (50%)
    repository.saveToGarden(plant2, "id-2", daysAgo(1.0)) // OVERWATERED (14%)
    repository.saveToGarden(plant3, "id-3", daysAgo(8.0)) // NEEDS_WATER (114%)

    val allPlants = repository.getAllOwnedPlants()

    assertEquals(3, allPlants.size)
    assertEquals(PlantHealthStatus.HEALTHY, allPlants[0].plant.healthStatus)
    assertEquals(PlantHealthStatus.OVERWATERED, allPlants[1].plant.healthStatus)
    assertEquals(PlantHealthStatus.NEEDS_WATER, allPlants[2].plant.healthStatus)
  }

  @Test
  fun getAllOwnedPlants_calculatesStatusForDifferentFrequencies() = runTest {
    // Different plants with different watering frequencies
    val cactus = createTestPlant(name = "Cactus", wateringFrequency = 30)
    val tropical = createTestPlant(name = "Tropical", wateringFrequency = 2)

    repository.saveToGarden(cactus, "id-1", daysAgo(15.0)) // 50% of 30-day cycle
    repository.saveToGarden(tropical, "id-2", daysAgo(1.0)) // 50% of 2-day cycle

    val allPlants = repository.getAllOwnedPlants()

    assertEquals(2, allPlants.size)
    // Both should be HEALTHY (50% of their respective cycles)
    assertEquals(PlantHealthStatus.HEALTHY, allPlants[0].plant.healthStatus)
    assertEquals(PlantHealthStatus.HEALTHY, allPlants[1].plant.healthStatus)
  }

  // Tests for waterPlant()

  @Test
  fun waterPlant_updatesLastWateredTimestamp_checksHealthStatus() = runTest {
    val plant = createTestPlant(wateringFrequency = 7)
    val initialWatering = daysAgo(8.0) // Old watering

    repository.saveToGarden(plant, "plant-1", initialWatering)

    // Water the plant now
    val newWateringTime = Timestamp(System.currentTimeMillis())
    repository.waterPlant("plant-1", newWateringTime)

    val retrieved = repository.getOwnedPlant("plant-1")
    assertEquals(newWateringTime, retrieved.lastWatered)
  }

  @Test
  fun waterPlant_changesHealthStatusAfterWatering() = runTest {
    val plant = createTestPlant(wateringFrequency = 7)
    val oldWatering = daysAgo(8.0) // 114% - NEEDS_WATER

    repository.saveToGarden(plant, "plant-1", oldWatering)

    // Verify initial status
    val beforeWatering = repository.getOwnedPlant("plant-1")
    assertEquals(PlantHealthStatus.NEEDS_WATER, beforeWatering.plant.healthStatus)

    // Water the plant now
    repository.waterPlant("plant-1", Timestamp(System.currentTimeMillis()))

    // Status should change to HEALTHY (grace period applies since plant was at 114% - needed water)
    val afterWatering = repository.getOwnedPlant("plant-1")
    assertEquals(PlantHealthStatus.HEALTHY, afterWatering.plant.healthStatus)
  }

  @Test
  fun waterPlant_severelyOverwatered_whenWateredTooEarly() = runTest {
    val plant = createTestPlant(wateringFrequency = 7)
    val oldWatering = daysAgo(3.0) // 43% - HEALTHY (too early to water)

    repository.saveToGarden(plant, "plant-1", oldWatering)

    // Verify initial status
    val beforeWatering = repository.getOwnedPlant("plant-1")
    assertEquals(PlantHealthStatus.HEALTHY, beforeWatering.plant.healthStatus)

    // Water the plant now (too early!)
    repository.waterPlant("plant-1", Timestamp(System.currentTimeMillis()))

    // Status should be SEVERELY_OVERWATERED (no grace period since plant didn't need water)
    val afterWatering = repository.getOwnedPlant("plant-1")
    assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, afterWatering.plant.healthStatus)
  }

  @Test
  fun waterPlant_throwsExceptionForNonExistentPlant() = runTest {
    try {
      repository.waterPlant("non-existent-id")
      fail("Expected IllegalArgumentException")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("not found") == true)
    }
  }

  @Test
  fun waterPlant_worksMultipleTimesOnSamePlant() = runTest {
    val plant = createTestPlant(wateringFrequency = 7)
    repository.saveToGarden(plant, "plant-1", daysAgo(10.0))

    // First watering
    val firstWatering = daysAgo(3.5)
    repository.waterPlant("plant-1", firstWatering)
    val afterFirst = repository.getOwnedPlant("plant-1")
    assertEquals(firstWatering, afterFirst.lastWatered)

    // Second watering
    val secondWatering = Timestamp(System.currentTimeMillis())
    repository.waterPlant("plant-1", secondWatering)
    val afterSecond = repository.getOwnedPlant("plant-1")
    assertEquals(secondWatering, afterSecond.lastWatered)
  }

  // Tests to verify original plant data is preserved

  @Test
  fun getOwnedPlant_preservesOriginalPlantData() = runTest {
    val originalPlant = createTestPlant(name = "Rose", wateringFrequency = 5)
    val lastWatered = daysAgo(2.5) // 50% - HEALTHY

    repository.saveToGarden(originalPlant, "plant-1", lastWatered)
    val retrieved = repository.getOwnedPlant("plant-1")

    // Verify all original data is preserved (except health status which is calculated)
    assertEquals("Rose", retrieved.plant.name)
    assertEquals("Testus Plantus", retrieved.plant.latinName)
    assertEquals("A test plant", retrieved.plant.description)
    assertEquals(5, retrieved.plant.wateringFrequency)
    assertEquals(lastWatered, retrieved.lastWatered)
  }

  @Test
  fun waterPlant_doesNotAffectOtherPlants() = runTest {
    val plant1 = createTestPlant(name = "Plant1", wateringFrequency = 7)
    val plant2 = createTestPlant(name = "Plant2", wateringFrequency = 7)

    val timestamp1 = daysAgo(3.0)
    val timestamp2 = daysAgo(4.0)

    repository.saveToGarden(plant1, "id-1", timestamp1)
    repository.saveToGarden(plant2, "id-2", timestamp2)

    // Water only plant1
    val newWateringTime = Timestamp(System.currentTimeMillis())
    repository.waterPlant("id-1", newWateringTime)

    // Plant1 should be updated
    val retrieved1 = repository.getOwnedPlant("id-1")
    assertEquals(newWateringTime, retrieved1.lastWatered)

    // Plant2 should remain unchanged
    val retrieved2 = repository.getOwnedPlant("id-2")
    assertEquals(timestamp2, retrieved2.lastWatered)
  }
}
