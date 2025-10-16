package com.android.mygarden.zendToEnd

import android.Manifest
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
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test for MyGarden's core user flow.
 *
 * Tests the complete journey: Camera → Plant Info → Garden → Navigation Runs in authenticated mode
 * (skips sign-in) via system property.
 */
@RunWith(AndroidJUnit4::class)
class EndToEndM1 {

  companion object {
    init {
      // Set system property BEFORE the compose rule is created
      System.setProperty("mygarden.e2e", "true")
    }
  }

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  /**
   * Automatically grants camera permission before tests run. Essential for camera functionality
   * testing without user interaction prompts.
   */
  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.CAMERA)

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

  /**
   * Tests the complete user flow:
   * 1. Camera screen - take photo
   * 2. Plant info screen - view description/health tabs
   * 3. Navigation - back/forth between screens
   * 4. Garden screen - save plant, use ADD
   * 5. Bottom bar navigation
   */
  @Test
  fun endToEndTest() {
    // === CAMERA SCREEN ===
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()

    // Wait for camera ready
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).isDisplayed()
    }

    // Verify UI elements
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.PROFILE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON).assertIsDisplayed()

    // Take photo
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()

    // === PLANT INFO SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).isDisplayed()

    // Test description tab content
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TAB).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
        .assertTextEquals("Roses are red")
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertTextEquals("Rose")
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertTextEquals("Rosum")

    // Test health tab
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

    // === BACK TO CAMERA ===
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).isDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).performClick()

    // === PLANT INFO AGAIN ===
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SAVE_BUTTON).assertIsDisplayed()

    // === GARDEN SCREEN ===
    // Save plant and navigate to garden
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()

    // Verify garden elements
    composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.USERNAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.ADD_PLANT_FAB).assertIsDisplayed()

    // Test FAB navigation
    composeTestRule.onNodeWithTag(GardenScreenTestTags.ADD_PLANT_FAB).performClick()

    // === BOTTOM BAR NAVIGATION ===
    // Verify back on camera from FAB
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()

    // Test garden button
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()

    // Test camera button
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()
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
