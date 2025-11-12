package com.android.mygarden.zendToEnd

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.MainActivity
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.ui.camera.CameraScreenTestTags
import com.android.mygarden.ui.camera.RequiresCamera
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.profile.ProfileScreenTestTags
import com.android.mygarden.utils.FakePlantRepositoryUtils
import com.android.mygarden.utils.FirestoreProfileTest
import com.android.mygarden.utils.PlantRepositoryType
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RequiresCamera
@RunWith(AndroidJUnit4::class)
class EndToEndM2 : FirestoreProfileTest() {
  companion object {
    init {
      // Set system property BEFORE the compose rule is created
      System.setProperty("mygarden.e2e", "true")
    }
  }

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  /**
   * Automatically grants camera permission before tests run. Essential for camera functionality
   * testing without user interaction prompts.
   */
  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

  //TODO add rule for notification permission

  private val TIMEOUT = 10_000L

  private val mockPlant = Plant(name = "Rose", latinName = "Rosa")

  @Before
  override fun setUp() {
    super.setUp()
    // Set up any necessary configurations or states before each test
    val fakePlantRepoUtils = FakePlantRepositoryUtils(PlantRepositoryType.PlantRepoFirestore)
    fakePlantRepoUtils.mockIdentifyPlant(mockPlant)
    fakePlantRepoUtils.setUpMockRepo()
  }

  @After
  override fun tearDown() {
    super.tearDown()
    // Clean up the system property to avoid affecting other tests
    System.clearProperty("mygarden.e2e")
  }

  @Test
  fun test_end_to_end_m2() {
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
      composeTestRule.waitUntil(TIMEOUT) {
        composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
      }
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
      composeTestRule
        .onNodeWithTag(GardenScreenTestTags.USERNAME)
        .assertIsDisplayed()
        .assertTextContains("John")

      // goto edit profile screen
      composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextClearance()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("Ada")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).isDisplayed()
    }
    composeTestRule.onNodeWithTag(GardenScreenTestTags.USERNAME).assertIsDisplayed().assertTextContains("Ada")


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
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).isDisplayed()
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertTextContains("Rose")
    }
  }