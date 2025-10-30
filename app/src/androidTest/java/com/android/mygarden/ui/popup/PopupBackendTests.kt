package com.android.mygarden.ui.popup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.MyGardenApp
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.ui.authentication.SignInScreenTestTags
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PopupBackendTests {

  @get:Rule val rule = createComposeRule()

  // Some fictional plants to use for the tests
  val almostThirstyPlant =
      Plant(
          name = "Water?",
          image = null,
          latinName = "laurem ipsum",
          description = "edge plant that will soon be thirsty",
          healthStatus = PlantHealthStatus.UNKNOWN,
          healthStatusDescription = "we don't care about the initial, will be recomputed soon",
          wateringFrequency = 1)
  val thirstyPlant =
      Plant(
          name = "WATER!",
          image = null,
          latinName = "laurem ipsum",
          description = "already thirsty plant!",
          healthStatus = PlantHealthStatus.NEEDS_WATER,
          healthStatusDescription = "already ?!",
          wateringFrequency = 1)

  private lateinit var repo: PlantsRepository

  /** Sets the repo as a local one and ensures the provider's repo is aligned with this new one. */
  @Before
  fun repoSetUp() {
    repo = PlantsRepositoryLocal()
    PlantsRepositoryProvider.repository = repo
  }

  /**
   * Took the idea from [com.android.mygarden.ui.garden.GardenScreenTests] to populate the repo with
   * an initial list and set the content to create the app to test
   */
  fun setContent(initialList: List<Plant> = emptyList()) {
    runTest { initialList.forEach { repo.saveToGarden(it, repo.getNewId(), Timestamp(1)) } }
    rule.setContent { MyGardenTheme { MyGardenApp() } }
    rule.waitForIdle()
  }

  /** Ensures to clear the repo at the end of each test for consistency */
  @After
  fun eraseFromRepo() {
    runTest { repo.getAllOwnedPlants().forEach { p -> repo.deleteFromGarden(p.id) } }
  }

  /** To be called to ensure all components of the pop-up are displayed */
  fun ComposeTestRule.wholePopupIsDisplayed() {
    onNodeWithTag(PopupScreenTestTags.CARD).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.TITLE).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsDisplayed()
  }

  /** To be called to ensure all components of the pop-up are NOT displayed */
  fun ComposeTestRule.wholePopupIsNotDisplayed() {
    onNodeWithTag(PopupScreenTestTags.CARD).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.TITLE).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsNotDisplayed()
  }

  /** Saves a plant in repo that is already in NEEDS_WATER status */
  private fun saveThirstyPlantInRepo() {
    runTest {
      repo.saveToGarden(thirstyPlant, repo.getNewId(), Timestamp(1))
      rule.waitForIdle()
    }
  }

  /**
   * Saves a plant in the repo that will have its status become NEEDS_WATER if it's recalculated at
   * least 5 seconds after the call to this function
   */
  private fun saveAlmostThirstyPlantInRepo() {
    val justMoreThanADay =
        Timestamp(
            System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(1) - TimeUnit.SECONDS.toMillis(5)))
    runTest { repo.saveToGarden(almostThirstyPlant, repo.getNewId(), justMoreThanADay) }
  }

  /** Tests that the pop-up is not displayed when the repo is empty */
  @Test
  fun noPopupWithEmptyRepo() {
    setContent()
    rule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO).assertIsDisplayed()
    rule.wholePopupIsNotDisplayed()
  }

  /** Tests that the popup gets displayed when the repo receives a thirsty plant */
  @Test
  fun popupPopsWhenThirstyPlantIsSavedInRepo() {
    setContent()
    rule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO).assertIsDisplayed()
    saveThirstyPlantInRepo()
    rule.wholePopupIsDisplayed()
  }

  /** Tests that the dismiss button works correcly */
  @Test
  fun canDismissPopupByClickingOnDismissButton() {
    setContent()
    saveThirstyPlantInRepo()
    rule.wholePopupIsDisplayed()
    rule.onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).performClick()
    rule.wholePopupIsNotDisplayed()
  }

  /** Tests that the confirm button makes the pop-up disappear */
  @Test
  fun popupLeavesScreenWhenClickingOnConfirmButton() {
    setContent()
    rule.wholePopupIsNotDisplayed()
    saveThirstyPlantInRepo()
    rule.wholePopupIsDisplayed()
    rule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).performClick()
    rule.wholePopupIsNotDisplayed()
  }

  /** Tests that the confirm button makes the user navigate to the garden */
  @Test
  fun popupNavToGardenWhenClickingOnConfirmButton() {
    setContent()
    rule.wholePopupIsNotDisplayed()
    saveThirstyPlantInRepo()
    rule.wholePopupIsDisplayed()
    rule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).performClick()
    rule.wholePopupIsNotDisplayed()
    rule.onNodeWithTag(GardenScreenTestTags.TITLE).assertIsDisplayed()
  }

  /**
   * Tests that the pop-up is correctly displayed when a plant's status is updated but was already
   * on the repo
   */
  @Test
  fun popupPopsWhenPlantsStatusAreUpdatedViaTimestamps() {
    setContent()
    rule.wholePopupIsNotDisplayed()
    saveAlmostThirstyPlantInRepo()
    Thread.sleep(7000)
    // to trigger the status update
    runTest { repo.getAllOwnedPlants() }
    rule.wholePopupIsDisplayed()
  }
}
