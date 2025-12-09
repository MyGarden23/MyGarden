package com.android.mygarden.zEndToEnd

import android.Manifest
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.MainActivity
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.plant.testTag
import com.android.mygarden.ui.authentication.SignInScreenTestTags
import com.android.mygarden.ui.camera.CameraScreenTestTags
import com.android.mygarden.ui.editPlant.DeletePlantPopupTestTags
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.profile.ProfileScreenTestTags
import com.android.mygarden.utils.FakePlantRepositoryUtils
import com.android.mygarden.utils.FirebaseUtils
import com.android.mygarden.utils.PlantRepositoryType
import com.android.mygarden.utils.RequiresCamera
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresCamera
@RunWith(AndroidJUnit4::class)
class EndToEndEpic2 {
  companion object {
    init {
      // Set system property BEFORE the compose rule is created
      System.setProperty("mygarden.e2e", "true")
    }
  }

  @get:Rule val composeTestRule = createEmptyComposeRule()

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

  private val mockPlant = Plant(name = "Rose", latinName = "Rosa", location = PlantLocation.OUTDOOR)

  private val firebaseUtils: FirebaseUtils = FirebaseUtils()
  private val fakePlantRepoUtils = FakePlantRepositoryUtils(PlantRepositoryType.PlantRepoFirestore)

  private lateinit var scenario: ActivityScenario<MainActivity>

  @Before
  fun setUp() = runTest {
    // Set up any necessary configurations or states before each test
    Log.d("EndToEndEpic2", "setUpEntry")
    firebaseUtils.initialize()
    Log.d("EndToEndEpic2", "Initialized and signed out")
    fakePlantRepoUtils.mockIdentifyPlant(mockPlant)
    fakePlantRepoUtils.setUpMockRepo()
    Log.d("EndToEndEpic2", "Set up mock repo")

    // Now launch the activity AFTER Firebase is cleaned up
    scenario = ActivityScenario.launch(MainActivity::class.java)
    Log.d("EndToEndEpic2", "Activity launched")

    composeTestRule.waitForIdle()
    waitForAppToLoad()
  }

  @After
  fun tearDown() {
    // Clean up the activity scenario
    if (::scenario.isInitialized) {
      scenario.close()
    }
    // Clean up the system property to avoid affecting other tests
    System.clearProperty("mygarden.e2e")
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

  @Test
  fun test_end_to_end_epic_2() {
    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    runBlocking {
      firebaseUtils.signIn()
      firebaseUtils.waitForAuthReady()
    }
    // === NEW PROFILE SCREEN ===
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD).performTextInput("pseudo")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .performTextInput("Switzerland")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
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

    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()

    // === PLANT INFO SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }

    // === EDIT PLANT SCREEN (NEW FLOW SINCE S4) ===
    // Click Next to save plant and navigate to EditPlant
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
    }

    // Verify we're on EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    // Scroll to the Save button
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))

    // Click Save to navigate to Garden
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()

    // Wait for the garden list to appear
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).isDisplayed()
    }

    val listOfOwnedPlant = runBlocking { PlantsRepositoryProvider.repository.getAllOwnedPlants() }

    // Check that the plant is in the garden.
    assert(listOfOwnedPlant.size == 1)
    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(listOfOwnedPlant.first())

    // click on plant
    composeTestRule.waitUntil(TIMEOUT) { composeTestRule.onNodeWithTag(plantTag).isDisplayed() }
    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Plant Info
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).isDisplayed()

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertTextContains("Rose")
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertTextContains("Rosa")

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).isDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).performClick()

    // Edit plant
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
    }

    // Fill the name, latin name and description
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).performTextClearance()
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).performTextInput("FakePlant")
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN)
        .performTextInput("FakePlantus")
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .performTextInput("Just a test plant")
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE)
        .performTextInput("Light exposure")
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.LOCATION_DROPDOWN).performClick()
    composeTestRule.onNodeWithTag(PlantLocation.OUTDOOR.testTag).performClick()

    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()
    // Garden
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    val listOfOwnedPlant2 = runBlocking { PlantsRepositoryProvider.repository.getAllOwnedPlants() }
    // Check that the plant is in the garden.
    assert(listOfOwnedPlant2.size == 1)
    val plantTag2 = GardenScreenTestTags.getTestTagForOwnedPlant(listOfOwnedPlant2.first())

    // click on plant
    composeTestRule.waitUntil(TIMEOUT) { composeTestRule.onNodeWithTag(plantTag2).isDisplayed() }
    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Plant Info
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).isDisplayed()

    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME)
        .assertTextContains("FakePlant")
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertTextContains("FakePlantus")
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
        .assertTextContains("Just a test plant")

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).isDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).performClick()

    // Edit plant
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
    }

    // Delete the plant
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()
    // Confirm the deletion
    composeTestRule.onNodeWithTag(DeletePlantPopupTestTags.POPUP).isDisplayed()
    composeTestRule.onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON).performClick()

    // Garden
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    val listOfOwnedPlant3 = runBlocking { PlantsRepositoryProvider.repository.getAllOwnedPlants() }
    // Check that the plant is in the garden.
    assert(listOfOwnedPlant3.isEmpty())
  }
}
