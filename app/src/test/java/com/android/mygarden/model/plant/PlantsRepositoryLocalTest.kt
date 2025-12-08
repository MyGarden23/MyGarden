package com.android.mygarden.model.plant

import java.sql.Timestamp
import kotlin.collections.get
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlantsRepositoryLocalTest {

  private lateinit var repository: PlantsRepositoryLocal

  @Before
  fun setup() {
    repository = PlantsRepositoryLocal()
  }

  // Helper function to create a test plant without needing a real Image
  private fun createTestPlant(
      name: String = "Test Plant",
      latinName: String = "Testus Plantus",
      healthStatus: PlantHealthStatus = PlantHealthStatus.HEALTHY
  ): Plant {

    return Plant(
        name = name,
        image = null,
        latinName = latinName,
        description = "A test plant description",
        healthStatus = healthStatus,
        healthStatusDescription = healthStatus.description,
        wateringFrequency = 7)
  }

  @Test
  fun getNewId_returnsZeroForFirstCall() {
    val id = repository.getNewId()
    assertEquals("0", id)
  }

  @Test
  fun getNewId_returnsIncrementedIds() {
    val id1 = repository.getNewId()
    val id2 = repository.getNewId()
    val id3 = repository.getNewId()

    assertEquals("0", id1)
    assertEquals("1", id2)
    assertEquals("2", id3)
  }

  @Test
  fun getNewId_generatesUniqueIds() {
    val id1 = repository.getNewId()
    val id2 = repository.getNewId()

    assertNotEquals(id1, id2)
  }

  @Test
  fun saveToGarden_returnsOwnedPlantWithCorrectData() = runTest {
    val plant = createTestPlant(name = "Rose", latinName = "Rosa rubiginosa")
    val id = "test-id-1"
    val timestamp = Timestamp(System.currentTimeMillis())

    val ownedPlant = repository.saveToGarden(plant, id, timestamp)

    assertEquals(id, ownedPlant.id)
    assertEquals(plant, ownedPlant.plant)
    assertEquals(timestamp, ownedPlant.lastWatered)
  }

  @Test
  fun saveToGarden_addsPlantToRepository() = runTest {
    val plant = createTestPlant(name = "Cactus", latinName = "Cactaceae")
    val id = "cactus-1"
    val timestamp = Timestamp(System.currentTimeMillis())

    repository.saveToGarden(plant, id, timestamp)

    // Verify by saving another plant and checking IDs are different
    val plant2 =
        createTestPlant(
            name = "Fern", latinName = "Pteridium", healthStatus = PlantHealthStatus.NEEDS_WATER)
    val id2 = "fern-1"
    val ownedPlant2 = repository.saveToGarden(plant2, id2, timestamp)

    assertEquals("fern-1", ownedPlant2.id)
    assertEquals(plant2, ownedPlant2.plant)
  }

  @Test
  fun saveToGarden_allowsDuplicatePlants() = runTest {
    val plant = createTestPlant(name = "Tulip", latinName = "Tulipa")
    val timestamp = Timestamp(System.currentTimeMillis())

    val ownedPlant1 = repository.saveToGarden(plant, "tulip-1", timestamp)
    val ownedPlant2 = repository.saveToGarden(plant, "tulip-2", timestamp)

    // Both should be saved successfully with different IDs
    assertEquals("tulip-1", ownedPlant1.id)
    assertEquals("tulip-2", ownedPlant2.id)
    assertEquals(plant, ownedPlant1.plant)
    assertEquals(plant, ownedPlant2.plant)
  }

  @Test
  fun saveToGarden_handlesAllHealthStatuses() = runTest {
    val healthStatuses =
        listOf(
            PlantHealthStatus.HEALTHY,
            PlantHealthStatus.NEEDS_WATER,
            PlantHealthStatus.OVERWATERED,
            PlantHealthStatus.UNKNOWN)

    healthStatuses.forEachIndexed { index, status ->
      val plant =
          createTestPlant(
              name = "Plant $index", latinName = "Plantus $index", healthStatus = status)
      val timestamp = Timestamp(System.currentTimeMillis())

      val ownedPlant = repository.saveToGarden(plant, "id-$index", timestamp)

      assertEquals(status, ownedPlant.plant.healthStatus)
    }
  }

  @Test
  fun saveToGarden_preservesAllPlantProperties() = runTest {
    val plant =
        Plant(
            name = "Orchid",
            image = null,
            latinName = "Orchidaceae",
            description = "An elegant flowering plant",
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "Perfect condition",
            wateringFrequency = 10)
    val timestamp = Timestamp(123456789L)

    val ownedPlant = repository.saveToGarden(plant, "orchid-1", timestamp)

    assertEquals("Orchid", ownedPlant.plant.name)
    assertEquals("Orchidaceae", ownedPlant.plant.latinName)
    assertEquals("An elegant flowering plant", ownedPlant.plant.description)
    assertEquals(PlantHealthStatus.HEALTHY, ownedPlant.plant.healthStatus)
    assertEquals("Perfect condition", ownedPlant.plant.healthStatusDescription)
    assertEquals(10, ownedPlant.plant.wateringFrequency)
    assertEquals(Timestamp(123456789L), ownedPlant.lastWatered)
  }

  @Test
  fun getAllOwnedPlants_returnsEmptyListWhenNoPlants() = runTest {
    val ownedPlants = repository.getAllOwnedPlants()

    assertTrue(ownedPlants.isEmpty())
  }

  @Test
  fun getAllOwnedPlants_returnsCorrectPlantsAfterSaving() = runTest {
    val plant1 = createTestPlant(name = "Rose", latinName = "Rosa")
    val plant2 = createTestPlant(name = "Tulip", latinName = "Tulipa")
    // Use recent timestamp to get HEALTHY status after calculation
    val timestamp = Timestamp(System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000))

    repository.saveToGarden(plant1, "rose-1", timestamp)
    repository.saveToGarden(plant2, "tulip-1", timestamp)

    val ownedPlants = repository.getAllOwnedPlants()

    assertEquals(2, ownedPlants.size)
    assertEquals("rose-1", ownedPlants[0].id)
    assertEquals("tulip-1", ownedPlants[1].id)
    assertEquals("Rose", ownedPlants[0].plant.name)
    assertEquals("Tulip", ownedPlants[1].plant.name)
  }

  @Test
  fun getAllOwnedPlants_returnsCorrectOrderOfInsertion() = runTest {
    val plant1 = createTestPlant(name = "First")
    val plant2 = createTestPlant(name = "Second")
    val plant3 = createTestPlant(name = "Third")
    val timestamp = Timestamp(System.currentTimeMillis())

    repository.saveToGarden(plant1, "id-1", timestamp)
    repository.saveToGarden(plant2, "id-2", timestamp)
    repository.saveToGarden(plant3, "id-3", timestamp)

    val ownedPlants = repository.getAllOwnedPlants()

    assertEquals(3, ownedPlants.size)
    assertEquals("First", ownedPlants[0].plant.name)
    assertEquals("Second", ownedPlants[1].plant.name)
    assertEquals("Third", ownedPlants[2].plant.name)
  }

  @Test
  fun getAllOwnedPlants_updatesAfterDeletion() = runTest {
    val plant1 = createTestPlant(name = "Rose")
    val plant2 = createTestPlant(name = "Tulip")
    val timestamp = Timestamp(System.currentTimeMillis())

    repository.saveToGarden(plant1, "rose-1", timestamp)
    repository.saveToGarden(plant2, "tulip-1", timestamp)
    repository.deleteFromGarden("rose-1")

    val ownedPlants = repository.getAllOwnedPlants()

    assertEquals(1, ownedPlants.size)
    assertEquals("tulip-1", ownedPlants[0].id)
    assertEquals("Tulip", ownedPlants[0].plant.name)
  }

  @Test
  fun deleteFromGarden_throwsExceptionWhenIdNotFound() = runTest {
    try {
      repository.deleteFromGarden("non-existent-id")
      fail("Expected IllegalArgumentException to be thrown")
    } catch (exception: IllegalArgumentException) {
      assertTrue(
          exception.message?.contains("OwnedPlant with id non-existent-id not found") == true)
    }
  }

  @Test
  fun getOwnedPlant_returnsCorrectPlant() = runTest {
    val plant1 = createTestPlant(name = "Rose", latinName = "Rosa")
    val plant2 = createTestPlant(name = "Tulip", latinName = "Tulipa")
    val timestamp = Timestamp(System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000))

    repository.saveToGarden(plant1, "rose-1", timestamp)
    repository.saveToGarden(plant2, "tulip-1", timestamp)

    val retrievedPlant = repository.getOwnedPlant("rose-1")

    assertEquals("rose-1", retrievedPlant.id)
    assertEquals(timestamp, retrievedPlant.lastWatered)
    assertEquals("Rose", retrievedPlant.plant.name)
    assertEquals("Rosa", retrievedPlant.plant.latinName)
  }

  @Test
  fun getOwnedPlant_throwsExceptionWhenIdNotFound() = runTest {
    try {
      repository.getOwnedPlant("non-existent-id")
      fail("Expected IllegalArgumentException to be thrown")
    } catch (exception: IllegalArgumentException) {
      assertTrue(
          exception.message?.contains("OwnedPlant with id non-existent-id not found") == true)
    }
  }

  @Test
  fun getOwnedPlant_returnsCorrectPlantAfterEdit() = runTest {
    val originalPlant = createTestPlant(name = "Original Rose", latinName = "Rosa Original")
    val originalTimestamp = Timestamp(123456789L)

    repository.saveToGarden(originalPlant, "rose-1", originalTimestamp)

    val updatedPlant = createTestPlant(name = "Updated Rose", latinName = "Rosa Updated")
    val newTimestamp = Timestamp(987654321L)
    val newOwnedPlant = OwnedPlant("rose-1", updatedPlant, newTimestamp)

    repository.editOwnedPlant("rose-1", newOwnedPlant)

    val retrievedPlant = repository.getOwnedPlant("rose-1")

    assertEquals("rose-1", retrievedPlant.id)
    assertEquals("Updated Rose", retrievedPlant.plant.name)
    assertEquals("Rosa Updated", retrievedPlant.plant.latinName)
    assertEquals(newTimestamp, retrievedPlant.lastWatered)
  }

  @Test
  fun editOwnedPlant_updatesExistingPlant() = runTest {
    val originalPlant = createTestPlant(name = "Rose", latinName = "Rosa")
    val originalTimestamp = Timestamp(123456789L)
    val newTimestamp = Timestamp(987654321L)

    repository.saveToGarden(originalPlant, "rose-1", originalTimestamp)

    val updatedPlant = createTestPlant(name = "Updated Rose", latinName = "Rosa Updated")
    val newOwnedPlant = OwnedPlant("rose-1", updatedPlant, newTimestamp)

    repository.editOwnedPlant("rose-1", newOwnedPlant)

    val retrievedPlant = repository.getOwnedPlant("rose-1")
    assertEquals("Updated Rose", retrievedPlant.plant.name)
    assertEquals("Rosa Updated", retrievedPlant.plant.latinName)
    assertEquals(newTimestamp, retrievedPlant.lastWatered)
  }

  @Test
  fun editOwnedPlant_throwsExceptionWhenIdNotFound() = runTest {
    val plant = createTestPlant()
    val ownedPlant = OwnedPlant("non-existent", plant, Timestamp(System.currentTimeMillis()))

    try {
      repository.editOwnedPlant("non-existent", ownedPlant)
      fail("Expected IllegalArgumentException to be thrown")
    } catch (exception: IllegalArgumentException) {
      assertTrue(exception.message?.contains("OwnedPlant with id non-existent not found") == true)
    }
  }

  @Test
  fun editOwnedPlant_doesNotAffectOtherPlants() = runTest {
    val plant1 = createTestPlant(name = "Rose")
    val plant2 = createTestPlant(name = "Tulip")
    val timestamp = Timestamp(System.currentTimeMillis())

    repository.saveToGarden(plant1, "rose-1", timestamp)
    repository.saveToGarden(plant2, "tulip-1", timestamp)

    val updatedPlant = createTestPlant(name = "Updated Rose")
    val newOwnedPlant = OwnedPlant("rose-1", updatedPlant, Timestamp(999999999L))

    repository.editOwnedPlant("rose-1", newOwnedPlant)

    // Verify rose was updated
    val updatedRose = repository.getOwnedPlant("rose-1")
    assertEquals("Updated Rose", updatedRose.plant.name)
    assertEquals(Timestamp(999999999L), updatedRose.lastWatered)

    // Verify tulip was not affected
    val unchangedTulip = repository.getOwnedPlant("tulip-1")
    assertEquals("Tulip", unchangedTulip.plant.name)
    assertEquals(timestamp, unchangedTulip.lastWatered)
  }

  @Test
  fun editOwnedPlant_canUpdateOnlyLastWatered() = runTest {
    val originalPlant = createTestPlant(name = "Cactus", latinName = "Cactaceae")
    val originalTimestamp = Timestamp(123456789L)
    val newTimestamp = Timestamp(987654321L)

    repository.saveToGarden(originalPlant, "cactus-1", originalTimestamp)

    // Update only the lastWatered timestamp, keep same plant data
    val updatedOwnedPlant = OwnedPlant("cactus-1", originalPlant, newTimestamp)
    repository.editOwnedPlant("cactus-1", updatedOwnedPlant)

    val retrievedPlant = repository.getOwnedPlant("cactus-1")
    assertEquals("Cactus", retrievedPlant.plant.name)
    assertEquals("Cactaceae", retrievedPlant.plant.latinName)
    assertEquals(newTimestamp, retrievedPlant.lastWatered)
  }

  @Test
  fun editOwnedPlant_canUpdateCompletelyDifferentPlant() = runTest {
    val originalPlant =
        createTestPlant(name = "Rose", latinName = "Rosa", healthStatus = PlantHealthStatus.HEALTHY)
    val completelDifferentPlant =
        createTestPlant(
            name = "Cactus", latinName = "Cactaceae", healthStatus = PlantHealthStatus.NEEDS_WATER)
    // Use recent timestamp to get HEALTHY status after calculation
    val timestamp = Timestamp(System.currentTimeMillis() - (3L * 24 * 60 * 60 * 1000))

    repository.saveToGarden(originalPlant, "plant-1", timestamp)

    val newOwnedPlant = OwnedPlant("plant-1", completelDifferentPlant, timestamp)
    repository.editOwnedPlant("plant-1", newOwnedPlant)

    val retrievedPlant = repository.getOwnedPlant("plant-1")
    assertEquals("Cactus", retrievedPlant.plant.name)
    assertEquals("Cactaceae", retrievedPlant.plant.latinName)
    // Note: health status will be recalculated based on lastWatered, not stored value
  }

  @Test
  fun editOwnedPlant_throwsExceptionWhenIdMismatch() = runTest {
    val originalPlant = createTestPlant(name = "Rose")
    val timestamp = Timestamp(System.currentTimeMillis())

    repository.saveToGarden(originalPlant, "rose-1", timestamp)

    val updatedPlant = createTestPlant(name = "Updated Rose")
    // Create OwnedPlant with different ID than the parameter
    val newOwnedPlant = OwnedPlant("different-id", updatedPlant, timestamp)

    try {
      repository.editOwnedPlant("rose-1", newOwnedPlant)
      fail("Expected IllegalArgumentException to be thrown")
    } catch (exception: IllegalArgumentException) {
      assertTrue(exception.message?.contains("ID mismatch") == true)
      assertTrue(exception.message?.contains("rose-1") == true)
      assertTrue(exception.message?.contains("different-id") == true)
    }
  }

  @Test
  fun getAllOwnedPlantsByUserId_returnsSameAsGetAllOwnedPlants() = runTest {
    val plant1 = createTestPlant(name = "Rose")
    val plant2 = createTestPlant(name = "Tulip")
    val timestamp = Timestamp(System.currentTimeMillis())

    repository.saveToGarden(plant1, "rose-1", timestamp)
    repository.saveToGarden(plant2, "tulip-1", timestamp)

    val all = repository.getAllOwnedPlants()
    val byUser = repository.getAllOwnedPlantsByUserId("some-user-id")

    assertEquals(all, byUser)
  }
}
