package com.android.mygarden.endToEnd

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.MainActivity
import com.android.mygarden.ui.camera.CameraScreenTestTags
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for MyGarden application. These tests assume the user is already signed in so we
 * begin on the camera screen.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndM1 {

  companion object {
    init {
      // Set system property BEFORE the compose rule is created
      System.setProperty("mygarden.e2e", "true")
    }
  }

  /**
   * Main compose test rule that creates and manages the MainActivity for testing. Provides access
   * to the full app UI hierarchy and interaction methods.
   */
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  /**
   * Automatically grants camera permission before tests run. Essential for camera functionality
   * testing without user interaction prompts.
   */
  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

  private val TIMEOUT = 10_000L

  /**
   * Pre-test setup method that ensures the application is fully loaded before test execution.
   *
   * Performs:
   * - Waits for Compose UI to be idle
   * - Calls waitForAppToLoad() to verify core components are ready
   */
  @Before
  fun setUp() {
    // Wait for the app to be fully loaded
    composeTestRule.waitForIdle()
    waitForAppToLoad()
  }

  @Test
  fun endToEndTest() {
    // === 1. CAMERA SCREEN VALIDATION ===
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
    // assertEquals(Screen.Camera.route, currentRoute.value)
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON).performClick()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).isDisplayed()

    // === 2. PHOTO CAPTURE & NAVIGATION ===
    // Take a picture
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()

    // Wait for navigation to plant info screen (async operation)
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).isDisplayed()
    }

    // === 3. PLANT INFO VALIDATION - DESCRIPTION TAB ===
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TAB).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
        .assertTextEquals("Roses are red")
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertTextEquals("Rose")
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertTextEquals("Rosum")

    // === 4. PLANT INFO VALIDATION - HEALTH TAB ===
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS_DESCRIPTION)
        .assertTextEquals("The plant is healthy 🌱")
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals("Status: The plant is healthy 🌱")
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY)
        .assertTextEquals("Watering Frequency: Every 2 days")

    // === 5. NAVIGATION FLOW TESTING ===
    // Back to Camera and return to PlantInfoScreen
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).isDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SAVE_BUTTON).assertIsDisplayed()

    // TODO: Continue to profile navigation via bottom bar

  }

  /** Waits for the app to fully load by checking for key UI elements */
  private fun waitForAppToLoad() {
    composeTestRule.waitUntil(TIMEOUT) {
      try {
        // Check if root is available
        composeTestRule.onRoot().fetchSemanticsNode()
        true
      } catch (_: Throwable) {
        false
      }
    }
  }

  @After
  fun tearDown() {
    // Clean up the system property to avoid affecting other tests
    System.clearProperty("mygarden.e2e")
  }
}
