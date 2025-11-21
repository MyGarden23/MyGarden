package com.android.mygarden.ui.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityWaterPlant
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import java.sql.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedScreenTests {

  @get:Rule val composeRule = createComposeRule()

  /*---------------- FICTIONAL PLANTS / ACTIVITIES --------------*/
  val plant1 =
      Plant(
          "hello",
          null,
          "laurem ipsum",
          "beautiful plant",
          PlantLocation.INDOOR,
          "Direct light",
          PlantHealthStatus.HEALTHY,
          "is healthy",
          10)

  val ownedPlant1 = OwnedPlant("id1", plant1, Timestamp(System.currentTimeMillis()))

  val addedPlantActivity =
      ActivityAddedPlant("uid", "gregory", Timestamp(System.currentTimeMillis()), ownedPlant1)

  val addedFriendActivity =
      ActivityAddFriend("uid1", "gregory", Timestamp(System.currentTimeMillis()), "uid2")

  val wateredPlantActivity =
      ActivityWaterPlant("uid1", "gregory", Timestamp(System.currentTimeMillis()), ownedPlant1)

  val gotAchievementActivity =
      ActivityAchievement("uid1", "gregory", Timestamp(System.currentTimeMillis()), "achievement")

  /*------------------- FAKE ACTIVITY REPOSITORY -----------------*/
  private class FakeActivityRepository : ActivityRepository {

    val activitiesFlow = MutableStateFlow<List<GardenActivity>>(emptyList())

    override fun getCurrentUserId(): String = "fake-uid"

    override fun getActivities(): Flow<List<GardenActivity>> = activitiesFlow

    override fun getActivitiesForUser(userId: String): Flow<List<GardenActivity>> {
      return emptyFlow()
    }

    override fun getFeedActivities(userIds: List<String>, limit: Int): Flow<List<GardenActivity>> {
      return emptyFlow()
    }

    override suspend fun addActivity(activity: GardenActivity) {
      val currentFlowValue = activitiesFlow.value
      activitiesFlow.value = currentFlowValue + activity
    }

    override fun cleanup() {
      activitiesFlow.value = emptyList()
    }
  }

  private lateinit var activityRepo: ActivityRepository

  /** additional function that checks that all activities from the given list are displayed */
  fun ComposeTestRule.allActivitiesAreDisplayed(activities: List<GardenActivity>) {
    activities.forEach {
      onNodeWithTag(FeedScreenTestTags.getTestTagForActivity(it)).assertIsDisplayed()
    }
  }

  /**
   * Sets up the fake repository as the one of the provider for the view model to use this fake repo
   * Sets up the screen
   */
  @Before
  fun setup() {
    ActivityRepositoryProvider.repository = FakeActivityRepository()
    activityRepo = ActivityRepositoryProvider.repository
    composeRule.setContent { MyGardenTheme { FeedScreen() } }
  }

  /** Ensures the list of activities is cleared between each test */
  @After fun cleanList() = runTest { activityRepo.cleanup() }

  /** Tests the correct display when no activity is found */
  @Test
  fun noActivitiesCorrectDisplay() {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.NO_ACTIVITY_MESSAGE).assertIsDisplayed()
  }

  /** Tests the correct display when an added plant activity is added to the activity repo */
  @Test
  fun addedPlantActivityCorrectDisplay() = runTest {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    activityRepo.addActivity(addedPlantActivity)
    composeRule.allActivitiesAreDisplayed(listOf(addedPlantActivity))
    composeRule.onNodeWithTag(FeedScreenTestTags.ADDED_PLANT_DESCRIPTION).assertIsDisplayed()
  }

  /** Tests the correct display when an added friend activity is added to the activity repo */
  @Test
  fun addedFriendActivityCorrectDisplay() = runTest {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    activityRepo.addActivity(addedFriendActivity)
    composeRule.allActivitiesAreDisplayed(listOf(addedFriendActivity))
    composeRule.onNodeWithTag(FeedScreenTestTags.ADDED_FRIEND_DESCRIPTION).assertIsDisplayed()
  }

  /** Tests the correct display when a watered plant activity is added to the activity repo */
  @Test
  fun wateredPlantActivityCorrectDisplay() = runTest {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    activityRepo.addActivity(wateredPlantActivity)
    composeRule.allActivitiesAreDisplayed(listOf(wateredPlantActivity))
    composeRule.onNodeWithTag(FeedScreenTestTags.WATERED_PLANT_DESCRIPTION).assertIsDisplayed()
  }

  /** Tests the correct display when a got achievement activity is added to the activity repo */
  @Test
  fun gotAchievementActivityCorrectDisplay() = runTest {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    activityRepo.addActivity(gotAchievementActivity)
    composeRule.allActivitiesAreDisplayed(listOf(gotAchievementActivity))
    composeRule.onNodeWithTag(FeedScreenTestTags.GOT_ACHIEVEMENT_DESCRIPTION).assertIsDisplayed()
  }
}
