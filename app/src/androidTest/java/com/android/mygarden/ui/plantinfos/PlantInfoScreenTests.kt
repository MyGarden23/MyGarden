package com.android.mygarden.ui.plantinfos

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.android.mygarden.R
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.utils.FirestoreProfileTest
import org.junit.Rule
import org.junit.Test

class PlantInfoScreenTests : FirestoreProfileTest() {
  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var gatedRepo: GatedPlantsRepository

  private lateinit var context: Context

  val plant: Plant =
      Plant(
          name = "test_plant",
          image = null,
          latinName = "testinus_plantus",
          description = "This is a test plant.",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "This plant is healthy.",
          wateringFrequency = 1,
      )

  /**
   * Sets up the PlantInfosScreen composable for testing.
   *
   * @param plant The Plant object to display
   * @param onSavePlant Callback for the "Next" button (default: empty lambda)
   * @param onBackPressed Callback for the back button (default: empty lambda)
   */
  fun setContent(plant: Plant, onSavePlant: (String) -> Unit = {}, onBackPressed: () -> Unit = {}) {
    composeTestRule.setContent {
      context = LocalContext.current
      PlantInfosScreen(plant, onBackPressed = onBackPressed, onNextPlant = onSavePlant)
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun allUIComponentsAreDisplayed() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TAB_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.CONTENT_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun descriptionTabIsSelectedByDefault() {
    setContent(plant)
    // Verify description is shown and health info is not
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS).assertDoesNotExist()
  }

  @Test
  fun healthStatusDescriptionIsDisplayedAfterClickingHealthTab() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS_DESCRIPTION)
        .assertIsDisplayed()
  }

  @Test
  fun healthStatusIsDisplayedAfterClickingHealthTab() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS).assertIsDisplayed()
  }

  @Test
  fun wateringFrequencyIsDisplayedAfterClickingHealthTab() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY).assertIsDisplayed()
  }

  @Test
  fun switchingToHealthTabHidesDescription() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertDoesNotExist()
  }

  @Test
  fun switchingBackToDescriptionTabShowsDescription() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
  }

  @Test
  fun plantNameDisplaysCorrectText() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertTextEquals(plant.name)
  }

  @Test
  fun plantLatinNameDisplaysCorrectText() {
    setContent(plant)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertTextEquals(plant.latinName)
  }

  @Test
  fun descriptionDisplaysCorrectText() {
    setContent(plant)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
        .assertTextEquals(plant.description)
  }

  @Test
  fun healthStatusDescriptionDisplaysCorrectText() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS_DESCRIPTION)
        .assertTextEquals(plant.healthStatusDescription)
  }

  @Test
  fun healthStatusDisplaysCorrectText() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals(context.getString(R.string.status_label, plant.healthStatus.description))
  }

  @Test
  fun wateringFrequencyDisplaysCorrectText() {
    setContent(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY)
        .assertTextEquals(context.getString(R.string.watering_frequency, plant.wateringFrequency))
  }

  @Test
  fun needsWaterStatusDisplaysCorrectDescription() {
    val needsWaterPlant =
        plant.copy(
            healthStatus = PlantHealthStatus.NEEDS_WATER,
            healthStatusDescription = "This plant needs water now.")
    setContent(needsWaterPlant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals(
            context.getString(R.string.status_label, PlantHealthStatus.NEEDS_WATER.description))
  }

  @Test
  fun overwateredStatusDisplaysCorrectDescription() {
    val overwateredPlant =
        plant.copy(
            healthStatus = PlantHealthStatus.OVERWATERED,
            healthStatusDescription = "This plant has too much water.")
    setContent(overwateredPlant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals(
            context.getString(R.string.status_label, PlantHealthStatus.OVERWATERED.description))
  }

  @Test
  fun unknownStatusDisplaysCorrectDescription() {
    val unknownPlant =
        plant.copy(
            healthStatus = PlantHealthStatus.UNKNOWN,
            healthStatusDescription = "We don't know the status of this plant.")
    setContent(unknownPlant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals(
            context.getString(R.string.status_label, PlantHealthStatus.UNKNOWN.description))
  }

  @Test
  fun healthStatusDescriptionChangesWithDifferentPlant() {
    val customDescription = "Custom health description for testing."
    val customPlant = plant.copy(healthStatusDescription = customDescription)
    setContent(customPlant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS_DESCRIPTION)
        .assertTextEquals(customDescription)
  }

  @Test
  fun differentWateringFrequencyDisplaysCorrectly() {
    val plantWith7DaysWatering = plant.copy(wateringFrequency = 7)
    setContent(plantWith7DaysWatering)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY)
        .assertTextEquals(context.getString(R.string.watering_frequency, 7))
  }

  @Test
  fun longDescriptionIsScrollable() {
    val plantWithLongDescription =
        plant.copy(
            description =
                "Line 1\n".repeat(100) + "End of long description") // Create very long content
    setContent(plantWithLongDescription)

    // Verify the content container is scrollable by checking it exists and is displayed
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.CONTENT_CONTAINER).assertIsDisplayed()

    // The text should be displayed (even if not all visible on screen)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()

    // Perform scroll action to verify scrollability (this will scroll to make the node visible)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).performScrollTo()
  }

  @Test
  fun healthTabContentIsAlsoScrollable() {
    val plantWithLongHealthDescription =
        plant.copy(healthStatusDescription = "Health info\n".repeat(100))
    setContent(plantWithLongHealthDescription)

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS_DESCRIPTION)
        .performScrollTo()
  }

  @Test
  fun saveButtonTriggersOnSavePlantCallback() {
    gatedRepo.gate.complete(Unit)
    composeTestRule.waitForIdle()

    var savePlantCalled = false
    setContent(plant, onSavePlant = { savePlantCalled = true })

    // Wait for initialization to complete
    composeTestRule.waitForIdle()

    // Click the save button
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

    // Wait for the action to complete
    composeTestRule.waitForIdle()

    // Verify that the callback was called
    assert(savePlantCalled) { "onSavePlant callback should have been called" }
  }

  @Test
  fun backButtonTriggersOnBackPressedCallback() {
    var backPressedCalled = false
    setContent(plant, onBackPressed = { backPressedCalled = true })

    // Wait for initialization to complete
    composeTestRule.waitForIdle()

    // Click the back button
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).performClick()

    // Wait for the action to complete
    composeTestRule.waitForIdle()

    // Verify that the callback was called
    assert(backPressedCalled) { "onBackPressed callback should have been called" }
  }

  @org.junit.Before
  fun installRepo() {
    val local = PlantsRepositoryLocal()
    gatedRepo = GatedPlantsRepository(local)
    PlantsRepositoryProvider.repository = gatedRepo
  }

  @OptIn(ExperimentalTestApi::class)
  @Test
  fun nextButton_showsLoading_untilWorkCompletes() {
    setContent(plant)

    // Click → VM put isSaving=true and bloc one gate.await()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

    // Wait loader
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(PlantInfoScreenTestTags.NEXT_BUTTON_LOADING, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON_LOADING, useUnmergedTree = true)
        .assertIsDisplayed()

    gatedRepo.gate.complete(Unit)
    composeTestRule.waitForIdle()
  }
}
