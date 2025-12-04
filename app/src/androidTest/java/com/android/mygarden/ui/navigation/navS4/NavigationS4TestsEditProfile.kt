package com.android.mygarden.ui.navigation.navS4

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.R
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.profile.PseudoRepositoryProvider
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.profile.ProfileScreenTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeAchievementsRepository
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.FakePseudoRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationS4TestsEditProfile {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var context: Context

  @Before
  fun setUp() {
    composeTestRule.setContent {
      context = LocalContext.current
      val controller = rememberNavController()
      navController = controller
      MyGardenTheme {
        AppNavHost(navController = controller, startDestination = Screen.Garden.route)
      }
    }
    ProfileRepositoryProvider.repository = FakeProfileRepository()
    PseudoRepositoryProvider.repository = FakePseudoRepository()
    AchievementsRepositoryProvider.repository = FakeAchievementsRepository()
    composeTestRule.waitForIdle()
  }

  @Test
  fun canGofromGardenToEditProfileAndBackToGardenWithSave() {
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains(context.getString(R.string.edit_profile_screen_title))
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD).performTextInput("pseudo")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .performTextInput("Switzerland")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
  }

  @Test
  fun canGofromGardenToEditProfileAndBackToGardenWithBack() {
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed()
  }
}
