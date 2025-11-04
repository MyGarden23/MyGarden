package com.android.mygarden.ui.navigation.navS1

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.ui.navigation.BottomBar
import com.android.mygarden.ui.navigation.NavigationActions
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Page
import com.android.mygarden.ui.navigation.Screen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * These tests will create fictional screens in order to simulate the bottom bar interactions
 * without the actual implementations of the screens. Only used for S1, next they will be adapted to
 * test it on the real app.
 */
@RunWith(AndroidJUnit4::class)
class NavigationS1Tests {

  @Composable
  fun FictionalGardenScreen(navActions: NavigationActions? = null) {
    Scaffold(
        modifier = Modifier.testTag(NavigationTestTags.GARDEN_SCREEN),
        bottomBar = {
          BottomBar(
              selectedPage = Page.Garden,
              onSelect = { page -> navActions?.navTo(page.destination) })
        },
        content = { pd -> Row(modifier = Modifier.padding(pd), content = {}) })
  }

  @Composable
  fun FictionalCameraScreen(navActions: NavigationActions? = null) {
    Scaffold(
        modifier = Modifier.testTag(NavigationTestTags.CAMERA_SCREEN),
        bottomBar = {
          BottomBar(
              selectedPage = Page.Camera,
              onSelect = { page -> navActions?.navTo(page.destination) })
        },
        content = { pd -> Row(modifier = Modifier.padding(pd), content = {}) })
  }

  @Composable
  fun FictionalApp() {
    val navController = rememberNavController()
    val navActions = NavigationActions(navController)
    val startDest = Screen.Camera.name
    NavHost(navController = navController, startDestination = startDest) {
      navigation(startDestination = Screen.Camera.route, route = Screen.Camera.name) {
        composable(route = Screen.Camera.route) { FictionalCameraScreen(navActions) }
      }
      navigation(startDestination = Screen.Garden.route, route = Screen.Garden.name) {
        composable(route = Screen.Garden.route) { FictionalGardenScreen(navActions) }
      }
    }
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setUp() {
    composeTestRule.setContent { FictionalApp() }
    composeTestRule.waitForIdle()
  }

  @Test
  fun allTagsAreDisplayed() {
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun bottomBarIsDisplayedOnCameraScreen() {
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
  }

  @Test
  fun bottomBarIsDisplayedOnProfileScreen() {
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
  }

  @Test
  fun canNavigateFromCameraToProfileUsingBottomBar() {
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
  }

  @Test
  fun canNavigateFromProfileToCameraUsingBottomBar() {
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
  }
}
