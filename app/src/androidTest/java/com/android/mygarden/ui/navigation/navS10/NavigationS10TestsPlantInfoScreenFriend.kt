package com.android.mygarden.ui.navigation.navS10

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
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
class NavigationS10TestsPlantInfoScreenFriend : FirestoreProfileTest() {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private val friendId = "friend-test-user-456"
  private val friendPseudo = "TestFriendForPlantInfo"
  private lateinit var originalFriendsRepo: FriendsRepository
  private lateinit var originalUserProfileRepo: UserProfileRepository
  private lateinit var originalPlantsRepo: PlantsRepository

  /**
   * Helper composable that sets up a NavController and tracks the current navigation route.
   *
   * @param onRouteChanged Callback invoked when the route changes, receives the new route
   * @param content Composable content that receives the NavHostController
   */
  @Composable
  private fun SetupNavControllerWithRouteTracking(
      onRouteChanged: (String?) -> Unit,
      content: @Composable (androidx.navigation.NavHostController) -> Unit
  ) {
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(backEntry) {
      val route = backEntry?.destination?.route
      onRouteChanged(route)
    }

    content(navController)
  }

  @Before
  fun setupFriendPlantInfoTest() = runTest {
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
            friendId, friendPseudo, Avatar.A3, "EXPERT", "Tomato")
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
  fun friendGarden_clickPlant_navigatesToPlantInfo() = runTest {
    var currentRoute: String? = null

    compose.setContent {
      SetupNavControllerWithRouteTracking(onRouteChanged = { currentRoute = it }) { navController ->
        AppNavHost(
            navController = navController,
            startDestination = Screen.FriendGarden.buildRoute(friendId))
      }
    }

    compose.waitForIdle()

    // Verify we're on FriendGarden
    compose.runOnIdle { assertTrue(currentRoute?.contains(Screen.FriendGarden.BASE) == true) }

    // Click on the first plant card
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    compose.onNodeWithTag(plant1Tag).assertIsDisplayed().performClick()

    // Wait for navigation to PlantInfoFromGarden
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute == Screen.PlantInfoFromGarden.route }

    // Verify we're now on PlantInfo screen
    compose.runOnIdle { assertEquals(Screen.PlantInfoFromGarden.route, currentRoute) }
    compose.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun friendPlantInfo_displaysCorrectPlantData() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Click on plant to navigate to PlantInfo
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    compose.onNodeWithTag(plant1Tag).performClick()

    compose.waitForIdle()

