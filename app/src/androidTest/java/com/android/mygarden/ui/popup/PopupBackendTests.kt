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

  @get:Rule
  // val rule = createAndroidComposeRule<MainActivity>()
  val rule = createComposeRule()

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

  @Before
  fun repoSetUp() {
    repo = PlantsRepositoryLocal()
    PlantsRepositoryProvider.repository = repo
  }

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

  fun ComposeTestRule.wholePopupIsDisplayed() {
    onNodeWithTag(PopupScreenTestTags.CARD).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.TITLE).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.wholePopupIsNotDisplayed() {
    onNodeWithTag(PopupScreenTestTags.CARD).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.TITLE).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsNotDisplayed()
  }

  fun ComposeTestRule.saveThirstyPlantInRepo() {
    runTest {
      repo.saveToGarden(thirstyPlant, repo.getNewId(), Timestamp(1))
      rule.waitForIdle()
    }
  }

  fun ComposeTestRule.saveAlmostThirstyPlantInRepo() {
    val justMoreThanADay =
        Timestamp(
            System.currentTimeMillis() - (TimeUnit.DAYS.toMillis(1) - TimeUnit.SECONDS.toMillis(5)))
    runTest { repo.saveToGarden(almostThirstyPlant, repo.getNewId(), justMoreThanADay) }
  }

  @Test
  fun noPopupWithEmptyRepo() {
    setContent()
    rule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO).assertIsDisplayed()
    rule.wholePopupIsNotDisplayed()
  }

  @Test
  fun popupPopsWhenThirstyPlantIsSavedInRepo() {
    setContent()
    rule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO).assertIsDisplayed()
    rule.saveThirstyPlantInRepo()
    rule.wholePopupIsDisplayed()
  }

  @Test
  fun canDismissPopupByClickingOnDismissButton() {
    setContent()
    rule.saveThirstyPlantInRepo()
    rule.wholePopupIsDisplayed()
    rule.onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).performClick()
    rule.wholePopupIsNotDisplayed()
  }

  @Test
  fun popupLeavesScreenWhenClickingOnConfirmButton() {
    setContent()
    rule.wholePopupIsNotDisplayed()
    rule.saveThirstyPlantInRepo()
    rule.wholePopupIsDisplayed()
    rule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).performClick()
    rule.wholePopupIsNotDisplayed()
  }

  @Test
  fun popupNavToGardenWhenClickingOnConfirmButton() {
    setContent()
    rule.wholePopupIsNotDisplayed()
    rule.saveThirstyPlantInRepo()
    rule.wholePopupIsDisplayed()
    rule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).performClick()
    rule.wholePopupIsNotDisplayed()
    rule.onNodeWithTag(GardenScreenTestTags.TITLE).assertIsDisplayed()
  }

  @Test
  fun popupPopsWhenPlantsStatusAreUpdatedViaTimestamps() {
    setContent()
    rule.wholePopupIsNotDisplayed()
    rule.saveAlmostThirstyPlantInRepo()
    Thread.sleep(7000)
    // to trigger the status update
    runTest { repo.getAllOwnedPlants() }
    rule.wholePopupIsDisplayed()
  }
}
