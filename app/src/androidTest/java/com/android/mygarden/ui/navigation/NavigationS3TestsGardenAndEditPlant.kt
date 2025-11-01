package com.android.mygarden.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.garden.GardenScreenTestTags
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation tests for the flows of Garden and EditPlant screens. This class verifies correct
 * navigation behavior and state handling.
 */
@RunWith(AndroidJUnit4::class)
class NavigationS3TestsGardenAndEditPlant {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController

  private val ownedPlant =
      OwnedPlant(
          id = "1",
          plant =
              Plant(
                  name = "Demo Monstera",
                  latinName = "Monstera deliciosa",
                  wateringFrequency = 10,
                  healthStatus = PlantHealthStatus.HEALTHY),
          lastWatered = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)))

  @Before
  fun setUp() {
    composeTestRule.setContent {
      val repo = PlantsRepositoryProvider.repository

      runBlocking {
        repo.saveToGarden(
            id = "1",
            plant =
                Plant(
                    name = "Demo Monstera",
                    latinName = "Monstera deliciosa",
                    wateringFrequency = 10,
                    healthStatus = PlantHealthStatus.HEALTHY),
            lastWatered = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)))
      }
      val controller = rememberNavController()
      navController = controller
      AppNavHost(navController = controller, startDestination = Screen.Garden.route)
    }
  }

  @After
  fun clearRepo() {
    val repo = PlantsRepositoryProvider.repository
    runBlocking {
      if (repo.getAllOwnedPlants().isNotEmpty()) {
        repo.deleteFromGarden("1")
      }
    }
  }

  /** Tests navigation from the Garden to EditPlant and return by saving. */
  @Test
  fun navigateFromGardenToEditScreenAndReturnWithSaving() {
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()

    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)

    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Verify that navigation happened to the EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    // Save the plant to the garden
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE)
        .assertIsDisplayed()
        .performClick()

    // Verify we are back on the Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
  }

  /** Tests navigation from the Garden to EditPlant and return by deleting. */
  @Test
  fun navigateFromGardenToEditScreenAndReturnWithDeleting() {
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()

    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)

    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Verify that navigation happened to the EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    // Delete the plant
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE)
        .assertIsDisplayed()
        .performClick()

    // Verify we are back on the Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
  }

  /** Tests navigation from the Garden to EditPlant and return by pressing back button. */
  @Test
  fun navigateFromGardenToEditScreenAndReturnWithBackArrow() {
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()

    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)

    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Verify that navigation happened to the EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    // Go back to garden
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.GO_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Verify we are back on the Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
  }
}
