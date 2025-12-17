package com.android.mygarden.ui.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.gardenactivity.activityclasses.GardenActivity
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeFriendsRepository
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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
      ActivityAddFriend(
          "uid1", "gregory", Timestamp(System.currentTimeMillis()), "uid2", "friendPseudo")

  val wateredPlantActivity =
      ActivityWaterPlant("uid1", "gregory", Timestamp(System.currentTimeMillis()), ownedPlant1)

  val gotAchievementActivity =
      ActivityAchievement(
          "uid1", "gregory", Timestamp(System.currentTimeMillis()), AchievementType.PLANTS_NUMBER)

  /*------------------- FAKE ACTIVITY REPOSITORY -----------------*/
  private class FakeActivityRepository : ActivityRepository {

    val activitiesFlow = MutableStateFlow<List<GardenActivity>>(emptyList())

    override fun getCurrentUserId(): String = "fake-uid"

    override fun getActivities(): Flow<List<GardenActivity>> = activitiesFlow

    override fun getActivitiesForUser(userId: String): Flow<List<GardenActivity>> {
      return emptyFlow()
    }

    override fun getFeedActivities(userIds: List<String>, limit: Int): Flow<List<GardenActivity>> {
      return activitiesFlow
    }

    override suspend fun addActivity(activity: GardenActivity) {
      val currentFlowValue = activitiesFlow.value
      activitiesFlow.value = currentFlowValue + activity
    }

    override suspend fun deletePlantActivityForPlant(plantId: String) {}

    override fun cleanup() {
      activitiesFlow.value = emptyList()
    }
  }

  private lateinit var activityRepo: ActivityRepository
  private lateinit var friendsRepo: FriendsRepository
  private lateinit var friendsRequestsRepo: FriendRequestsRepository

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
    FriendsRepositoryProvider.repository = FakeFriendsRepository()
    FriendRequestsRepositoryProvider.repository = FakeFriendRequestsRepository()
    activityRepo = ActivityRepositoryProvider.repository
    friendsRepo = FriendsRepositoryProvider.repository
    friendsRequestsRepo = FriendRequestsRepositoryProvider.repository
    composeRule.setContent { MyGardenTheme { FeedScreen() } }
  }

  /** Ensures the list of activities is cleared between each test */
  @After fun cleanList() = runTest { activityRepo.cleanup() }

  /** Tests the correct display when no activity is found */
  @Test
  fun noActivitiesCorrectDisplay() {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.FRIENDS_REQUESTS_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.NO_ACTIVITY_MESSAGE).assertIsDisplayed()
  }

  /** Tests the correct display when an added plant activity is added to the activity repo */
  @Test
  fun addedPlantActivityCorrectDisplay() = runTest {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.FRIENDS_REQUESTS_BUTTON).assertIsDisplayed()
    activityRepo.addActivity(addedPlantActivity)
    composeRule.allActivitiesAreDisplayed(listOf(addedPlantActivity))
    composeRule.onNodeWithTag(FeedScreenTestTags.GENERIC_CARD_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.GENERIC_CARD_ICON).assertIsDisplayed()
  }

  /** Tests the correct display when an added friend activity is added to the activity repo */
  @Test
  fun addedFriendActivityCorrectDisplay() = runTest {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.FRIENDS_REQUESTS_BUTTON).assertIsDisplayed()
    activityRepo.addActivity(addedFriendActivity)
    composeRule.allActivitiesAreDisplayed(listOf(addedFriendActivity))
    composeRule.onNodeWithTag(FeedScreenTestTags.GENERIC_CARD_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.GENERIC_CARD_ICON).assertIsDisplayed()
  }

  /** Tests the correct display when a watered plant activity is added to the activity repo */
  @Test
  fun wateredPlantActivityCorrectDisplay() = runTest {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.FRIENDS_REQUESTS_BUTTON).assertIsDisplayed()
    activityRepo.addActivity(wateredPlantActivity)
    composeRule.allActivitiesAreDisplayed(listOf(wateredPlantActivity))
    composeRule.onNodeWithTag(FeedScreenTestTags.GENERIC_CARD_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.GENERIC_CARD_ICON).assertIsDisplayed()
  }

  /** Tests the correct display when a got achievement activity is added to the activity repo */
  @Test
  fun gotAchievementActivityCorrectDisplay() = runTest {
    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.FRIENDS_REQUESTS_BUTTON).assertIsDisplayed()
    activityRepo.addActivity(gotAchievementActivity)
    composeRule.allActivitiesAreDisplayed(listOf(gotAchievementActivity))
    composeRule.onNodeWithTag(FeedScreenTestTags.GENERIC_CARD_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithTag(FeedScreenTestTags.GENERIC_CARD_ICON).assertIsDisplayed()
  }
}
