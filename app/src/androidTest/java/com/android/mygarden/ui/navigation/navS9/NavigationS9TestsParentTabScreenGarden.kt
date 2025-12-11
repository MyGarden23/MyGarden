package com.android.mygarden.ui.navigation.navS9

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.garden.GardenAchievementsParentScreenTestTags
import com.android.mygarden.ui.garden.GardenTab
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.FirestoreProfileTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationS9TestsParentTabScreenGarden : FirestoreProfileTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController

  @Before
  override fun setUp() {
    super.setUp()

    // Inject fake repos to display correctly the user name (so it is not empty and displayed)
    UserProfileRepositoryProvider.repository =
        object : UserProfileRepository {
          override suspend fun getUserProfile(userId: String): UserProfile? {
            return UserProfile("test-id", "test-name", Avatar.A1, "test-skill", "test-plant")
          }
        }
    ProfileRepositoryProvider.repository = FakeProfileRepository(Profile(pseudo = "test-pseudo"))

    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      AppNavHost(navController = navController, startDestination = Screen.Garden.route)
    }
    composeTestRule.waitForIdle()
  }

  /** Base case: start garden screen with garden content */
  @Test
  fun gardenTabRowStartsInGardenScreen() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.INTERNAL_GARDEN_SCREEN).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.INTERNAL_ACHIEVEMENTS_SCREEN)
        .assertIsNotDisplayed()
  }

  /** Clicking on the tab row actually make the content change */
  @Test
  fun gardenTabRowDoesSwitchBetweenGardenAndAchievements() {
    composeTestRule
        .onNodeWithTag(
            GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.ACHIEVEMENTS))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.GARDEN))
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(
            GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.ACHIEVEMENTS))
        .performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.INTERNAL_ACHIEVEMENTS_SCREEN)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.INTERNAL_GARDEN_SCREEN).assertIsNotDisplayed()

    composeTestRule
        .onNodeWithTag(
            GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.ACHIEVEMENTS))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.GARDEN))
        .assertIsDisplayed()
  }

  /** The profile row that is in the parent screen stays with both content */
  @Test
  fun profileRowInfoStaysInBothScreens() {
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.AVATAR_EDIT_PROFILE)
        .assertIsDisplayed()
        .assertHasClickAction()
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.PSEUDO, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasClickAction()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasClickAction()

    composeTestRule
        .onNodeWithTag(
            GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.ACHIEVEMENTS))
        .performClick()

    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.AVATAR_EDIT_PROFILE)
        .assertIsDisplayed()
        .assertHasClickAction()
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.PSEUDO, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasClickAction()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertHasClickAction()
  }
}
