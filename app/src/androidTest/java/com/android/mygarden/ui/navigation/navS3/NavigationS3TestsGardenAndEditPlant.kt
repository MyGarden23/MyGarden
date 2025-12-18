package com.android.mygarden.ui.navigation.navS3

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.LikesRepositoryProvider
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.editPlant.DeletePlantPopupTestTags
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.utils.FakeLikesRepository
import com.android.mygarden.utils.FakeProfileRepository
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation tests for the flows of Garden, PlantInfo and EditPlant screens. This class verifies
 * correct navigation behavior and state handling.
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

  /**
   * A function ran before each tests to do the appropriate set up. Saving a plant to the Garden and
   * starting the tests at the garden screen.
   */
  @Before
  fun setUp() {
    LikesRepositoryProvider.repository = FakeLikesRepository()
    ProfileRepositoryProvider.repository = FakeProfileRepository()
    composeTestRule.setContent {
      PlantsRepositoryProvider.repository = PlantsRepositoryLocal()
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

  /** Ensuring the repo is empty at the end of each tests. */
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
  fun navigateFromGardenToPlantInfoToEditScreenAndReturnWithSaving() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
        .assertIsDisplayed()

    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)

    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Verify that navigation happened to the PlantInfo screen
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).performClick()

    // Verify that navigation happened to the EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))
    // Save the plant to the garden
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE)
        .assertIsDisplayed()
        .performClick()

    // Verify we are back on the Garden screen
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
        .assertIsDisplayed()
  }

  /** Tests navigation from the Garden to EditPlant and return by deleting. */
  @Test
  fun navigateFromGardenToEditScreenAndReturnWithDeleting() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
        .assertIsDisplayed()

    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)

    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Verify that navigation happened to the PlantInfo screen
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).performClick()

    // Verify that navigation happened to the EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    // Delete the plant
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE)
        .assertIsDisplayed()
        .performClick()

    composeTestRule
        .onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    // Verify we are back on the Garden screen
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
        .assertIsDisplayed()
  }

  /** Tests navigation from the Garden to EditPlant and return by pressing back button. */
  @Test
  fun navigateFromGardenToEditScreenAndReturnWithBackArrow() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
        .assertIsDisplayed()

    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)

    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Verify that navigation happened to the PlantInfo screen
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).performClick()

    // Verify that navigation happened to the EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    // Go back to Plant info screen
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Verify we are back on the PlantInfo screen
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    // Go back to Garden screen
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Verify we are back on the Garden screen
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
        .assertIsDisplayed()
  }

  /**
   * Tests navigation from the Garden to EditPlant then press the delete button but return to
   * EditPlant by keeping the plan in the garden.
   */
  @Test
  fun navigateFromGardenToEditScreenAndKeepInGarden() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
        .assertIsDisplayed()

    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)

    composeTestRule.onNodeWithTag(plantTag).assertIsDisplayed().performClick()

    // Verify that navigation happened to the PlantInfo screen
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).performClick()

    // Verify that navigation happened to the EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))

    // Keep the plant
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE)
        .assertIsDisplayed()
        .performClick()

    composeTestRule
        .onNodeWithTag(DeletePlantPopupTestTags.CANCEL_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    // Verify we are back on the Edit plant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).assertIsDisplayed()
  }
}
