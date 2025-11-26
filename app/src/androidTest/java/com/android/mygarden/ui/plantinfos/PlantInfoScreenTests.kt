package com.android.mygarden.ui.plantinfos

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.printToLog
import com.android.mygarden.R
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.utils.FakePlantRepositoryUtils
import com.android.mygarden.utils.FirestoreProfileTest
import com.android.mygarden.utils.PlantRepositoryType
import com.android.mygarden.utils.TestPlants
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PlantInfoScreenTests : FirestoreProfileTest() {
  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var gatedRepo: GatedPlantsRepository

  private lateinit var context: Context

  val plant = TestPlants.plantInfoPlant

  /**
   * Sets up the PlantInfosScreen composable for testing that comes from the camera screen.
   *
   * @param plant The Plant object to display
   * @param onSavePlant Callback for the "Next" button (default: empty lambda)
   * @param onBackPressed Callback for the back button (default: empty lambda)
   */
  fun setContentFromCamera(
      plant: Plant,
      onSavePlant: (String) -> Unit = {},
      onBackPressed: () -> Unit = {}
  ) {
    composeTestRule.setContent {
      context = LocalContext.current
      PlantInfosScreen(plant, onBackPressed = onBackPressed, onNextPlant = onSavePlant)
    }
    composeTestRule.waitForIdle()
  }

  /**
   * Sets up the PlantInfosScreen composable for testing that comes from the garden screen. This
   * function actually stores the given plant to create a OwnedPlant.
   *
   * @param plant The Plant object to display
   * @param onSavePlant Callback for the "Next" button (default: empty lambda)
   * @param onBackPressed Callback for the back button (default: empty lambda)
   */
  suspend fun setContentFromGarden(
      plant: Plant,
      onSavePlant: (String) -> Unit = {},
      onBackPressed: () -> Unit = {},
      status: PlantHealthStatus = PlantHealthStatus.UNKNOWN
  ) {
    // Store the given plant as a OwnedPlant and give to the PlantInfoScreen a ID
    val repository = PlantsRepositoryLocal()
    val id = "test getOwned id 1"
    val ratio =
        when (status) {
          PlantHealthStatus.SEVERELY_OVERWATERED -> 0.05
          PlantHealthStatus.OVERWATERED -> 0.29
          PlantHealthStatus.HEALTHY -> 0.69
          PlantHealthStatus.SLIGHTLY_DRY -> 0.99
          PlantHealthStatus.NEEDS_WATER -> 1.29
          PlantHealthStatus.SEVERELY_DRY -> 3.0
          PlantHealthStatus.UNKNOWN -> 0.0
        }
    val dayMillisLong = (plant.wateringFrequency * ratio).toLong()
    val timestamp = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(dayMillisLong))
    val ownedPlant = repository.saveToGarden(plant, id, timestamp)
    val vm = PlantInfoViewModel(repository)

    composeTestRule.waitForIdle()
    composeTestRule.setContent {
      context = LocalContext.current
      PlantInfosScreen(plant, ownedPlant.id, vm, onBackPressed, onSavePlant)
    }

    composeTestRule.waitForIdle()
  }

  @Test
  fun allUIComponentsAreDisplayed() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_IMAGE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TAB_ROW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.CONTENT_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun descriptionTabIsSelectedByDefault() {
    setContentFromCamera(plant)
    // Verify description is shown and health info is not
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_HEADER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TEXT).assertDoesNotExist()
  }

  @Test
  fun healthStatusIsDisplayedAfterClickingHealthTab() = runTest {
    setContentFromGarden(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS).assertIsDisplayed()
  }

  @Test
  fun wateringFrequencyIsDisplayedAfterClickingHealthTab() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY_HEADER)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY).assertIsDisplayed()
  }

  /**
   * Check that if the user comes from the camera screen (by default in this test class) the last
   * time watered information is not displayed.
   */
  @Test
  fun lastTimeWateredIsNotDisplayedAfterClickingHealthTabIfWeComeFromTheCamera() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.LAST_TIME_WATERED_HEADER)
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LAST_TIME_WATERED).assertIsNotDisplayed()
  }

  /** Check that if the user comes from the garden screen the health status is displayed. */
  @Test
  fun plantInfoFromGarden_showsPlantHealthStatus() = runTest {
    // We need a repository to store the ownedPlant
    setContentFromGarden(plant, {}, {})

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS).assertIsDisplayed()
  }

  /**
   * Check that if the user comes from the garden screen the last time watered information is
   * displayed.
   */
  @Test
  fun plantInfoFromGarden_showsLastTimeWatered() = runTest {
    // We need a repository to store the ownedPlant
    setContentFromGarden(plant, {}, {})

    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.LAST_TIME_WATERED_HEADER)
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LAST_TIME_WATERED).assertIsDisplayed()
  }

  /**
   * Verifies that when coming from the garden (i.e the ownedPlant id is not null), the node that
   * contains the date of creation is displayed.
   */
  @Test
  fun plantInfoFromGarden_showDateOfCreation() = runTest {
    // We need a repository to store the ownedPlant
    setContentFromGarden(plant, {}, {})

    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_DATE_OF_CREATION)
        .assertIsDisplayed()
  }

  /**
   * Check that if the user comes from the camera screen (by default in this test class) the date of
   * creation information is not displayed.
   */
  @Test
  fun plantInfoFromCamera_doesNotShowDateOfCreation() {
    setContentFromCamera(plant)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_DATE_OF_CREATION)
        .assertIsNotDisplayed()
  }

  @Test
  fun locationTextIsDisplayedAfterClickingLocationTab() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TEXT).assertIsDisplayed()
  }

  @Test
  fun lightExposureDescriptionIsDisplayedAfterClickingLocationTab() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LIGHT_EXPOSURE_TEXT).assertIsDisplayed()
  }

  @Test
  fun switchingToHealthTabHidesDescriptionAndLocationText() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TEXT).assertDoesNotExist()
  }

  @Test
  fun switchingBackToDescriptionTabShowsDescription() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
  }

  @Test
  fun plantNameDisplaysCorrectText() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME).assertTextEquals(plant.name)
  }

  @Test
  fun plantLatinNameDisplaysCorrectText() {
    setContentFromCamera(plant)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
        .assertTextEquals(plant.latinName)
  }

  @Test
  fun descriptionDisplaysCorrectText() {
    setContentFromCamera(plant)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
        .assertTextEquals(plant.description)
  }

  @Test
  fun healthStatusNotDisplayedWhenFromCamera() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS).assertIsNotDisplayed()
  }

  @Test
  fun wateringFrequencyDisplaysCorrectText() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY_HEADER)
        .assertTextEquals(context.getString(R.string.watering_frequency_header))
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY)
        .assertTextEquals(
            context.getString(R.string.watering_frequency_value, plant.wateringFrequency))
  }

  @Test
  fun locationTabDisplaysCorrectText() {
    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.LOCATION_TEXT)
        .assertTextEquals(plant.location.name)
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.LIGHT_EXPOSURE_TEXT)
        .assertTextEquals(plant.lightExposure)
  }

  @Test
  fun needsWaterStatusDisplaysCorrectDescription() = runTest {
    val needsWaterPlant =
        plant.copy(
            healthStatus = PlantHealthStatus.NEEDS_WATER,
            healthStatusDescription = "This plant needs water now.")
    setContentFromGarden(plant = needsWaterPlant, status = PlantHealthStatus.NEEDS_WATER)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals(
            context.getString(R.string.status_label, PlantHealthStatus.NEEDS_WATER.description))
  }

  @Test
  fun severelyDryStatusDisplaysCorrectDescription() = runTest {
    val overwateredPlant =
        plant.copy(
            healthStatus = PlantHealthStatus.SEVERELY_DRY,
            healthStatusDescription = "This plant really needs water.")
    setContentFromGarden(plant = overwateredPlant, status = PlantHealthStatus.SEVERELY_DRY)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS).printToLog("TEST_DEBUG")
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals(
            context.getString(R.string.status_label, PlantHealthStatus.SEVERELY_DRY.description))
  }

  @Test
  fun healthyStatusDisplaysCorrectDescription() = runTest {
    val healthyPlant =
        plant.copy(
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "This plant is healthy!")
    setContentFromGarden(plant = healthyPlant, status = PlantHealthStatus.HEALTHY)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.HEALTH_STATUS)
        .assertTextEquals(
            context.getString(R.string.status_label, PlantHealthStatus.HEALTHY.description))
  }

  @Test
  fun differentWateringFrequencyDisplaysCorrectly() {
    val plantWith7DaysWatering = plant.copy(wateringFrequency = 7)
    setContentFromCamera(plantWith7DaysWatering)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.HEALTH_TAB).performClick()
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.WATERING_FREQUENCY)
        .assertTextEquals(context.getString(R.string.watering_frequency_value, 7))
  }

  @Test
  fun longDescriptionIsScrollable() {
    val plantWithLongDescription =
        plant.copy(
            description =
                "Line 1\n".repeat(100) + "End of long description") // Create very long content
    setContentFromCamera(plantWithLongDescription)

    // Verify the content container is scrollable by checking it exists and is displayed
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.CONTENT_CONTAINER).assertIsDisplayed()

    // The text should be displayed (even if not all visible on screen)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).assertIsDisplayed()

    // Perform scroll action to verify scrollability (this will scroll to make the node visible)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT).performScrollTo()
  }

  @Test
  fun saveButtonTriggersOnSavePlantCallback() {
    gatedRepo.gate.complete(Unit)
    composeTestRule.waitForIdle()

    var savePlantCalled = false
    setContentFromCamera(plant, onSavePlant = { savePlantCalled = true })

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
    setContentFromCamera(plant, onBackPressed = { backPressedCalled = true })

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
    setContentFromCamera(plant)

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

  @Test
  fun tipsButton_isDisplayed() {
    PlantsRepositoryProvider.repository = PlantsRepositoryLocal()

    setContentFromCamera(plant)
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_BUTTON).assertIsDisplayed()
  }

  @Test
  fun clickingTipsButton_showsDialogWithTipsText() {
    // Provide a repo that returns a known tips string
    val fakeTips = "Keep soil slightly moist and provide bright indirect light."

    val fakePlantRepositoryUtils = FakePlantRepositoryUtils(PlantRepositoryType.PlantRepoLocal)
    fakePlantRepositoryUtils.mockPlantCareTips(fakeTips)
    fakePlantRepositoryUtils.setUpMockRepo()

    setContentFromCamera(plant)

    // Click the tips button
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Verify dialog displayed and contains the tips text
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_TEXT).assertIsDisplayed()
    // Check the text equals the expected tips
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_TEXT).assertTextEquals(fakeTips)
  }

  @Test
  fun closingTipsDialog_hidesDialog() {
    // Provide a repo that returns a known tips string
    val fakeTips = "Keep soil slightly moist and provide bright indirect light."
    val fakePlantRepositoryUtils = FakePlantRepositoryUtils(PlantRepositoryType.PlantRepoLocal)
    fakePlantRepositoryUtils.mockPlantCareTips(fakeTips)
    fakePlantRepositoryUtils.setUpMockRepo()

    setContentFromCamera(plant)

    // Open dialog
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Close dialog using the close button
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_CLOSE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Dialog should no longer exist
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_DIALOG).assertDoesNotExist()
  }
}
