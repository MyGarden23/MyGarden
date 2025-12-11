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
import com.android.mygarden.utils.TestPlants
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val john = "John"
const val doe = "Doe"
const val user_pseudo = "pseudo"
const val switzerland = "Switzerland"

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

  private val mockPlant = TestPlants.healthyBamboo

  private val newMockPlant = TestPlants.samplePlant1

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
    Log.d("EndToEndEpic2", "Set up mock repo")
    fakePlantRepoUtils.setUpMockRepo()
    Log.d("EndToEndEpic2", "Set up the UserProfileRepository")

    // Now launch the activity AFTER Firebase is cleaned up
    scenario = ActivityScenario.launch(MainActivity::class.java)
    Log.d("EndToEndEpic2", "Activity launched")

    composeTestRule.waitForIdle()
    waitForAppToLoad()
  }

  /**
   * End to end test for the epic Garden Owner (epic 2).
   *
   * Test's flow :
   *
   * Create a new profile -> take a picture -> plant info -> edit plant -> save the plant to the
   * garden. Ensure the user's avatar and username are displayed. Ensure the plant is displayed in
   * the garden. Click on the plant -> plant info -> edit plant -> change the information of the
   * plant -> save the modifications -> click again on the plant form the garden -> plant info.
   * Ensure the information about the plant matches what was filled earlier. -> edit plant -> delete
   * the plant -> Ensure the plant is not in the garden anymore.
   */
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
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput(john)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).performTextInput(doe)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD).performTextInput(user_pseudo)
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput(switzerland)
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

    // Click Next to save plant and navigate to EditPlant
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

    // === EDIT PLANT SCREEN ===
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

    // === GARDEN SCREEN ===

    // Wait for the garden list to appear
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).isDisplayed()
    }

    // Check that the pseudo and the avatar are displayed
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.USERNAME)
        .assertIsDisplayed()
        .assertTextContains(user_pseudo)
    composeTestRule.onNodeWithTag(GardenScreenTestTags.USER_PROFILE_PICTURE).assertIsDisplayed()

    // Get the list of ownedPlant in the repository
    val listOfOwnedPlant = runBlocking { PlantsRepositoryProvider.repository.getAllOwnedPlants() }

    // Check that the plant is in the garden.
    assert(listOfOwnedPlant.size == 1)

    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(listOfOwnedPlant.first())

    // Click on plant
    composeTestRule.waitUntil(TIMEOUT) { composeTestRule.onNodeWithTag(plantTag).isDisplayed() }
    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // === PLANT INFO SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    // Check that the plant's name and latin name are displayed and correct
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME)
        .assertIsDisplayed()
        .assertTextContains(mockPlant.name)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertIsDisplayed()
        .assertTextContains(mockPlant.latinName)

    // Click Edit button to navigate to EditPlant
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // === EDIT PLANT SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
    }

    // Fill the name, latin name, description, light exposure and location of the plant

    // Clear and fill plant name
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME)
        .assertIsDisplayed()
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME)
        .performTextInput(newMockPlant.name)

    // Clear and fill plant latin name
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN)
        .assertIsDisplayed()
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN)
        .performTextInput(newMockPlant.latinName)

    // Clear and fill plant description
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .assertIsDisplayed()
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .performTextInput(newMockPlant.description)

    // Clear and fill plant light exposure
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE)
        .assertIsDisplayed()
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE)
        .performTextInput(newMockPlant.lightExposure)

    // Select the OUTDOOR location
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.LOCATION_DROPDOWN).performClick()
    composeTestRule.onNodeWithTag(PlantLocation.OUTDOOR.testTag).performClick()

    // Click Save to navigate to Garden
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()

    // === GARDEN SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    }

    // Get the list of ownedPlant in the repository
    val listOfOwnedPlant2 = runBlocking { PlantsRepositoryProvider.repository.getAllOwnedPlants() }

    // Check that the plant is in the garden.
    assert(listOfOwnedPlant2.size == 1)

    val plantTag2 = GardenScreenTestTags.getTestTagForOwnedPlant(listOfOwnedPlant2.first())

    // Click on the plant
    composeTestRule.waitUntil(TIMEOUT) { composeTestRule.onNodeWithTag(plantTag2).isDisplayed() }
    composeTestRule.onNodeWithTag(plantTag2).assertIsDisplayed().performClick()

    // === PLANT INFO SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }

    // Check that the information about the plant matches what was filled earlier.
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME)
        .assertIsDisplayed()
        .assertTextContains(newMockPlant.name)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertIsDisplayed()
        .assertTextContains(newMockPlant.latinName)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
        .assertIsDisplayed()
        .assertTextContains(newMockPlant.description)

    // Click Edit button to navigate to EditPlant
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // === EDIT PLANT SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
    }

    // Delete the plant
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()

    // Confirm the deletion
    composeTestRule.onNodeWithTag(DeletePlantPopupTestTags.POPUP).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // === GARDEN SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    }

    // Get the list of ownedPlant in the repository
    val listOfOwnedPlant3 = runBlocking { PlantsRepositoryProvider.repository.getAllOwnedPlants() }

    // Check that there is no plant in the garden
    assert(listOfOwnedPlant3.isEmpty())
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
    // Clean up the activity scenario
    if (::scenario.isInitialized) {
      scenario.close()
    }
    // Clean up the system property to avoid affecting other tests
    System.clearProperty("mygarden.e2e")
  }
}
