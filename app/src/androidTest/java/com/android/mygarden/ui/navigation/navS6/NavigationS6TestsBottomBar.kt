package com.android.mygarden.ui.navigation.navS6

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.MyGardenApp
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FirestoreProfileTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationS6TestsBottomBar : FirestoreProfileTest() {

  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  /**
   * Automatically grants camera permission before tests run. Essential for camera functionality
   * testing without user interaction prompts.
   */
  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

  @get:Rule
  val permissionNotifsRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

  /** Sets up the Compose test environment before each test. */
  @Before
  override fun setUp() {
    super.setUp()
    rule.setContent { MyGardenTheme { MyGardenApp() } }
  }

  /** Tests the flow Camera --> Feed */
  @Test
  fun fromCameraToFeed() {
    // on camera
    rule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    rule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    // click on bottom bar
    rule.onNodeWithTag(NavigationTestTags.FEED_BUTTON).performClick()
    // navigated to feed
    rule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).assertIsDisplayed()
  }

  /** Tests the flow Garden --> Feed */
  @Test
  fun fromGardenToFeed() {
    // on garden
    rule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    rule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    rule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
    rule.onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN).assertIsDisplayed()
    // click on bottom bar
    rule.onNodeWithTag(NavigationTestTags.FEED_BUTTON).performClick()
    // navigated to feed
    rule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).assertIsDisplayed()
  }

  /** Tests the flow Feed --> Camera */
  @Test
  fun fromFeedToCamera() {
    // on feed
    rule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    rule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    rule.onNodeWithTag(NavigationTestTags.FEED_BUTTON).performClick()
    rule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).assertIsDisplayed()

    // comes back on camera
    rule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).performClick()
    rule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
  }

  /** Tests the flow Feed --> Garden */
  @Test
  fun fromFeedToGarden() {
    // on feed
    rule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    rule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    rule.onNodeWithTag(NavigationTestTags.FEED_BUTTON).performClick()
    rule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).assertIsDisplayed()

    // navigated to garden
    rule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
    rule.onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN).assertIsDisplayed()
  }
}
