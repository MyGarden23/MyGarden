package com.android.mygarden.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.garden.GardenScreen
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.profile.ProfileScreenTestTags
import com.android.mygarden.ui.profile.ProfileViewModel
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeProfileRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationS4TestsEditProfile {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController

  @Before
  fun setUp() {
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      MyGardenTheme {
        AppNavHost(navController = controller, startDestination = Screen.Garden.route)
      }
    }
    ProfileRepositoryProvider.repository = FakeProfileRepository()
    composeTestRule.waitForIdle()
  }


  @Test fun canGofromGardenToEditProfileAndBackToGardenWithSave() {
      composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed().performClick()
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.TITLE).assertTextContains("Edit Profile")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("Switzerland")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed()
  }

  @Test fun canGofromGardenToEditProfileAndBackToGardenWithBack() {
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed().performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed()
  }
}