    // Verify correct plant data is displayed
    compose.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertIsDisplayed()
    compose.onNodeWithText(TestPlants.plant1.name).assertIsDisplayed()
    compose.onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME).assertIsDisplayed()
    compose.onNodeWithText(TestPlants.plant1.latinName).assertIsDisplayed()
  }

  @Test
  fun friendPlantInfo_editButtonIsHidden() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Navigate to PlantInfo
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    compose.onNodeWithTag(plant1Tag).performClick()

    compose.waitForIdle()

    // Verify Edit/Next button does NOT exist
    compose.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
    compose.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).assertDoesNotExist()
  }

  @Test
  fun friendPlantInfo_backButton_returnsToFriendGarden() = runTest {
    var currentRoute: String? = null

    compose.setContent {
      SetupNavControllerWithRouteTracking(onRouteChanged = { currentRoute = it }) { navController ->
        AppNavHost(
            navController = navController,
            startDestination = Screen.FriendGarden.buildRoute(friendId))
      }
    }

    compose.waitForIdle()

    // Navigate to PlantInfo
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    compose.onNodeWithTag(plant1Tag).performClick()

    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute == Screen.PlantInfoFromGarden.route }

    // Verify we're on PlantInfo
    compose.runOnIdle { assertEquals(Screen.PlantInfoFromGarden.route, currentRoute) }

    // Click back button
    compose.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).assertIsDisplayed().performClick()

    // Wait for navigation back to FriendGarden
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute?.contains(Screen.FriendGarden.BASE) == true }

    // Verify we're back on FriendGarden
    compose.runOnIdle { assertTrue(currentRoute?.contains(Screen.FriendGarden.BASE) == true) }
  }

  @Test
  fun friendPlantInfo_displaysAllTabs() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Navigate to PlantInfo
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    compose.onNodeWithTag(plant1Tag).performClick()

    compose.waitForIdle()

    // Verify all three tabs are displayed
    compose.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TAB).assertIsDisplayed()
    compose.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).assertIsDisplayed()
    compose.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TAB).assertIsDisplayed()

    // Test clicking on Health tab
    compose.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    compose.waitForIdle()
    compose.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS).assertIsDisplayed()

    // Test clicking on Location tab
    compose.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TAB).performClick()
    compose.waitForIdle()
    compose.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TEXT).assertIsDisplayed()
  }

  @Test
  fun friendPlantInfo_loadsPlantFromFriendRepository() = runTest {
    compose.setContent {
      val navController = rememberNavController()
      AppNavHost(
          navController = navController,
          startDestination = Screen.FriendGarden.buildRoute(friendId))
    }

    compose.waitForIdle()

    // Navigate to PlantInfo for plant2
    val plant2Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-2", TestPlants.plant2, Timestamp(0)))
    compose.onNodeWithTag(plant2Tag).performClick()

    compose.waitForIdle()

    // Verify correct plant data is loaded (plant2, not plant1)
    compose.onNodeWithText(TestPlants.plant2.name).assertIsDisplayed()
    compose.onNodeWithText(TestPlants.plant2.latinName).assertIsDisplayed()
  }

  @Test
  fun navigateFromFriendListToFriendGardenToPlantInfo() = runTest {
    var currentRoute: String? = null

    // Configure FakeFriendsRepository for this test
    val fakeFriends = FakeFriendsRepository()
    fakeFriends.friendsFlow.value = listOf(friendId)
    FriendsRepositoryProvider.repository = fakeFriends

    compose.setContent {
      SetupNavControllerWithRouteTracking(onRouteChanged = { currentRoute = it }) { navController ->
        AppNavHost(navController = navController, startDestination = Screen.FriendList.route)
      }
    }

    // Wait for FriendList to load
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute == Screen.FriendList.route }

    // Verify we're on FriendList
    compose.runOnIdle { assertEquals(Screen.FriendList.route, currentRoute) }

    // Click on friend to navigate to their garden
    compose.onNodeWithText(friendPseudo).assertIsDisplayed().performClick()

    // Wait for navigation to FriendGarden
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute?.contains(Screen.FriendGarden.BASE) == true }

    // Verify we're on FriendGarden
    compose.runOnIdle { assertTrue(currentRoute?.contains(Screen.FriendGarden.BASE) == true) }

    // Click on a plant to navigate to PlantInfo
    val plant1Tag =
        GardenScreenTestTags.getTestTagForOwnedPlant(
            OwnedPlant("friend-plant-1", TestPlants.plant1, Timestamp(0)))
    compose.onNodeWithTag(plant1Tag).assertIsDisplayed().performClick()

    // Wait for navigation to PlantInfo
    compose.waitForIdle()
    compose.waitUntil(DEFAULT_WAIT_MS) { currentRoute == Screen.PlantInfoFromGarden.route }

    // Verify we're on PlantInfo
    compose.runOnIdle { assertEquals(Screen.PlantInfoFromGarden.route, currentRoute) }

    // Verify plant data is displayed
    compose.onNodeWithText(TestPlants.plant1.name).assertIsDisplayed()

    // Verify buttons are in correct state (view mode)
    compose.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).assertDoesNotExist()
    compose.onNodeWithTag(PlantInfoScreenTestTags.TIPS_BUTTON).assertIsDisplayed()
  }
}
