package com.android.mygarden.ui.navigation.navS4

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.PlantsRepositoryProvider
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
@RunWith(AndroidJUnit4::class)
class NavigationS4TestsEditPlantFromPlantInfo {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController

  @Before
  fun setUp() {
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      MyGardenTheme {
        // Start at PlantInfo screen
        AppNavHost(navController = controller, startDestination = Screen.PlantInfo.route)
      }
    }
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
   * Tests navigation from PlantInfo to EditPlant, then back to PlantInfo using back button.
   *
   * User journey: 1. User sees plant info 2. User clicks "Next" to save 3. User is on EditPlant 4.
   * User clicks back button 5. User returns to PlantInfo (and plant is deleted)
   */
  @Test
  fun navigateFromPlantInfoToEditPlantAndBackToPlantInfoByPressingBack() {
    // Start on PlantInfo screen
    composeTestRule.waitForIdle()
    navigateFromPlantInfoToEditPlant()

    // Click back button on EditPlant
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()

    // Verify we're back on PlantInfo screen
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    // Verify the plant was deleted (check that no plant exists in repository)
    val repo = PlantsRepositoryProvider.repository
    runTest {
      val allPlants = repo.getAllOwnedPlants()
      TestCase.assertTrue(
          "Plant should have been deleted when going back from EditPlant", allPlants.isEmpty())
    }
  }
}
