package com.android.mygarden.ui.feed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.android.mygarden.model.caretips.CareTipsRepositoryProvider
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.feed.FeedScreenTests.FakeActivityRepository
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.popup.PopupScreenTestTags
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.*
import java.sql.Timestamp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedNavigationTests {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var navController: NavHostController
  private lateinit var activityRepo: ActivityRepository
  private lateinit var plantRepo: PlantsRepository
  private lateinit var userProfileRepo: FakeUserProfileRepository

  // Test plant data
  private val testPlant =
      Plant(
          name = "Test Plant",
          image = null,
          latinName = "Testus Plantus",
          description = "A test plant for testing",
          location = PlantLocation.INDOOR,
          lightExposure = "Bright indirect light",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "Plant is healthy",
          wateringFrequency = 7)

  private val testOwnedPlant =
      OwnedPlant(
          id = "test-plant-1",
          plant = testPlant,
          lastWatered = Timestamp(System.currentTimeMillis()))

  val addPlantActivity: ActivityAddedPlant =
      ActivityAddedPlant(
          userId = "fake-uid",
          pseudo = "TestUser",
          createdAt = Timestamp(System.currentTimeMillis() - 2000),
          ownedPlant = testOwnedPlant)

  val waterPlantActivity: ActivityWaterPlant =
      ActivityWaterPlant(
          userId = "fake-uid",
          pseudo = "TestUser",
          createdAt = Timestamp(System.currentTimeMillis() - 1000),
          ownedPlant = testOwnedPlant)

  val addFriendActivity: ActivityAddFriend =
      ActivityAddFriend(
          userId = "fake-uid",
          pseudo = "TestUser",
          createdAt = Timestamp(System.currentTimeMillis()),
          friendUserId = "friend-user-id",
          friendPseudo = "FriendUser")

  @Before
  fun setup() {
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller

      activityRepo = FakeActivityRepository()
      ActivityRepositoryProvider.repository = activityRepo

      plantRepo = PlantsRepositoryLocal()
      PlantsRepositoryProvider.repository = plantRepo

      val fakeFriendsRepo = FakeFriendsRepository()
      // Add the friend to the friends list so they show up as FRIEND, not NOT_FRIEND
      runBlocking { fakeFriendsRepo.addFriend("friend-user-id") }
      FriendsRepositoryProvider.repository = fakeFriendsRepo
      FriendRequestsRepositoryProvider.repository = FakeFriendRequestsRepository()

      userProfileRepo = FakeUserProfileRepository()
      // Populate user profiles for the add friend activity
      userProfileRepo.profiles["fake-uid"] =
          UserProfile(
              id = "fake-uid",
              pseudo = "TestUser",
              avatar = Avatar.A1,
              gardeningSkill = "Beginner",
              favoritePlant = "Rose")
      userProfileRepo.profiles["friend-user-id"] =
          UserProfile(
              id = "friend-user-id",
              pseudo = "FriendUser",
              avatar = Avatar.A2,
              gardeningSkill = "Expert",
              favoritePlant = "Tulip")
      UserProfileRepositoryProvider.repository = userProfileRepo

      ProfileRepositoryProvider.repository = FakeProfileRepository()
      CareTipsRepositoryProvider.repository = FakeCareTipsRepository()
      AchievementsRepositoryProvider.repository = FakeAchievementsRepository()

      // initialize activities and plant
      runBlocking {
        plantRepo.saveToGarden(
            plant = testOwnedPlant.plant,
            id = testOwnedPlant.id,
            lastWatered = testOwnedPlant.lastWatered)
        activityRepo.addActivity(addPlantActivity)
        activityRepo.addActivity(waterPlantActivity)
        activityRepo.addActivity(addFriendActivity)
      }

      MyGardenTheme {
        // Start at FeedScreen
        AppNavHost(navController = controller, startDestination = Screen.Feed.route)
      }
    }
  }

  @Test
  fun clickingOnAddedPlantActivity_NavigatesToPlantInfoScreen() = runTest {
    // Wait for the activity to appear in the UI
    composeTestRule.waitForIdle()

    // When: Click on the activity card
    val activityTag = FeedScreenTestTags.getTestTagForActivity(addPlantActivity)
    composeTestRule.onNodeWithTag(activityTag).assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).assertIsDisplayed()
  }

  @Test
  fun clickingOnWaterPlantActivity_NavigatesToPlantInfoScreen() = runTest {
    // Wait for the activity to appear in the UI
    composeTestRule.waitForIdle()

    // When: Click on the activity card
    val activityTag = FeedScreenTestTags.getTestTagForActivity(waterPlantActivity)
    composeTestRule.onNodeWithTag(activityTag).assertIsDisplayed().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).assertIsDisplayed()
  }

  @Test
  fun clickingOnFriendActivity_OpensPopupScreen_AndNavigatesToGardenScreen() = runTest {
    // Wait for the activity to appear in the UI
    composeTestRule.waitForIdle()

    // When: Click on the activity card
    val activityTag = FeedScreenTestTags.getTestTagForActivity(addFriendActivity)
    composeTestRule.onNodeWithTag(activityTag).assertIsDisplayed().performClick()

    // Wait for the popup to load and fetch user profiles
    composeTestRule.waitForIdle()

    // Verify popup is displayed
    composeTestRule.onNodeWithTag(PopupScreenTestTags.CARD).assertIsDisplayed()

    // Click on the friend's button (not the current user's button)
    composeTestRule
        .onNodeWithTag(FriendsPopupTestTags.buttonTestTag(addFriendActivity.friendUserId))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(FriendsPopupTestTags.buttonTestTag(addFriendActivity.friendUserId))
        .performClick()
    composeTestRule.waitForIdle()

    // Should navigate to the friend's garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.INTERNAL_GARDEN_SCREEN).assertIsDisplayed()
  }
}
