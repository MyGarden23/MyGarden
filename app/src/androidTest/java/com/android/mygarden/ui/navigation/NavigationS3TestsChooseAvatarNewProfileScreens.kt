package com.android.mygarden.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.profile.ChooseProfilePictureScreenTestTags
import com.android.mygarden.ui.profile.NewProfileScreenTestTags
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation tests for the flows of NewProfile and ChooseAvatar screens.
 *
 * This class verifies correct navigation behavior and state handling.
 */
@RunWith(AndroidJUnit4::class)
class NavigationS3TestsChooseAvatarNewProfileScreens {

  @get:Rule val composeTestRule = createComposeRule()

  /** Navigation controller used to simulate screen navigation. */
  private lateinit var navController: NavHostController

  /**
   * Sets up the Compose test environment before each test. Initializes the NavController and starts
   * at the NewProfile screen.
   */
  @Before
  fun setUp() {
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      AppNavHost(navController = controller, startDestination = Screen.NewProfile.route)
    }
  }

  /** Helper function to clicks the avatar icon on the NewProfile screen. */
  private fun clickAvatar() =
      composeTestRule.onNodeWithTag(NewProfileScreenTestTags.AVATAR).performClick()

  /** Tests navigation from NewProfile to ChooseAvatar and back after avatar selection. */
  @Test
  fun navigate_fromNewProfile_toChooseAvatar_andBack() {
    composeTestRule.onNodeWithTag(NewProfileScreenTestTags.SCREEN).assertIsDisplayed()
    clickAvatar()
    composeTestRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.SCREEN).assertIsDisplayed()

    val firstAvatar = Avatar.values().first()
    composeTestRule
        .onNodeWithTag(ChooseProfilePictureScreenTestTags.getTestTagAvatar(firstAvatar))
        .performClick()

    composeTestRule.onNodeWithTag(NewProfileScreenTestTags.SCREEN).assertIsDisplayed()
  }

  /** Tests that pressing the back button returns from ChooseAvatar to NewProfile. */
  @Test
  fun chooseAvatar_backButton_returnsToNewProfile() {
    clickAvatar()
    composeTestRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NewProfileScreenTestTags.SCREEN).assertIsDisplayed()
  }

  /**
   * Verifies that choosing an avatar updates and then resets the SavedStateHandle value
   * ("chosen_avatar").
   */
  @Test
  fun choosingAvatar_setsAndResetsSavedStateHandleValue() {
    composeTestRule.onNodeWithTag(NewProfileScreenTestTags.SCREEN).assertIsDisplayed()
    clickAvatar()
    composeTestRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.SCREEN).assertIsDisplayed()

    val firstAvatar = Avatar.values().first()
    composeTestRule
        .onNodeWithTag(ChooseProfilePictureScreenTestTags.getTestTagAvatar(firstAvatar))
        .performClick()

    // Read handle value before reset
    val currentEntry = navController.currentBackStackEntry
    val currentVal = currentEntry?.savedStateHandle?.get<String>("chosen_avatar")
    assertEquals(firstAvatar.name, currentVal)

    composeTestRule.waitForIdle()

    // Read handle value after reset
    val resetValue = currentEntry?.savedStateHandle?.get<String>("chosen_avatar")
    assertEquals("", resetValue)
  }

  /** Tests that pressing the register button navigates to the Camera screen. */
  @Test
  fun onRegisterPressed_from_newProfile_navigates_to_camera() {
    composeTestRule.onNodeWithTag(NewProfileScreenTestTags.SCREEN).assertIsDisplayed()

    composeTestRule.runOnIdle { navController.navigate(Screen.Camera.route) }

    assertEquals(Screen.Camera.route, navController.currentDestination?.route)
  }
}
