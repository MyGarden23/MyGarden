package com.android.mygarden.model.gardenactivity

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.utils.FirebaseUtils
import com.android.mygarden.utils.TestPlants
import java.sql.Timestamp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GardenActivityTests {

  private lateinit var firebaseUtils: FirebaseUtils
  private val activityRepo: ActivityRepository
    get() = ActivityRepositoryProvider.repository

  @Before
  fun setup() = runTest {
    firebaseUtils = FirebaseUtils()
    firebaseUtils.initialize()

    // Sign in and wait until auth + Firestore are ready
    firebaseUtils.signIn()
    firebaseUtils.waitForAuthReady()
  }

  private fun createTestOwnedPlant(): OwnedPlant {
    val plant = TestPlants.gardenActivityPlant

    return OwnedPlant(
        id = "plant123",
        plant = plant,
        lastWatered = Timestamp(System.currentTimeMillis()),
        previousLastWatered = Timestamp(System.currentTimeMillis() - 86_400_000L))
  }

  @Test
  fun addedPlantActivity_isStoredAndRetrievedForCurrentUser() = runTest {
    val uid = firebaseUtils.auth.currentUser!!.uid

    val activityAddedPlantActivity =
        ActivityAddedPlant(
            userId = uid,
            pseudo = "TestUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            ownedPlant = createTestOwnedPlant())

    // Add activity
    activityRepo.addActivity(activityAddedPlantActivity)

    // Read back via getActivities() (current user)
    val activities = activityRepo.getActivities().first()
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
        ActivityAchievement(
            userId = uid,
            pseudo = "Achiever",
            createdAt = Timestamp(System.currentTimeMillis()),
            achievementName = "Gained Badge")

    // Add activity
    activityRepo.addActivity(activityAchievement)

    // Read back via getActivitiesForUser(uid)
    val activities = activityRepo.getActivitiesForUser(uid).first()
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
        ActivityAchievement(
            userId = uid,
            pseudo = "UserFeed",
            createdAt = Timestamp(System.currentTimeMillis()),
            achievementName = "Gained Badge")

    val activity2 =
        ActivityAddedPlant(
            userId = uid,
            pseudo = "UserFeed",
            createdAt = Timestamp(System.currentTimeMillis()),
            ownedPlant = createTestOwnedPlant())

    activityRepo.addActivity(activity1)
    activityRepo.addActivity(activity2)

    // For now, test feed with a single user id (logic is the same for multiple)
    val feed = activityRepo.getFeedActivities(listOf(uid), limit = 10).first()
    assertTrue(feed.size >= 2)

    val types = feed.map { it.type }.toSet()
    assertTrue(types.contains(ActivityType.ACHIEVEMENT))
    assertTrue(types.contains(ActivityType.ADDED_PLANT))
  }

  @Test
  fun addFriendActivity_isStoredAndRetrievedForCurrentUser() = runTest {
    val uid = firebaseUtils.auth.currentUser!!.uid

    val activityAddFriend =
        ActivityAddFriend(
            userId = uid,
            pseudo = "FriendUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            friendUserId = "friend123")

    activityRepo.addActivity(activityAddFriend)

    val activities = activityRepo.getActivities().first()
    assertTrue(activities.isNotEmpty())

    val first = activities.first { it is ActivityAddFriend } as ActivityAddFriend
    assertEquals(uid, first.userId)
    assertEquals("FriendUser", first.pseudo)
    assertEquals(ActivityType.ADDED_FRIEND, first.type)
    assertEquals("friend123", first.friendUserId)
  }

  @Test
  fun waterPlantActivity_isStoredAndRetrievedForCurrentUser() = runTest {
    val uid = firebaseUtils.auth.currentUser!!.uid

    val activityWaterPlant =
        ActivityWaterPlant(
            userId = uid,
            pseudo = "WaterUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            ownedPlant = createTestOwnedPlant())

    activityRepo.addActivity(activityWaterPlant)

    val activities = activityRepo.getActivities().first()
    assertTrue(activities.isNotEmpty())

    val first = activities.first { it is ActivityWaterPlant } as ActivityWaterPlant
    assertEquals(uid, first.userId)
    assertEquals("WaterUser", first.pseudo)
    assertEquals(ActivityType.WATERED_PLANT, first.type)
    assertEquals("plant123", first.ownedPlant.id)
    assertEquals("Test Rose", first.ownedPlant.plant.name)
  }

  @Test
  fun feedActivities_mergesAllActivityTypesFromSingleUser() = runTest {
    val uid = firebaseUtils.auth.currentUser!!.uid

    val achievement =
        ActivityAchievement(
            userId = uid,
            pseudo = "FeedUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            achievementName = "Gained Badge")

    val addedPlant =
        ActivityAddedPlant(
            userId = uid,
            pseudo = "FeedUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            ownedPlant = createTestOwnedPlant())

    val addFriend =
        ActivityAddFriend(
            userId = uid,
            pseudo = "FeedUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            friendUserId = "friendFeed123")

    val waterPlant =
        ActivityWaterPlant(
            userId = uid,
            pseudo = "FeedUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            ownedPlant = createTestOwnedPlant())

    activityRepo.addActivity(achievement)
    activityRepo.addActivity(addedPlant)
    activityRepo.addActivity(addFriend)
    activityRepo.addActivity(waterPlant)

    val feed = activityRepo.getFeedActivities(listOf(uid), limit = 10).first()
    assertTrue(feed.size >= 4)

    val types = feed.map { it.type }.toSet()
    assertTrue(types.contains(ActivityType.ACHIEVEMENT))
    assertTrue(types.contains(ActivityType.ADDED_PLANT))
    assertTrue(types.contains(ActivityType.ADDED_FRIEND))
    assertTrue(types.contains(ActivityType.WATERED_PLANT))
  }

  @Test
  fun feedActivities_mergesActivitiesFromMultipleUsers() = runTest {
    val currentUserId = firebaseUtils.auth.currentUser!!.uid
    val otherUserId = "otherUser123"

    val activityCurrentUser =
        ActivityAchievement(
            userId = currentUserId,
            pseudo = "CurrentUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            achievementName = "Current User Badge")

    val activityOtherUser =
        ActivityAddedPlant(
            userId = otherUserId,
            pseudo = "OtherUser",
            createdAt = Timestamp(System.currentTimeMillis()),
            ownedPlant = createTestOwnedPlant())

    // Add activity for current user through the repo
    activityRepo.addActivity(activityCurrentUser)

    val serializedOtherActivity = ActivityMapper.fromActivityToSerializedActivity(activityOtherUser)

    firebaseUtils.db
        .collection("users")
        .document(otherUserId)
        .collection("activities")
        .add(serializedOtherActivity)
        .await()

    val flow = activityRepo.getFeedActivities(listOf(currentUserId, otherUserId), limit = 10)
    // drop the first because the flow emits per user so we have to wait for the activities of the
    // second user to be added
    val feed = flow.drop(1).first()

    assertTrue(feed.isNotEmpty())

    val userIdsInFeed = feed.map { it.userId }.toSet()
    assertTrue(userIdsInFeed.contains(currentUserId))
    assertTrue(userIdsInFeed.contains(otherUserId))
  }
}
