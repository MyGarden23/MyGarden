package com.android.mygarden.endToEnd

import android.Manifest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.MainActivity
import com.android.mygarden.R
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.ui.authentication.SignInScreenTestTags
import com.android.mygarden.ui.camera.CameraScreenTestTags
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.profile.ProfileScreenTestTags
import com.android.mygarden.utils.FirebaseUtils
import com.android.mygarden.utils.RequiresCamera
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Constant string for the e2e test
private const val ERROR_LATIN_NAME_DESCRIPTION = "There was an error getting the plant latin name."
private const val NOT_IDENTIFY_PLANT_DESCRIPTION = "The AI was not able to identify the plant."
private const val UNKNOWN_PLANT_NAME = "Unknown"
private const val NO_HEALTH_DESCRIPTION = "No health status description available"
/**
 * End-to-end test for MyGarden's core user flow. This test assume that we have an internet
 * connection
 *
 * Tests the complete journey: Camera → Plant Info → Garden → Navigation Runs in authenticated mode
 * (skips sign-in) via system property.
 */
@RequiresCamera
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
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

  @get:Rule
  val permissionNotifsRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)
  private val TIMEOUT = 10_000L
  private val firebaseUtils: FirebaseUtils = FirebaseUtils()

  /**
   * Pre-test setup method that ensures the application is fully loaded before test execution.
   *
   * Performs:
   * - Waits for Compose UI to be idle
   * - Calls waitForAppToLoad() to verify core components are ready
   */
  @Before
  fun setUp() = runTest {
    firebaseUtils.initialize()
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
  fun endToEndTest() = runTest {
    val context = composeTestRule.activity
    PlantsRepositoryProvider.repository = PlantsRepositoryLocal()
    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    firebaseUtils.signIn()
    // === NEW PROFILE SCREEN ===
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD).performTextInput("pseudo")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .performTextInput("Switzerland")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).isDisplayed()
    }
    // === CAMERA SCREEN ===
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()

    // Wait for camera ready
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).isDisplayed()
    }

    // Verify UI elements
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).assertIsDisplayed()
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

    // This handles the non-deterministic behavior of the AI (between 2 possible cases)
    val text =
        composeTestRule
            .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            ?.joinToString(separator = "") { it.text } ?: ""
    assertTrue(
        "Expected one of the possible texts, but was: $text",
        text == context.getString(R.string.loading_plant_infos) ||
            text == ERROR_LATIN_NAME_DESCRIPTION)

    /**
     * This is in the specific case where the AI does not return the [ERROR_LATIN_NAME_DESCRIPTION]
     * description
     *
     * Need to wait for Gemini description, assume that it takes < 10 seconds. The pictures are
     * taken from the emulator, hence the AI does not recognize plants
     */
    if (text == context.getString(R.string.loading_plant_infos)) {
      composeTestRule.waitUntil(TIMEOUT) {
        val currentText =
            composeTestRule
                .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.Text)
                ?.joinToString(separator = "") { it.text } ?: ""

        currentText.contains(NOT_IDENTIFY_PLANT_DESCRIPTION)
      }
      composeTestRule
          .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
          .assertTextEquals(NOT_IDENTIFY_PLANT_DESCRIPTION)
    }

    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME)
        .assertTextEquals(UNKNOWN_PLANT_NAME)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertTextEquals(UNKNOWN_PLANT_NAME)

    // Test health tab
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS_DESCRIPTION)
        .assertTextEquals(NO_HEALTH_DESCRIPTION)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals(
            context.getString(R.string.status_label, PlantHealthStatus.UNKNOWN.description))
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY)
        .assertTextEquals(context.getString(R.string.watering_frequency, 0))

    // === BACK TO CAMERA ===
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).isDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).performClick()

    // Wait for camera to be ready after navigation
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).isDisplayed()
    }

    // === PLANT INFO AGAIN ===
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).assertIsDisplayed()

    // === EDIT PLANT SCREEN (NEW FLOW SINCE S4) ===
    // Click Next to save plant and navigate to EditPlant
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
    }

    // Verify we're on EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    // Fill the name, latin name and description
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).performTextInput("FakePlant")
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN)
        .performTextInput("FakePlantus")
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .performTextInput("Just a test plant")
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE)
        .performTextInput("Light exposure")

    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))
    // Click Save to navigate to Garden
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()

    // === GARDEN SCREEN ===
    // Verify navigation to garden after saving from EditPlant
    composeTestRule.waitForIdle()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()

    // Verify garden elements
    composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.USER_PROFILE_PICTURE).assertIsDisplayed()
    // assertExists and not assertIsDisplayed because the username is an empty string for this test.
    // Hence the Node exists but is not displayed
    composeTestRule.onNodeWithTag(GardenScreenTestTags.USERNAME).assertExists()
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
