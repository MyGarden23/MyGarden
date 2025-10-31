package com.android.mygarden.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
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

  @Before
  fun setUp() {
    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      AppNavHost(navController = controller, startDestination = Screen.Garden.route)
    }
  }

  /** Tests navigation from NewProfile to ChooseAvatar and back after avatar selection. */
  @Test
  fun navigateFromGardenToEditScreenAndReturn() {
    /*
    val repo = PlantsRepositoryProvider.repository
    val ownedPlant = OwnedPlant(id = "ff",
      plant =
        Plant(
          name = "Demo Monstera",
          latinName = "Monstera deliciosa",
          wateringFrequency = 10,
          healthStatus = PlantHealthStatus.SLIGHTLY_DRY),
      lastWatered = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)))

    runBlocking {
      repo.saveToGarden(
        id = "ff",
        plant =
          Plant(
            name = "Demo Monstera",
            latinName = "Monstera deliciosa",
            wateringFrequency = 10,
            healthStatus = PlantHealthStatus.SLIGHTLY_DRY),
        lastWatered = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)))
    }
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()

    // 3. Scroll to and click on the plant card
    val plantTag = GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)

    composeTestRule.onNodeWithTag(plantTag, useUnmergedTree = true)
      .assertIsDisplayed()
      .performClick()

    // 4. Verify that navigation happened to the EditPlant screen
    composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN)
      .assertIsDisplayed()

    // 5. Simulate pressing the system back button
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE)
      .assertIsDisplayed()
      .performClick()

    // 6. Verify we are back on the Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN)
      .assertIsDisplayed() */
  }
}
