package com.android.mygarden.zendToEnd

import android.Manifest
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.MainActivity
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.ui.authentication.SignInScreenTestTags
import com.android.mygarden.ui.camera.CameraScreenTestTags
import com.android.mygarden.ui.camera.RequiresCamera
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.profile.ProfileScreenTestTags
import com.android.mygarden.utils.FakePlantRepositoryUtils
import com.android.mygarden.utils.FirebaseUtils
import com.android.mygarden.utils.PlantRepositoryType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresCamera
@RunWith(AndroidJUnit4::class)
class EndToEndM2 {
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

  private val mockPlant = Plant(name = "Rose", latinName = "Rosa", location = PlantLocation.OUTDOOR)

  private val firebaseUtils: FirebaseUtils = FirebaseUtils()
  private val fakePlantRepoUtils = FakePlantRepositoryUtils(PlantRepositoryType.PlantRepoFirestore)

  @Before
  fun setUp() = runTest {
    // Set up any necessary configurations or states before each test
    Log.d("EndToEndM2", "setUpEntry")
    Log.d("EndToEndM2", "Injected profile repository")
    fakePlantRepoUtils.mockIdentifyPlant(mockPlant)
    fakePlantRepoUtils.setUpMockRepo()
    Log.d("EndToEndM2", "Set up mock repo")
    composeTestRule.waitForIdle()
    waitForAppToLoad()

    // Wait for either sign-in screen OR camera screen to be ready
    // (depends on timing of when MainActivity detected auth state)
    composeTestRule.waitUntil(TIMEOUT) {
      try {
        // Check if we're on the sign-in screen
        composeTestRule
            .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
            .fetchSemanticsNode()
        true
      } catch (_: Throwable) {
        try {
          // Or check if we somehow ended up on camera screen
          composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).fetchSemanticsNode()
          Log.d("EndToEndM2", "Sign-in screen not found, but camera screen is ready")
          true
        } catch (_: Throwable) {
          false
        }
      }
    }
  }

  @After
  fun tearDown() {
    // Clean up the system property to avoid affecting other tests
    System.clearProperty("mygarden.e2e")
  }

  @Test
  fun test_end_to_end_m2() {
    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
        .assertIsDisplayed()
        .performClick()
    runBlocking {
      firebaseUtils.initialize()
      firebaseUtils.injectProfileRepository()
      firebaseUtils.signIn()
      firebaseUtils.waitForAuthReady()
    }
    // === NEW PROFILE SCREEN ===
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .performTextInput("Switzerland")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).isDisplayed()
    }

    // goto MyGarden
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(GardenScreenTestTags.USERNAME).isDisplayed()
    }
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.USERNAME)
        .assertIsDisplayed()
        .assertTextContains("John")

    // goto edit profile screen
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).isDisplayed()
    }
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("Ada")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    }
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(GardenScreenTestTags.USERNAME).isDisplayed()
    }
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.USERNAME)
        .assertIsDisplayed()
        .assertTextContains("Ada")

    // goto camera screen
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).performClick()

    // === CAMERA SCREEN ===

    // Wait for camera ready
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).isDisplayed()
    }
    // Take photo
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()

    // === PLANT INFO SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).isDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertTextContains("Rose")

    // See plant location
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TEXT)
        .assertTextContains("OUTDOOR")

    // click on next
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
    }

    // === EDIT PLANT SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).isDisplayed()
    }
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()

    // === GARDEN SCREEN ===
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    }

    // Wait for the garden list to appear in the UI (confirms plant data has loaded)
    composeTestRule.waitUntil(TIMEOUT) {
      try {
        composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).isDisplayed()
        true
      } catch (e: AssertionError) {
        false
      }
    }

    // logout
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON).isDisplayed()
    }

    runBlocking {
      firebaseUtils.signIn()
      firebaseUtils.waitForAuthReady()
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON).performClick()

    // Wait for navigation to be ready after re-login
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).isDisplayed()
    }

    // goto garden
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    }

    // Wait for the garden list to appear
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).isDisplayed()
    }

    val listOfOwnedPlantAfterLogout = runBlocking {
      PlantsRepositoryProvider.repository.getAllOwnedPlants()
    }
    assert(listOfOwnedPlantAfterLogout.size == 1)
    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(listOfOwnedPlantAfterLogout.first())

    // click on plant
    composeTestRule.waitUntil(TIMEOUT) { composeTestRule.onNodeWithTag(plantTag).isDisplayed() }
    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Edit Plant
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).isDisplayed()
    }
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME)
        .assertTextContains(mockPlant.name)
    composeTestRule.waitForIdle()
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
}
