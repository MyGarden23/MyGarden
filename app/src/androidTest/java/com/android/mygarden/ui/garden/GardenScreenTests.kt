package com.android.mygarden.ui.garden

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.CustomColors
import com.android.mygarden.ui.theme.ExtendedTheme
import com.android.mygarden.ui.theme.MyGardenTheme
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
          10)
  val plant2 =
      Plant(
          "world",
          null,
          "laurem ipsum",
          "even more beautiful plant",
          PlantHealthStatus.NEEDS_WATER,
          "is thirsty",
          10)
  val plant3 =
      Plant(
          "Poseidon",
          null,
          "laurem ipsum",
          "water ++ plant",
          PlantHealthStatus.OVERWATERED,
          "is full",
          10)
  val plant4 =
      Plant(
          "Anonymous",
          null,
          "laurem ipsum",
          "who is this guy",
          PlantHealthStatus.UNKNOWN,
          "is ?",
          10)

  /** Fake profile local repository used to test the viewModel/profile interactions */
  private class FakeProfileRepository(
      initialProfile: Profile? =
          Profile(
              firstName = "Test",
              lastName = "User",
              gardeningSkill = GardeningSkill.BEGINNER,
              favoritePlant = "Rose",
              country = "Switzerland",
              hasSignedIn = true,
              avatar = Avatar.A1)
  ) : ProfileRepository {

    private val flow = MutableStateFlow(initialProfile)

    override fun getCurrentUserId(): String = "fake-uid"

    override fun getProfile(): Flow<Profile?> = flow

    override suspend fun saveProfile(profile: Profile) {
      flow.value = profile
    }
  }

  private lateinit var plantsRepo: PlantsRepository
  private lateinit var profileRepo: ProfileRepository

  /**
   * Sets the [plantsRepo] as a local repository for testing and sets the provider's repo to this
   * one to ensure that the local repo of the test class is the one used by the screen instance
   */
  @Before
  fun setUp() {
    plantsRepo = PlantsRepositoryLocal()
    profileRepo = FakeProfileRepository()
    ProfileRepositoryProvider.repository = profileRepo
    PlantsRepositoryProvider.repository = plantsRepo
  }

  /**
   * To be called in each of the test in order to have a repository that contains either nothing or
   * a fictional list of plants
   *
   * @param initialOwnedPlants the list wanted in the repo for the current test
   */
  fun setContent(initialOwnedPlants: List<Plant> = emptyList()) {
    runTest {
      initialOwnedPlants.forEach {
        plantsRepo.saveToGarden(
            it,
            plantsRepo.getNewId(),
            Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)))
      }
    }
    // Buttons have no use : tests are for the garden screen in isolation
    composeTestRule.setContent { GardenScreen(onEditProfile = {}, onAddPlant = {}) }
    composeTestRule.waitForIdle()
  }

  /** Ensures to clear the repo at the end of each test for consistency */
  @After
  fun eraseFromRepo() {
    runTest { plantsRepo.getAllOwnedPlants().forEach { p -> plantsRepo.deleteFromGarden(p.id) } }
  }

  /** Ensures that all profile-related components are currently displayed */
  fun ComposeTestRule.userRowIsDisplayed() {
    onNodeWithTag(GardenScreenTestTags.USER_PROFILE_PICTURE).assertIsDisplayed()
    onNodeWithTag(GardenScreenTestTags.USERNAME).assertIsDisplayed()
    onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsDisplayed()
  }

  /** Ensures that all plants currently on the [plantsRepo] are displayed on the screen */
  fun ComposeTestRule.allPlantsAreDisplayed() {
    runTest {
      plantsRepo.getAllOwnedPlants().forEach { p ->
        onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlant(p)).assertIsDisplayed()
        onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlantName(p), useUnmergedTree = true)
            .assertIsDisplayed()
        onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlantStatus(p), useUnmergedTree = true)
            .assertIsDisplayed()
        onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlantImage(p), useUnmergedTree = true)
            .assertIsDisplayed()
        onNodeWithTag(
                GardenScreenTestTags.getTestTagForOwnedPlantLatinName(p), useUnmergedTree = true)
            .assertIsDisplayed()
        onNodeWithTag(
                GardenScreenTestTags.getTestTagForOwnedPlantWaterButton(p), useUnmergedTree = true)
            .assertIsDisplayed()
        onNodeWithTag(
                GardenScreenTestTags.getTestTagForOwnedPlantWaterBar(p), useUnmergedTree = true)
            .assertIsDisplayed()
      }
    }
  }

  /**
   * Tests that when the list is empty, the empty list message and profile components are displayed
   */
  @Test
  fun correctDisplayWhenEmptyGarden() {
    setContent()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.SIGN_OUT_BUTTON).assertIsDisplayed()
    composeTestRule.userRowIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EMPTY_GARDEN_MSG).assertIsDisplayed()
  }

  /**
   * Tests that the whole list is displayed (alongside profile components) when the list is not
   * empty
   */
  @Test
  fun correctDisplayWhenNonEmptyGarden() {
    val plants = listOf(plant1, plant2, plant3, plant4)
    setContent(plants)
    composeTestRule.onNodeWithTag(GardenScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.SIGN_OUT_BUTTON).assertIsDisplayed()
    composeTestRule.userRowIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.GARDEN_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EMPTY_GARDEN_MSG).assertIsNotDisplayed()
    composeTestRule.allPlantsAreDisplayed()
  }

  /** Tests that all buttons present are clickable */
  @Test
  fun buttonsAreClickable() {
    val plants = listOf(plant1, plant2, plant3, plant4)
    setContent(plants)
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.ADD_PLANT_FAB).assertIsEnabled()

    // Check that all watering button are clickable
    runTest {
      plantsRepo.getAllOwnedPlants().forEach { p ->
        composeTestRule
            .onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlantWaterButton(p))
            .assertIsEnabled()
      }
    }
  }

  /** Tests that all plants have a valid status */
  @Test
  fun statusAreValid() {
    val plants = listOf(plant1, plant2, plant3, plant4)
    setContent(plants)
    runTest {
      plantsRepo.getAllOwnedPlants().forEach { p ->
        var success = false
        for (status in PlantHealthStatus.entries) {
          try {
            composeTestRule
                .onNodeWithTag(
                    GardenScreenTestTags.getTestTagForOwnedPlantStatus(p), useUnmergedTree = true)
                .assertTextEquals(status.description)
            success = true
            break
          } catch (e: AssertionError) {
            // Do nothing until it matched one of the plant status possible
          }
        }
        assertTrue(success)
      }
    }
  }

  /** Tests that all the names and latin names are corresponding to the actual plant names */
  @Test
  fun namesAndLatinNamesAreCorrect() {
    val plants = listOf(plant1, plant2, plant3, plant4)
    setContent(plants)
    runTest {
      plantsRepo.getAllOwnedPlants().forEach { p ->
        composeTestRule
            .onNodeWithTag(
                GardenScreenTestTags.getTestTagForOwnedPlantName(p), useUnmergedTree = true)
            .assertTextEquals(p.plant.name)
        composeTestRule
            .onNodeWithTag(
                GardenScreenTestTags.getTestTagForOwnedPlantLatinName(p), useUnmergedTree = true)
            .assertTextEquals(p.plant.latinName)
      }
    }
  }

  /** Tests that the right color palette is returned when a status is given */
  @Test
  fun rightColorPaletteForEachStatus() {
    var colorScheme: ColorScheme? = null
    var customColors: CustomColors? = null
    composeTestRule.setContent {
      MyGardenTheme { GardenScreen(onEditProfile = {}, onAddPlant = {}) }
      colorScheme = MaterialTheme.colorScheme
      customColors = ExtendedTheme.colors
    }
    composeTestRule.waitForIdle()

    if (colorScheme != null && customColors != null) {
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.UNKNOWN, colorScheme, customColors),
          PlantCardColorPalette(colorScheme.surfaceVariant, customColors.wateringBlue))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.HEALTHY, colorScheme, customColors),
          PlantCardColorPalette(colorScheme.primary, customColors.wateringBlue))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.SLIGHTLY_DRY, colorScheme, customColors),
          PlantCardColorPalette(colorScheme.primaryContainer, customColors.wateringBlue))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.NEEDS_WATER, colorScheme, customColors),
          PlantCardColorPalette(colorScheme.secondaryContainer, customColors.wateringOrange))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.OVERWATERED, colorScheme, customColors),
          PlantCardColorPalette(colorScheme.secondaryContainer, customColors.wateringOrange))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.SEVERELY_OVERWATERED, colorScheme, customColors),
          PlantCardColorPalette(customColors.redPlantCardBackground, colorScheme.error))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.SEVERELY_DRY, colorScheme, customColors),
          PlantCardColorPalette(customColors.redPlantCardBackground, colorScheme.error))
    }
  }
}
