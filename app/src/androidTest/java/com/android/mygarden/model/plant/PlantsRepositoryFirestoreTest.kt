package com.android.mygarden.model.plant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.android.mygarden.R
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.AchievementsRepositoryFirestore
import com.android.mygarden.ui.camera.LocalImageDisplay
import com.android.mygarden.utils.FirestoreProfileTest
import com.android.mygarden.utils.TestPlants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlantsRepositoryFirestoreTest : FirestoreProfileTest() {

  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var repository: PlantsRepository
  private lateinit var repositoryAchievements: AchievementsRepository
  private val healthCalculator = PlantHealthCalculator()

  /**
   * Starts up everything needed for Firestore (handled by FirestoreProfileTest) & sets up the
   * repository
   */
  @Before
  fun setup() = runTest {
    // Start up Firebase emulator, clear data, etc. (handled by FirestoreProfileTest)
    super.setUp()

    // Inject repositories
    repositoryAchievements = AchievementsRepositoryFirestore(db, auth)
    repository = PlantsRepositoryFirestore(db, auth, achievementsRepo = repositoryAchievements)
  }

  /** Ensures to clear the repo at the end of each test for consistency */
  @After
  fun eraseFromRepo() {
    runTest { repository.getAllOwnedPlants().forEach { p -> repository.deleteFromGarden(p.id) } }
  }

  /*------------------------ HELPER FUNCTIONS -------------------------*/

  // Helper function to create a test plant without needing a real Image

  /**
   * Saves a plant in the repo that will have its status become NEEDS_WATER if it's recalculated at
   * least 5 seconds after the call to this function
   */
  private fun saveAlmostThirstyPlantInRepo() {
    val justMoreThanADay =
        Timestamp(
            System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(1) - TimeUnit.SECONDS.toMillis(5)))
    runTest { repository.saveToGarden(almostThirstyPlant, repository.getNewId(), justMoreThanADay) }
  }

  /** Saves a HEALTHY plant in the repo and returns its id */
  private fun saveHealthyPlantAndReturnId(): String {
    val id = repository.getNewId()
    runTest { repository.saveToGarden(healthyPlant, id, Timestamp(System.currentTimeMillis())) }
    return id
  }

  /** Suspend version: Saves a plant and returns its id (for use inside runTest blocks) */
  private suspend fun savePlantSuspend(plant: Plant = healthyPlant): String {
    val id = repository.getNewId()
    repository.saveToGarden(plant, id, Timestamp(System.currentTimeMillis()))
    return id
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

    // Handle the transition to and from HEALTHY/SLIGHTLY_DRY for the healthy streak achievement
    val isNowHealthy =
        calculatedStatus == PlantHealthStatus.HEALTHY ||
            calculatedStatus == PlantHealthStatus.SLIGHTLY_DRY
    val wasHealthy =
        ownedPlant.plant.healthStatus == PlantHealthStatus.HEALTHY ||
            ownedPlant.plant.healthStatus == PlantHealthStatus.SLIGHTLY_DRY

    // Set the healthySince if the plant goes from HEALTHY/SLIGHTLY_DRY to another status
    val newHealthySince =
        when {
          !wasHealthy && isNowHealthy -> Timestamp(System.currentTimeMillis())
          wasHealthy && !isNowHealthy -> null
          else -> ownedPlant.healthySince
        }

    return ownedPlant.copy(plant = updatedPlant, healthySince = newHealthySince)
  }

  /*-------------------------- FICTIONAL PLANTS -------------------*/

  private val plant1 = TestPlants.plant1

  private val plant2 = TestPlants.plant2

  private val plant3 = TestPlants.plant3

  private val plant4 = TestPlants.plant4

  private val healthyPlant = TestPlants.healthyPlant
  private val almostThirstyPlant = TestPlants.almostThirstyPlant

  /*---------------------- REPOSITORY TESTS --------------------*/

  @Test
  fun getNewId_GeneratesUniqueIds() = runTest {
    val id1 = repository.getNewId()
    val id2 = repository.getNewId()

    assertNotEquals(id1, id2)
  }

  @Test
  fun saveToGarden_returnsOwnedPlantWithCorrectData() = runTest {
    val id = "test-id-1"
    val timestamp = Timestamp(System.currentTimeMillis())

    val ownedPlant = repository.saveToGarden(plant1, id, timestamp)

    assertEquals(id, ownedPlant.id)
    assertEquals(plant1, ownedPlant.plant)
    assertEquals(timestamp, ownedPlant.lastWatered)
    // Assert that the plant has a valid date of creation
    assertTrue(ownedPlant.dateOfCreation < Timestamp(System.currentTimeMillis()))
  }

  @Test
  fun getOwnedPlant_returnsTheSamePlantSavedToGarden1() = runTest {
    val id = "test getOwned id 1"
    val timestamp = Timestamp(System.currentTimeMillis())

    val ownedPlantBefore = repository.saveToGarden(plant2, id, timestamp)

    val ownedPlantFromRepo = repository.getOwnedPlant(id)

    assertEquals(updatePlantHealthStatus(ownedPlantBefore), ownedPlantFromRepo)
  }

  @Test
  fun getOwnedPlant_returnsTheSamePlantSavedToGarden2() = runTest {
    val plant =
        TestPlants.createTestPlant(
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
  fun getOwnedPlant_returnsTheSamePlantSavedToGarden3() = runTest {
    val plant =
        TestPlants.createTestPlant(
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
  fun getAllOwnedPlant_withNoPlantReturnsEmptyList() = runTest {
    val emptyListOwnedPlant: List<OwnedPlant> = repository.getAllOwnedPlants()
    assertEquals(emptyList<OwnedPlant>(), emptyListOwnedPlant)
    assertEquals(listOf<OwnedPlant>(), emptyListOwnedPlant)
  }

  @Test
  fun getAllOwnedPlant_returnsTheSamePlantsSavedToGarden1() = runTest {
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

    // Remove healthySince Timestamps that can differ due to update timing difference
    val allOwnedPlantBefore: List<OwnedPlant> =
        listOf(ownedPlantBefore1, ownedPlantBefore2, ownedPlantBefore3, ownedPlantBefore4).map {
            ownedP ->
          updatePlantHealthStatus(ownedP).copy(healthySince = null)
        }

    val allOwnedPlantFromRepo: List<OwnedPlant> = repository.getAllOwnedPlants()

    assertEquals(allOwnedPlantBefore, allOwnedPlantFromRepo.map { it.copy(healthySince = null) })
  }

  @Test
  fun deleteFromGarden_works() = runTest {
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
          updatePlantHealthStatus(ownedP).copy(healthySince = null)
        }

    val allOwnedPlantFromRepo: List<OwnedPlant> =
        repository.getAllOwnedPlants().map { it.copy(healthySince = null) }

    assertEquals(allOwnedPlantBefore, allOwnedPlantFromRepo)
    repository.deleteFromGarden(id2)
    val newListBefore =
        allOwnedPlantBefore - updatePlantHealthStatus(ownedPlantBefore2).copy(healthySince = null)
    assertEquals(3, newListBefore.size)
    assertEquals(newListBefore, repository.getAllOwnedPlants().map { it.copy(healthySince = null) })
  }

  @Test
  fun deleteFromGarden_throwsExceptionWhenWrongId() = runTest {
    val idTest = "delete empty repo"
    try {
      repository.deleteFromGarden(idTest)
      fail("Expected IllegalArgumentException to be thrown")
    } catch (e: Exception) {
      assertTrue(e.message?.contains("OwnedPlant with id $idTest not found") == true)
    }
  }

  @Test
  fun editOwnedPlant_works() = runTest {
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
          updatePlantHealthStatus(ownedP).copy(healthySince = null)
        }
    val allOwnedPlantFromRepo = repository.getAllOwnedPlants()
    assertEquals(allOwnedPlantEdit, allOwnedPlantFromRepo.map { it.copy(healthySince = null) })
  }

  @Test
  fun waterPlant_works() = runTest {
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

  /*--------------------- REPOSITORY CLOUD STORAGE TESTS -----------------*/
  /**
   * Tests that the image stored locally is stored in Cloud Storage when the plant is saved to
   * garden and that the image can be retrieved from Cloud Storage.
   */
  @Test
  fun loadLocalImage_andThenSaveToGardenWithLogoImageWorks() = runTest {
    // Give the Android context
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Take the image app_loo for the test
    val inputStream = context.resources.openRawResource(R.drawable.app_logo_for_test)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    // Compress and save this image in context.filesDir
    val file = File(context.filesDir, "test_app_logo.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

    val id = "test-id-1"
    val timestamp = Timestamp(System.currentTimeMillis())
    val plantTestCloud = plant1.copy(image = file.absolutePath)

    val ownedPlant = repository.saveToGarden(plantTestCloud, id, timestamp)
    // Display the image with the AsyncImage version
    composeTestRule.setContent {
      LocalImageDisplay(
          imagePath = ownedPlant.plant.image!!,
          testVersionRemeberAsync = false,
          contentDescription = "Plant image")
    }

    // Check that the image is displayed
    composeTestRule.onNodeWithContentDescription("Plant image").assertExists()
  }

  /** Tests that the image stored in Cloud Storage can be deleted from it. */
  @Test
  fun loadLocalImage_andThenSaveToGardenWithLogoImageThenDeleteWorks() = runTest {
    // Give the Android context
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Take the image app_loo for the test
    val inputStream = context.resources.openRawResource(R.drawable.app_logo_for_test)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    // Compress and save this image in context.filesDir
    val file = File(context.filesDir, "test_app_logo.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

    val id = "test-id-1"
    val timestamp = Timestamp(System.currentTimeMillis())
    val plantTestCloud = plant1.copy(image = file.absolutePath)

    val ownedPlant = repository.saveToGarden(plantTestCloud, id, timestamp)
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference.child("users/${auth.currentUser!!.uid}/plants/$id.jpg")

    val downloadUrl = storageRef.downloadUrl.await()
    assertNotNull(downloadUrl)

    // Delete the plant from the garden, it also deletes the image from Cloud Storage
    repository.deleteFromGarden(id)

    // Check that it works
    try {
      storageRef.downloadUrl.await()
      fail("Expected StorageException for deleted image, but succeeded")
    } catch (e: StorageException) {
      assertEquals(StorageException.ERROR_OBJECT_NOT_FOUND, e.errorCode)
    }
  }

  /*--------------------- REPOSITORY FLOW TESTS -----------------*/

  /** Tests that the repo's flow emits something when a plant is saved on the repo */
  @Test
  fun flowEmissionWhenSavingPlant() = runTest {
    repository.plantsFlow.test {
      // initial emission from stateIn
      assertEquals(emptyList<OwnedPlant>(), awaitItem())

      val id = savePlantSuspend()

      val list = awaitItem()
      assertEquals(1, list.size)
      assertEquals(id, list.first().id)

      cancelAndIgnoreRemainingEvents()
    }
  }

  /** Tests that the flow emits when a plant is changed on the repo */
  @Test
  fun flowEmissionWhenChangingAPlant() = runTest {
    val id = repository.getNewId()
    val now = Timestamp(System.currentTimeMillis())
    val yesterday =
        Timestamp(
            System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1) + TimeUnit.MINUTES.toMillis(3))
    val newPlant = OwnedPlant(id, healthyPlant, lastWatered = yesterday)

    repository.plantsFlow.test {
      // initial emission from stateIn
      assertEquals(emptyList<OwnedPlant>(), awaitItem())

      repository.saveToGarden(healthyPlant, id, now)

      val list = awaitItem()
      assertEquals(1, list.size)
      assertEquals(id, list.first().id)

      repository.editOwnedPlant(id, newPlant)
      val list2 = awaitItem()
      assertNotEquals(list, list2)
      assertEquals(1, list2.size)
      assertEquals(list.first().id, list2.first().id)

      cancelAndIgnoreRemainingEvents()
    }
  }

  /**
   * Tests that the flow doesn't emit when an update is made but this update doesn't change the list
   */
  @Test
  fun noFlowEmissionWhenEditPlantDoesntChangeIt() = runTest {
    val id = repository.getNewId()
    val randomDate = Timestamp(1737273600) // 19th of January 2025 at 9am

    repository.plantsFlow.test {
      // initial emission from stateIn
      assertEquals(emptyList<OwnedPlant>(), awaitItem())

      repository.saveToGarden(healthyPlant, id, randomDate)
      awaitItem()

      val plant = repository.getOwnedPlant(id)

      // edit the owned plant with the same values should not emit
      repository.editOwnedPlant(id, plant)
      expectNoEvents()

      cancelAndIgnoreRemainingEvents()
    }
  }

  /**
   * Tests that the flow doesn't emit when a trigger is called but the list is unchanged compared to
   * the previous emitted list
   */
  @Test
  fun noFlowEmissionWhenNoChangeOnList() = runTest {
    repository.plantsFlow.test {
      // initial emission from stateIn
      assertEquals(emptyList<OwnedPlant>(), awaitItem())

      savePlantSuspend()
      awaitItem()

      repository.getAllOwnedPlants()
      expectNoEvents()

      cancelAndIgnoreRemainingEvents()
    }
  }

  /** Tests that the flow emits when a plant status has changed between 2 trigger calls */
  @Test
  fun flowEmissionWhenRetrievingAPlantThatChangedStatus() = runTest {
    repository.plantsFlow.test {
      // initial emission from stateIn
      assertEquals(emptyList<OwnedPlant>(), awaitItem())

      saveAlmostThirstyPlantInRepo()
      val stillHealthy = awaitItem()
      assertEquals(PlantHealthStatus.SLIGHTLY_DRY, stillHealthy.first().plant.healthStatus)

      // This should make the plant status change to NEEDS_WATER
      Thread.sleep(7000)

      repository.getAllOwnedPlants()
      val nowThirsty = awaitItem()
      assertNotEquals(stillHealthy, nowThirsty)
      assertEquals(PlantHealthStatus.NEEDS_WATER, nowThirsty.first().plant.healthStatus)

      cancelAndIgnoreRemainingEvents()
    }
  }

  /*--------------------- CLEANUP TESTS -----------------*/

  /** Tests that cleanup() can be called without throwing exceptions. */
  @Test
  fun cleanup_doesNotThrowException() = runTest {
    // Save a plant
    savePlantSuspend()

    // Call cleanup - should not throw
    repository.cleanup()
  }

  /** Tests that cleanup() can be called multiple times safely. */
  @Test
  fun cleanup_canBeCalledMultipleTimes() = runTest {
    // Save a plant
    savePlantSuspend()

    // Call cleanup multiple times - should not throw exceptions
    repository.cleanup()
    repository.cleanup()
    repository.cleanup()

    // Verify the repository is still functional after multiple cleanups
    val plants = repository.getAllOwnedPlants()
    assertEquals(1, plants.size)
  }

  /** Tests that after cleanup(), basic repository operations still work. */
  @Test
  fun cleanup_repositoryStillFunctional() = runTest {
    // Save a plant
    val id = savePlantSuspend()

    // Call cleanup
    repository.cleanup()

    // Verify we can still get the plant
    val plant = repository.getOwnedPlant(id)
    assertNotNull(plant)
    assertEquals(id, plant.id)

    // Verify we can still get all plants
    val allPlants = repository.getAllOwnedPlants()
    assertEquals(1, allPlants.size)

    // Verify we can still save a new plant
    savePlantSuspend()
    val updatedList = repository.getAllOwnedPlants()
    assertEquals(2, updatedList.size)
  }

  /** Tests that cleanup() doesn't interfere with existing data in Firestore. */
  @Test
  fun cleanup_doesNotDeleteData() = runTest {
    // Save multiple plants
    savePlantSuspend(healthyPlant)
    savePlantSuspend(plant2)
    savePlantSuspend(plant3)

    // Verify we have 3 plants
    val beforeCleanup = repository.getAllOwnedPlants()
    assertEquals(3, beforeCleanup.size)

    // Call cleanup
    repository.cleanup()

    // Verify all plants are still there
    val afterCleanup = repository.getAllOwnedPlants()
    assertEquals(3, afterCleanup.size)
  }
}
