package com.android.mygarden.ui.garden

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.LikesRepositoryProvider
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.CustomColors
import com.android.mygarden.ui.theme.ExtendedTheme
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeAchievementsRepository
import com.android.mygarden.utils.FakeActivityRepository
import com.android.mygarden.utils.FakeLikesRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import com.android.mygarden.utils.TestPlants
import com.google.firebase.FirebaseApp
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GardenScreenTests {

  @get:Rule val composeTestRule = createComposeRule()

  // Some fictional plants to play with
  val plant1 = TestPlants.samplePlant1
  val plant2 = TestPlants.samplePlant2
  val plant3 = TestPlants.samplePlant3
  val plant4 = TestPlants.samplePlant4

  /** Fake profile local repository used to test the viewModel/profile interactions */
  private class FakeProfileRepository(
      initialProfile: Profile? =
          Profile(
              firstName = "Test",
              lastName = "User",
              pseudo = "Pseudo",
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

    override suspend fun attachFCMToken(token: String): Boolean {
      return false
    }

    override suspend fun getFCMToken(): String? {
      return null
    }

    override suspend fun isCurrentUserPseudo(pseudo: String): Boolean {
      return true
    }

    override fun cleanup() {}
  }

  private lateinit var plantsRepo: PlantsRepository
  private lateinit var profileRepo: ProfileRepository

  /**
   * Sets the [plantsRepo] as a local repository for testing and sets the provider's repo to this
   * one to ensure that the local repo of the test class is the one used by the screen instance
   */
  @Before
  fun setUp() {
    // Initialize Firebase for tests
    if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
      FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }
    plantsRepo = PlantsRepositoryLocal()
    profileRepo = FakeProfileRepository()
    ProfileRepositoryProvider.repository = profileRepo
    PlantsRepositoryProvider.repository = plantsRepo
    UserProfileRepositoryProvider.repository = FakeUserProfileRepository()
    ActivityRepositoryProvider.repository = FakeActivityRepository()
    AchievementsRepositoryProvider.repository = FakeAchievementsRepository()
    LikesRepositoryProvider.repository = FakeLikesRepository()
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
    composeTestRule.setContent {
      ParentTabScreenGarden(
          gardenCallbacks = GardenScreenCallbacks(onEditProfile = {}, onAddPlant = {}))
    }
    composeTestRule.waitForIdle()
  }

  /** Ensures to clear the repo at the end of each test for consistency */
  @After
  fun eraseFromRepo() {
    runTest { plantsRepo.getAllOwnedPlants().forEach { p -> plantsRepo.deleteFromGarden(p.id) } }
  }

  /** Ensures that all profile-related components are currently displayed */
  fun ComposeTestRule.userRowIsDisplayed() {
    onNodeWithTag(GardenAchievementsParentScreenTestTags.AVATAR_EDIT_PROFILE).assertIsDisplayed()
    onNodeWithTag(GardenAchievementsParentScreenTestTags.PSEUDO).assertIsDisplayed()
    onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).assertIsDisplayed()
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
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).assertHasClickAction()
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
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).assertHasClickAction()
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
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.AVATAR_EDIT_PROFILE)
        .assertIsEnabled()
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
      MyGardenTheme {
        GardenScreen(callbacks = GardenScreenCallbacks(onEditProfile = {}, onAddPlant = {}))
      }
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
          PlantCardColorPalette(colorScheme.secondaryContainer, customColors.wateringBlue))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.OVERWATERED, colorScheme, customColors),
          PlantCardColorPalette(colorScheme.secondaryContainer, customColors.wateringBlue))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.SEVERELY_OVERWATERED, colorScheme, customColors),
          PlantCardColorPalette(customColors.redPlantCardBackground, customColors.wateringBlue))
      assertEquals(
          colorsFromHealthStatus(PlantHealthStatus.SEVERELY_DRY, colorScheme, customColors),
          PlantCardColorPalette(customColors.redPlantCardBackground, customColors.wateringBlue))
    }
  }

  /** Checks that the plant's card is displayed when a plant is saved to garden afterwards. */
  @Test
  fun listIsUpdatedWhenNewPlantAddedToGarden() {
    // empty garden for now
    setContent()
    composeTestRule.userRowIsDisplayed()
    composeTestRule.onNodeWithTag(GardenScreenTestTags.EMPTY_GARDEN_MSG).assertIsDisplayed()

    // add a plant to repo
    runTest {
      plantsRepo.saveToGarden(plant1, plantsRepo.getNewId(), Timestamp(System.currentTimeMillis()))
    }

    composeTestRule.onNodeWithTag(GardenScreenTestTags.EMPTY_GARDEN_MSG).assertIsNotDisplayed()
    composeTestRule.allPlantsAreDisplayed()
  }

  /**
   * Checks that the plant's health status correctly changes when clicked on the button to water the
   * plant
   */
  @Test
  fun wateringAPlantWorks() = runTest {
    setContent()
    val id = plantsRepo.getNewId()

    plantsRepo.saveToGarden(
        plant1,
        id,
        // Timestamp to make the plant thirsty (watering frequency = 10 days)
        Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(12)))

    // the plant is currently thirsty
    val plantFromRepo = plantsRepo.getOwnedPlant(id)
    val currentStatus = plantFromRepo.plant.healthStatus
    assertEquals(PlantHealthStatus.NEEDS_WATER, currentStatus)

    // water the plant
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlantWaterButton(plantFromRepo))
        .performClick()

    // the plant's health status has been correctly updated
    val plantFromRepoAfterWatering = plantsRepo.getOwnedPlant(id)
    val statusAfterWatering = plantFromRepoAfterWatering.plant.healthStatus
    assertEquals(PlantHealthStatus.HEALTHY, statusAfterWatering)
  }

  @Test
  fun likeCounter_isDisplayed_inTopBar() {
    setContent()

    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.LIKE_COUNTER)
        .assertIsDisplayed()
        .assertTextEquals("0")
  }

  @Test
  fun likeButton_hasNoClickAction_onOwnGarden() {
    setContent()

    // Own garden => isViewMode = false => like disabled
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.LIKE_BUTTON)
        .assertIsDisplayed()
        .assertHasNoClickAction()
  }

  @Test
  fun likeButton_isClickable_inViewMode_andTogglesHeartAndCount() {
    // Setup screen in view mode by passing a friendId
    composeTestRule.setContent {
      MyGardenTheme {
        ParentTabScreenGarden(
            friendId = "friend-uid",
            isViewMode = true,
            gardenCallbacks = GardenScreenCallbacks(onEditProfile = {}, onAddPlant = {}))
      }
    }
    composeTestRule.waitForIdle()

    // Initially not liked
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.LIKE_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()

    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.LIKE_COUNTER, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextEquals("0")

    // Click like
    composeTestRule.onNodeWithTag(GardenAchievementsParentScreenTestTags.LIKE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Count should update to 1 (and heart should be filled, indirectly validated via state)
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.LIKE_COUNTER, useUnmergedTree = true)
        .assertTextEquals("1")

    // Click again (toggle back)
    composeTestRule.onNodeWithTag(GardenAchievementsParentScreenTestTags.LIKE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.LIKE_COUNTER, useUnmergedTree = true)
        .assertTextEquals("0")
  }
}
