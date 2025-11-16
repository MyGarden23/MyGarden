package com.android.mygarden.model.gardenactivity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddedPlant
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.utils.FirebaseUtils
import com.google.firebase.Timestamp
import java.sql.Timestamp as SqlTimestamp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GardenActivityTests {

  private lateinit var firebaseUtils: FirebaseUtils
  private val profileRepo: ProfileRepository
    get() = ProfileRepositoryProvider.repository

  @Before
  fun setup() = runTest {
    firebaseUtils = FirebaseUtils()
    firebaseUtils.initialize()
    firebaseUtils.injectProfileRepository()

    // Sign in and wait until auth + Firestore are ready
    firebaseUtils.signIn()
    firebaseUtils.waitForAuthReady()
  }

  private fun createTestOwnedPlant(): OwnedPlant {
    val plant =
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

    return OwnedPlant(
        id = "plant123",
        plant = plant,
        lastWatered = SqlTimestamp(System.currentTimeMillis()),
        previousLastWatered = SqlTimestamp(System.currentTimeMillis() - 86_400_000L))
  }

  @Test
  fun addedPlantActivity_isStoredAndRetrievedForCurrentUser() = runTest {
    val uid = firebaseUtils.auth.currentUser!!.uid

    val activityAddedPlantActivity =
        ActivityAddedPlant(
            userId = uid,
            pseudo = "TestUser",
            timestamp = Timestamp.now(),
            ownedPlant = createTestOwnedPlant())

    // Add activity
    profileRepo.addActivity(activityAddedPlantActivity)

    // Read back via getActivities() (current user)
    val activities = profileRepo.getActivities().first()
    assertTrue(activities.isNotEmpty())

    val first = activities.first() as ActivityAddedPlant
    assertEquals(uid, first.userId)
    assertEquals("TestUser", first.pseudo)
    assertEquals(ActivityType.ADDED_PLANT, first.type)
    assertEquals("plant123", first.ownedPlant.id)
    assertEquals("Test Rose", first.ownedPlant.plant.name)
    assertEquals("Rosa test", first.ownedPlant.plant.latinName)
  }

  @Test
  fun achievementActivity_isStoredAndRetrievedForSpecificUser() = runTest {
    val uid = firebaseUtils.auth.currentUser!!.uid

    val activityAchievement =
        ActivityAchievement(userId = uid, pseudo = "Achiever", timestamp = Timestamp.now())

    // Add activity
    profileRepo.addActivity(activityAchievement)

    // Read back via getActivitiesForUser(uid)
    val activities = profileRepo.getActivitiesForUser(uid).first()
    assertTrue(activities.isNotEmpty())

    val first = activities.first() as ActivityAchievement
    assertEquals(uid, first.userId)
    assertEquals("Achiever", first.pseudo)
    assertEquals(ActivityType.ACHIEVEMENT, first.type)
  }

  @Test
  fun feedActivities_mergesActivitiesFromSingleUser() = runTest {
    val uid = firebaseUtils.auth.currentUser!!.uid

    val activity1 =
        ActivityAchievement(userId = uid, pseudo = "UserFeed", timestamp = Timestamp.now())

    val activity2 =
        ActivityAddedPlant(
            userId = uid,
            pseudo = "UserFeed",
            timestamp = Timestamp.now(),
            ownedPlant = createTestOwnedPlant())

    profileRepo.addActivity(activity1)
    profileRepo.addActivity(activity2)

    // For now, test feed with a single user id (logic is the same for multiple)
    val feed = profileRepo.getFeedActivities(listOf(uid), limit = 10).first()
    assertTrue(feed.size >= 2)

    val types = feed.map { it.type }.toSet()
    assertTrue(types.contains(ActivityType.ACHIEVEMENT))
    assertTrue(types.contains(ActivityType.ADDED_PLANT))
  }
}
