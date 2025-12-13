package com.android.mygarden.ui.navigation.navS9

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.garden.GardenAchievementsParentScreenTestTags
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakePlantRepositoryUtils
import com.android.mygarden.utils.FakeUserProfileRepository
import com.android.mygarden.utils.FirestoreProfileTest
import com.android.mygarden.utils.PlantRepositoryType
import com.android.mygarden.utils.TestPlants
import java.sql.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DEFAULT_WAIT_MS = 5_000L

@RunWith(AndroidJUnit4::class)
class NavigationS9TestsGardenScreenFriend : FirestoreProfileTest() {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private val friendId = "friend-test-user-123"
  private val friendPseudo = "TestFriend"
  private lateinit var originalFriendsRepo: FriendsRepository
  private lateinit var originalUserProfileRepo: UserProfileRepository
  private lateinit var originalPlantsRepo: PlantsRepository

  @Before
  fun setupFriendGardenTest() = runTest {
    super.setUp()

    originalFriendsRepo = FriendsRepositoryProvider.repository
    originalUserProfileRepo = UserProfileRepositoryProvider.repository
    originalPlantsRepo = PlantsRepositoryProvider.repository

    // Use FakePlantRepositoryUtils to mock plant repository with multi-user support
    val fakePlantsRepoUtils = FakePlantRepositoryUtils(PlantRepositoryType.PlantRepoLocal)
    fakePlantsRepoUtils.setUpMockRepo()

    // Add two plants owned by the friend
    val ts = Timestamp(System.currentTimeMillis())
    fakePlantsRepoUtils.addPlantForUser(friendId, TestPlants.plant1, "friend-plant-1", ts)
    fakePlantsRepoUtils.addPlantForUser(friendId, TestPlants.plant2, "friend-plant-2", ts)

    // Mock getAllOwnedPlantsByUserId to return plants we added
    fakePlantsRepoUtils.mockGetAllOwnedPlantsByUserId()

    // Set a fake user profile repo so GardenViewModel can load the friend's profile
    val fakeUserProfiles = FakeUserProfileRepository()
    fakeUserProfiles.profiles[friendId] =
        com.android.mygarden.model.users.UserProfile(
            friendId, friendPseudo, Avatar.A2, "INTERMEDIATE", "Rose")
    UserProfileRepositoryProvider.repository = fakeUserProfiles
  }

  @After
  fun tearDownProviders() {
    // Restore original providers to avoid side effects on other tests
    FriendsRepositoryProvider.repository = originalFriendsRepo
    UserProfileRepositoryProvider.repository = originalUserProfileRepo
    PlantsRepositoryProvider.repository = originalPlantsRepo
  }

  @Test
  fun friendGarden_displaysCorrectly() = runTest {
    var currentRoute: String? = null

    compose.setContent {
      val navController = rememberNavController()
      val backEntry by navController.currentBackStackEntryAsState()
      val route = remember { mutableStateOf<String?>(null) }

      LaunchedEffect(backEntry) {
        route.value = backEntry?.destination?.route
        currentRoute = route.value
      }

      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    // Wait for the screen to load
    compose.waitForIdle()

    // Verify we're on the FriendGarden screen
    compose.onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN).assertIsDisplayed()

    // Verify friend's username is displayed
    compose.onNodeWithText(friendPseudo).assertIsDisplayed()

    // Verify plants are displayed
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    compose.onNodeWithTag(plant1Tag).assertIsDisplayed()
  }

