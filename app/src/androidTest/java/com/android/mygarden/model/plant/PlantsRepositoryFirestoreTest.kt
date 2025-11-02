package com.android.mygarden.model.plant

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.utils.FakeJwtGenerator
import com.android.mygarden.utils.FirebaseEmulator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import java.sql.Timestamp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Assert.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantsRepositoryFirestoreTest {
  private lateinit var repository: PlantsRepository
  private lateinit var db: FirebaseFirestore
  private lateinit var auth: FirebaseAuth
  private val healthCalculator = PlantHealthCalculator()

  @Before
  fun setup() = runBlocking {
    FirebaseEmulator.connectAuth()
    FirebaseEmulator.clearAuthEmulator()
    db = FirebaseEmulator.connectFirestore()
    FirebaseEmulator.clearFirestoreEmulator()
    auth = FirebaseEmulator.auth

    // Fake sign-in (suspend)
    auth.signOut()
    val uniqueEmail = "test.profile+${System.currentTimeMillis()}@example.com"
    val idToken = FakeJwtGenerator.createFakeGoogleIdToken(email = uniqueEmail)
    val cred = GoogleAuthProvider.getCredential(idToken, null)
    val result = auth.signInWithCredential(cred).await()
    val uid = result.user?.uid
    assertNotNull(uid)

    // Clean user doc (suspend)
    db.collection("users").document(uid!!).delete().await()

    // Inject repo
    PlantsRepositoryProvider.repository = PlantsRepositoryFirestore(db, auth)
    repository = PlantsRepositoryProvider.repository
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

  /** Plants to use for the tests */
  private val plant1 =
      createTestPlant(
          name = "test plant 1",
          latinName = "test in latin plant 1",
          healthStatus = PlantHealthStatus.SEVERELY_OVERWATERED)

  private val plant2 =
      createTestPlant(
          name = "test plant 2",
          latinName = "test in latin plant 2",
          healthStatus = PlantHealthStatus.SEVERELY_DRY)

  private val plant3 =
      createTestPlant(
          name = "test plant 3",
          latinName = "test in latin plant 3",
          healthStatus = PlantHealthStatus.SLIGHTLY_DRY)

  private val plant4 =
      createTestPlant(
          name = "test plant 4",
          latinName = "test in latin plant 4",
          healthStatus = PlantHealthStatus.NEEDS_WATER)

  @Test
  fun getNewId_GeneratesUniqueIds() = runBlocking {
    val id1 = repository.getNewId()
    val id2 = repository.getNewId()

    assertNotEquals(id1, id2)
  }

  @Test
  fun saveToGarden_returnsOwnedPlantWithCorrectData() = runBlocking {
    val id = "test-id-1"
    val timestamp = Timestamp(System.currentTimeMillis())

    val ownedPlant = repository.saveToGarden(plant1, id, timestamp)

    assertEquals(id, ownedPlant.id)
    assertEquals(plant1, ownedPlant.plant)
    assertEquals(timestamp, ownedPlant.lastWatered)
  }

  @Test
  fun getOwnedPlant_returnsTheSamePlantSavedToGarden1() = runBlocking {
    val id = "test getOwned id 1"
    val timestamp = Timestamp(System.currentTimeMillis())

    val ownedPlantBefore = repository.saveToGarden(plant2, id, timestamp)

    val ownedPlantFromRepo = repository.getOwnedPlant(id)

    assertEquals(updatePlantHealthStatus(ownedPlantBefore), ownedPlantFromRepo)
  }

  @Test
  fun getOwnedPlant_returnsTheSamePlantSavedToGarden2() = runBlocking {
    val plant =
        createTestPlant(
            name = "test getOwned 2",
            latinName = "test getOwned 2 in latin",
            healthStatus = PlantHealthStatus.OVERWATERED)
    val id = "test getOwned id 2"
    val timestamp = Timestamp(System.currentTimeMillis())

    val ownedPlantBefore = repository.saveToGarden(plant, id, timestamp)

    val ownedPlantFromRepo = repository.getOwnedPlant(id)

    assertEquals(updatePlantHealthStatus(ownedPlantBefore), ownedPlantFromRepo)
  }

  @Test
  fun getOwnedPlant_returnsTheSamePlantSavedToGarden3() = runBlocking {
    val plant =
        createTestPlant(
            name = "test getOwned 3",
            latinName = "test getOwned 3 in latin",
            healthStatus = PlantHealthStatus.UNKNOWN)
    val id = "test getOwned id 3"
    val timestamp = Timestamp(System.currentTimeMillis())

    val ownedPlantBefore = repository.saveToGarden(plant, id, timestamp)

    val ownedPlantFromRepo = repository.getOwnedPlant(id)

    assertEquals(updatePlantHealthStatus(ownedPlantBefore), ownedPlantFromRepo)
  }

  @Test
  fun getAllOwnedPlant_returnsTheSamePlantsSavedToGarden1() = runBlocking {
    val id1 = "test getAllOwned plant 1"
    val timestamp1 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore1 = repository.saveToGarden(plant1, id1, timestamp1)

    val id2 = "test getAllOwned plant 2"
    val timestamp2 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore2 = repository.saveToGarden(plant2, id2, timestamp2)

    val id3 = "test getAllOwned plant 3"
    val timestamp3 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore3 = repository.saveToGarden(plant3, id3, timestamp3)

    val id4 = "test getAllOwned plant 4"
    val timestamp4 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore4 = repository.saveToGarden(plant4, id4, timestamp4)

    val allOwnedPlantBefore: List<OwnedPlant> =
        listOf(ownedPlantBefore1, ownedPlantBefore2, ownedPlantBefore3, ownedPlantBefore4).map {
            ownedP ->
          updatePlantHealthStatus(ownedP)
        }

    val allOwnedPlantFromRepo: List<OwnedPlant> = repository.getAllOwnedPlants()

    assertEquals(allOwnedPlantBefore, allOwnedPlantFromRepo)
  }

  @Test
  fun deleteFromGarden_works() = runBlocking {
    val id1 = "test deleteFromGarden 1 plant 1"
    val timestamp1 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore1 = repository.saveToGarden(plant1, id1, timestamp1)

    val id2 = "test deleteFromGarden 1 plant 2"
    val timestamp2 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore2 = repository.saveToGarden(plant2, id2, timestamp2)

    val id3 = "test deleteFromGarden 1 plant 3"
    val timestamp3 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore3 = repository.saveToGarden(plant3, id3, timestamp3)

    val id4 = "test deleteFromGarden 1 plant 4"
    val timestamp4 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore4 = repository.saveToGarden(plant4, id4, timestamp4)

    val allOwnedPlantBefore: List<OwnedPlant> =
        listOf(ownedPlantBefore1, ownedPlantBefore2, ownedPlantBefore3, ownedPlantBefore4).map {
            ownedP ->
          updatePlantHealthStatus(ownedP)
        }

    val allOwnedPlantFromRepo: List<OwnedPlant> = repository.getAllOwnedPlants()

    assertEquals(allOwnedPlantBefore, allOwnedPlantFromRepo)
    repository.deleteFromGarden(id2)
    val newListBefore = allOwnedPlantBefore - updatePlantHealthStatus(ownedPlantBefore2)
    assertEquals(3, newListBefore.size)
    assertEquals(newListBefore, repository.getAllOwnedPlants())
  }

  @Test
  fun deleteFromGarden_throwsExceptionWhenWrongId() = runBlocking {
    val idTest = "delete empty repo"
    try {
      repository.deleteFromGarden(idTest)
      fail("Expected IllegalArgumentException to be thrown")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("OwnedPlant with id $idTest not found") == true)
    }
  }

  @Test
  fun editOwnedPlant_works() = runBlocking {
    val id1 = "test editOwnedPlant 1 plant 1"
    val timestamp1 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore1 = repository.saveToGarden(plant1, id1, timestamp1)

    val id2 = "test editOwnedPlant 1 plant 2"
    val timestamp2 = Timestamp(System.currentTimeMillis())
    repository.saveToGarden(plant2, id2, timestamp2)

    val timestamp3 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore3 = OwnedPlant(id2, plant3, timestamp3)

    repository.editOwnedPlant(id2, ownedPlantBefore3)
    val allOwnedPlantEdit: List<OwnedPlant> =
        listOf(ownedPlantBefore1, ownedPlantBefore3).map { ownedP ->
          updatePlantHealthStatus(ownedP)
        }
    val allOwnedPlantFromRepo = repository.getAllOwnedPlants()
    assertEquals(allOwnedPlantEdit, allOwnedPlantFromRepo)
  }

  @Test
  fun waterPlant_works() = runBlocking {
    val id1 = "test waterPlant_works plant 1"
    val timestamp1 = Timestamp(System.currentTimeMillis())
    val ownedPlantBefore1 = repository.saveToGarden(plant1, id1, timestamp1)

    val timestamp2 = Timestamp(System.currentTimeMillis())

    repository.waterPlant(id1, timestamp2)

    val wateredOwnedPlant =
        ownedPlantBefore1.copy(
            lastWatered = timestamp2, previousLastWatered = ownedPlantBefore1.lastWatered)

    val wateredOwnedPlantFromRepo = repository.getOwnedPlant(id1)
    assertEquals(wateredOwnedPlant, wateredOwnedPlantFromRepo)
  }

  /**
   * Updates the health status of a plant based on current watering cycle.
   *
   * @param ownedPlant The plant to update
   * @return A copy of the plant with updated health status
   */
  private fun updatePlantHealthStatus(ownedPlant: OwnedPlant): OwnedPlant {
    val calculatedStatus =
        healthCalculator.calculateHealthStatus(
            lastWatered = ownedPlant.lastWatered,
            wateringFrequency = ownedPlant.plant.wateringFrequency,
            previousLastWatered = ownedPlant.previousLastWatered)
    val updatedPlant = ownedPlant.plant.copy(healthStatus = calculatedStatus)
    return ownedPlant.copy(plant = updatedPlant)
  }
}
