package com.android.mygarden.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import kotlinx.coroutines.runBlocking
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

  private val testPlant =
      Plant(
          name = "Test Rose",
          latinName = "Rosa testus",
          description = "A beautiful test rose",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "This plant is healthy",
          wateringFrequency = 7)

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
    runBlocking {
      val allPlants = repo.getAllOwnedPlants()
      allPlants.forEach { repo.deleteFromGarden(it.id) }
    }
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
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify navigation to EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

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
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    // Click the "Next" button to save the plant
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify navigation to EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    // Click back button on EditPlant
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.GO_BACK_BUTTON).performClick()

    // Verify we're back on PlantInfo screen
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    // Verify the plant was deleted (check that no plant exists in repository)
    val repo = PlantsRepositoryProvider.repository
    runBlocking {
      val allPlants = repo.getAllOwnedPlants()
      assert(allPlants.isEmpty()) {
        "Plant should have been deleted when going back from EditPlant"
      }
    }
  }

  /**
   * Tests navigation from PlantInfo to EditPlant, then delete and go to Garden.
   *
   * User journey: 1. User sees plant info 2. User clicks "Next" to save 3. User is on EditPlant 4.
   * User clicks "Delete" button 5. User is navigated to Garden (plant is deleted)
   */
  @Test
  fun navigateFromPlantInfoToEditPlantAndDeletePlant() {
    // Start on PlantInfo screen
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    // Click the "Next" button to save the plant
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify navigation to EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    // Click delete button on EditPlant
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()

    // Verify navigation to Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()

    // Verify the plant was deleted
    val repo = PlantsRepositoryProvider.repository
    runBlocking {
      val allPlants = repo.getAllOwnedPlants()
      assert(allPlants.isEmpty()) { "Plant should have been deleted" }
    }
  }
}
