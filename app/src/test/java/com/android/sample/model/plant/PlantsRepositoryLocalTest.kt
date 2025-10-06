package com.android.sample.model.plant


import java.sql.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

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
            wateringFrequency = 7
        )
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
        val plant2 = createTestPlant(
            name = "Fern",
            latinName = "Pteridium",
            healthStatus = PlantHealthStatus.NEEDS_WATER
        )
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
        val healthStatuses = listOf(
            PlantHealthStatus.HEALTHY,
            PlantHealthStatus.NEEDS_WATER,
            PlantHealthStatus.OVERWATERED,
            PlantHealthStatus.UNKNOWN
        )

        healthStatuses.forEachIndexed { index, status ->
            val plant = createTestPlant(
                name = "Plant $index",
                latinName = "Plantus $index",
                healthStatus = status
            )
            val timestamp = Timestamp(System.currentTimeMillis())

            val ownedPlant = repository.saveToGarden(plant, "id-$index", timestamp)

            assertEquals(status, ownedPlant.plant.healthStatus)
        }
    }

    @Test
    fun saveToGarden_preservesAllPlantProperties() = runTest {
        val plant = Plant(
            name = "Orchid",
            image = null,
            latinName = "Orchidaceae",
            description = "An elegant flowering plant",
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "Perfect condition",
            wateringFrequency = 10
        )
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
}