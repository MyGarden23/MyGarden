package com.android.mygarden.ui.navigation.navS4

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.ui.camera.RequiresCamera
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import junit.framework.TestCase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation tests for the flow: PlantInfo → EditPlant → Garden.
 *
 * This class verifies the new navigation behavior where saving a plant from PlantInfo navigates to
 * EditPlant (instead of directly to Garden), allowing the user to set the description and last
 * watered date.
 */
@RequiresCamera
@RunWith(AndroidJUnit4::class)
class NavigationS4TestsEditPlantFromPlantInfo {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Automatically grants camera permission before tests run. Essential for camera functionality
   * testing without user interaction prompts.
   */
  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

  private lateinit var navController: NavHostController

  @Before
  fun setUp() {
    PlantsRepositoryProvider.repository = PlantsRepositoryLocal()

    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      MyGardenTheme {
        // Start at Camera screen because we need the Camera in the stack to navigate to Camera from
        // EditPlant with Back button
        AppNavHost(navController = controller, startDestination = Screen.Camera.route)
      }
    }
    composeTestRule.waitForIdle()
    // Go to PlantInfo because the objective of this test is not to verify the Camera → PlantInfo
    // navigation
    composeTestRule.runOnUiThread { navController.navigate(Screen.PlantInfo.route) }
    composeTestRule.waitForIdle()
  }

  @After
  fun cleanUp() {
    // Clean up any plants that were saved during tests
    val repo = PlantsRepositoryProvider.repository
    runTest {
      val allPlants = repo.getAllOwnedPlants()
      allPlants.forEach { repo.deleteFromGarden(it.id) }
    }
  }

  /**
   * Helper function to navigate from PlantInfo screen to EditPlant screen.
   *
   * This function performs the common steps shared across multiple tests: verifies PlantInfo screen
   * is displayed, clicks the "Next" button, and verifies navigation to EditPlant screen.
   */
  private fun navigateFromPlantInfoToEditPlant() {
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()
  }

  /**
   * Tests the complete flow from PlantInfo to EditPlant to Garden.
   *
   * User journey: 1. User sees plant info after taking a photo 2. User clicks "Next" button to save
   * plant 3. User is navigated to EditPlant screen 4. User clicks "Save" on EditPlant 5. User is
   * navigated to Garden screen
   */
  @Test
  fun navigateFromPlantInfoToEditPlantAndThenToGardenBySaving() {
    // Start on PlantInfo screen
    composeTestRule.waitForIdle()
    navigateFromPlantInfoToEditPlant()
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).assertIsDisplayed()

    // Click "Save" on EditPlant screen
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()

    // Verify navigation to Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
  }

  /**
   * Tests navigation from PlantInfo to EditPlant, then back to Camera using back button.
   *
   * User journey:
   * 1. User sees plant info
   * 2. User clicks "Next" to save
   * 3. User is on EditPlant
   * 4. User clicks back button
   * 5. User returns to Camera (PlantInfo is removed from backstack, and plant is deleted)
   */
  @Test
  fun navigateFromPlantInfoToEditPlantAndBackToCameraByPressingBack() {
    // Start on PlantInfo screen
    composeTestRule.waitForIdle()
    navigateFromPlantInfoToEditPlant()

    // Click back button on EditPlant
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify we're back on Camera screen (not PlantInfo, since it was removed from backstack)
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_SCREEN).assertIsDisplayed()

    // Verify the plant was deleted (check that no plant exists in repository)
    val repo = PlantsRepositoryProvider.repository
    runTest {
      val allPlants = repo.getAllOwnedPlants()
      TestCase.assertTrue(
          "Plant should have been deleted when going back from EditPlant", allPlants.isEmpty())
    }
  }
}