  @Test
  fun friendGarden_waterButtonDisabled() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Get the tag for the water button of the first plant
    val waterButtonTag =
        GardenScreenTestTags.getTestTagForOwnedPlantWaterButton(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))

    compose.onNodeWithTag(waterButtonTag).assertDoesNotExist()
  }

  @Test
  fun friendGarden_addPlantButtonHidden() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Verify the Add Plant FAB is not displayed
    compose.onNodeWithTag(GardenScreenTestTags.ADD_PLANT_FAB).assertDoesNotExist()
  }

  @Test
  fun friendGarden_editProfileButtonIsNotEnabled() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Verify the Edit Profile button (i.e. the friend avatar) is not clickable to go and edit
    compose
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.AVATAR_EDIT_PROFILE)
        .assertIsDisplayed()
        .performClick()
    compose.onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN).assertIsDisplayed()
  }

  @Test
  fun friendGarden_backButtonWorks() = runTest {
    var currentRoute: String? = null

    compose.setContent {
      val navController = rememberNavController()
      val backEntry by navController.currentBackStackEntryAsState()
      val route = remember { mutableStateOf<String?>(null) }

      LaunchedEffect(backEntry) {
        route.value = backEntry?.destination?.route
        currentRoute = route.value
      }

      // Start at FriendList, then navigate to FriendGarden
      AppNavHost(navController = navController, startDestination = Screen.FriendList.route)

      // Navigate to friend's garden after compose is set up
      LaunchedEffect(Unit) { navController.navigate(Screen.FriendGarden.buildRoute(friendId)) }
    }

    // Wait for navigation to FriendGarden to complete
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute?.contains(Screen.FriendGarden.BASE) == true }

    // Verify we're on FriendGarden
    compose.runOnIdle { assertTrue(currentRoute?.contains(Screen.FriendGarden.BASE) == true) }

    // Click the back button to navigate back to FriendList
    compose
        .onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Wait for navigation back to FriendList
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute == Screen.FriendList.route }

    // Verify we're back on FriendList
    compose.runOnIdle { assertEquals(Screen.FriendList.route, currentRoute) }
  }

  @Test
  fun friendGarden_loadsUserProfileFromUserProfileRepository() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Verify that the friend's pseudo is displayed (loaded via UserProfileRepository)
    compose.onNodeWithText(friendPseudo).assertIsDisplayed()

    // Verify the avatar is displayed (we can't directly test Avatar but the profile row exists)
    compose.onNodeWithTag(GardenAchievementsParentScreenTestTags.AVATAR_EDIT_PROFILE).assertExists()
  }

  @Test
  fun friendGarden_loadsPlantsFromCorrectUserId() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Verify both plants are displayed (loaded via getAllOwnedPlantsByUserId)
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    val plant2Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-2", TestPlants.plant2, Timestamp(0)))

    compose.onNodeWithTag(plant1Tag).assertExists()
    compose.onNodeWithTag(plant2Tag).assertExists()
  }

  @Test
  fun navigateFromFriendListToFriendGarden() = runTest {
    var currentRoute: String? = null

    // Configure FakeFriendsRepository ONLY for this test
    // because it's the only one that needs to display FriendList screen
    val fakeFriends = FakeFriendsRepository()
    fakeFriends.friendsFlow.value = listOf(friendId)
    FriendsRepositoryProvider.repository = fakeFriends

    compose.setContent {
      val navController = rememberNavController()
      val backEntry by navController.currentBackStackEntryAsState()
      val route = remember { mutableStateOf<String?>(null) }

      LaunchedEffect(backEntry) {
        route.value = backEntry?.destination?.route
        currentRoute = route.value
      }

      AppNavHost(navController = navController, startDestination = Screen.FriendList.route)
    }

    // Wait for FriendList to load
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute == Screen.FriendList.route }

    // Verify we're on FriendList screen
    compose.runOnIdle { assertEquals(Screen.FriendList.route, currentRoute) }

    // Click on the friend card to navigate to their garden
    // The friend card should display the pseudo
    compose.onNodeWithText(friendPseudo).assertIsDisplayed()
    compose.onNodeWithText(friendPseudo).performClick()

    // Wait for navigation to FriendGarden
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute?.contains(Screen.FriendGarden.BASE) == true }

    // Verify we're now on FriendGarden screen
    compose.runOnIdle { assertTrue(currentRoute?.contains(Screen.FriendGarden.BASE) == true) }

    // Verify the friend's garden is displayed
    compose.onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN).assertIsDisplayed()
    compose.onNodeWithText(friendPseudo).assertIsDisplayed()

    // Verify plants are displayed
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    compose.onNodeWithTag(plant1Tag).assertIsDisplayed()
  }
}
