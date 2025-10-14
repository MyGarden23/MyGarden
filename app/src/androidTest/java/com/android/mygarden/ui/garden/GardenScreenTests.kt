package com.android.mygarden.ui.garden

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import java.sql.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GardenScreenTests {

  @get:Rule val composeTestRule = createComposeRule()

  // Some fictional plants to play with
  val plant1 =
      Plant(
          "hello",
          null,
          "laurem ipsum",
          "beautiful plant",
          PlantHealthStatus.HEALTHY,
          "is healthy",
          2)
  val plant2 =
      Plant(
          "world",
          null,
          "laurem ipsum",
          "even more beautiful plant",
          PlantHealthStatus.NEEDS_WATER,
          "is thirsty",
          8)
  val plant3 =
      Plant(
          "Poseidon",
          null,
          "laurem ipsum",
          "water ++ plant",
          PlantHealthStatus.OVERWATERED,
          "is full",
          8)
  val plant4 =
      Plant(
          "Anonymous",
          null,
          "laurem ipsum",
          "who is this guy",
          PlantHealthStatus.UNKNOWN,
          "is ?",
          8)

  private lateinit var repo: PlantsRepository

  /**
   * Sets the [repo] as a local repository for testing and sets the provider's repo to this one to
   * ensure that the local repo of the test class is the one used by the screen instance
   */
  @Before
  fun setUp() {
    repo = PlantsRepositoryLocal()
    PlantsRepositoryProvider.repository = repo
  }

  /**
   * To be called in each of the test in order to have a repository that contains either nothing or
   * a fictional list of plants
   *
   * @param initialOwnedPlants the list wanted in the repo for the current test
   */
  fun setContent(initialOwnedPlants: List<Plant> = emptyList()) {
    runTest { initialOwnedPlants.forEach { repo.saveToGarden(it, repo.getNewId(), Timestamp(1)) } }
    // Buttons have no use : tests are for the garden screen in isolation
    composeTestRule.setContent { GardenScreen(onEditProfile = {}, onAddPlant = {}) }
    composeTestRule.waitForIdle()
  }

  /** Ensures to clear the repo at the end of each test for consistency */
  @After
  fun eraseFromRepo() {
    runTest { repo.getAllOwnedPlants().forEach { p -> repo.deleteFromGarden(p.id) } }
  }

  fun ComposeTestRule.userRowIsDisplayed() {
    onNodeWithTag(GardenScreenTestTags.USER_PROFILE_PICTURE).assertIsDisplayed()
    onNodeWithTag(GardenScreenTestTags.USERNAME).assertIsDisplayed()
    onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.allPlantsAreDisplayed() {
    runTest {
      repo.getAllOwnedPlants().forEach { p ->
        onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlant(p)).assertIsDisplayed()
      }
    }
  }

  @Test
  fun correctDisplayWhenEmptyGarden() {
    setContent()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.userRowIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EMPTY_GARDEN_MSG).assertIsDisplayed()
  }

  @Test
  fun correctDisplayWhenNonEmptyGarden() {
    val plants = listOf(plant1, plant2, plant3, plant4)
    setContent(plants)
    composeTestRule.onNodeWithTag(GardenScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.userRowIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EMPTY_GARDEN_MSG).assertIsNotDisplayed()
    composeTestRule.allPlantsAreDisplayed()
  }

  @Test
  fun buttonsAreClickable() {
    setContent()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.ADD_PLANT_FAB).assertIsEnabled()
  }
}
