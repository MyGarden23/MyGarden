package com.android.mygarden.ui.plantinfos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mygarden.model.caretips.CareTipsRepository
import com.android.mygarden.model.caretips.CareTipsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeActivityRepository
import com.android.mygarden.utils.FakeCareTipsRepository
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.TestPlants
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Tests for offline functionality in PlantInfoScreen.
 *
 * These tests verify that:
 * 1. Tips button shows toast when clicked offline
 * 2. Actions are not executed when offline
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlantInfoScreenOfflineTests {

  @get:Rule val composeTestRule = createComposeRule()

  // Sample plant for testing
  private val samplePlant = TestPlants.plantInfoPlant

  private lateinit var plantsRepo: PlantsRepository
  private lateinit var profileRepo: ProfileRepository
  private lateinit var activityRepo: ActivityRepository
  private lateinit var careTipsRepo: CareTipsRepository

  @Before
  fun setUp() {
    plantsRepo = PlantsRepositoryLocal()
    profileRepo =
        FakeProfileRepository(
            Profile(
                firstName = "Test",
                lastName = "User",
                pseudo = "TestUser",
                gardeningSkill = GardeningSkill.BEGINNER,
                favoritePlant = "Rose",
                country = "Switzerland",
                hasSignedIn = true,
                avatar = Avatar.A1))
    activityRepo = FakeActivityRepository()
    careTipsRepo = FakeCareTipsRepository()

    ProfileRepositoryProvider.repository = profileRepo
    PlantsRepositoryProvider.repository = plantsRepo
    ActivityRepositoryProvider.repository = activityRepo
    CareTipsRepositoryProvider.repository = careTipsRepo

    // Ensure we start with online state
    OfflineStateManager.setOnlineState(true)
    ShadowToast.reset()
  }

  /**
   * Sets up the test content with specified online/offline state
   *
   * @param plant the plant to display
   * @param ownedPlantId optional ID if coming from garden
   * @param isOnline whether the device should be in online or offline mode
   */
  fun setContent(
      plant: Plant = samplePlant,
      ownedPlantId: String? = null,
      isOnline: Boolean = true
  ) {
    // Set the offline state before composing the screen
    OfflineStateManager.setOnlineState(isOnline)

    composeTestRule.setContent {
      MyGardenTheme {
        PlantInfosScreen(
            plant = plant, ownedPlantId = ownedPlantId, onBackPressed = {}, onNextPlant = {})
      }
    }
    composeTestRule.waitForIdle()
  }

  @After
  fun tearDown() {
    runTest { plantsRepo.getAllOwnedPlants().forEach { p -> plantsRepo.deleteFromGarden(p.id) } }
    // Reset to online state after each test
    OfflineStateManager.setOnlineState(true)
  }

  /** Test that the Tips button shows a toast when clicked while offline */
  @Test
  fun tipsButtonShowsToastWhenOffline() {
    setContent(isOnline = false)

    // Click the tips button
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.TIPS_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot load tips while offline", toastText)
  }

  /** Test that the Tips dialog does not appear when clicking offline */
  @Test
  fun tipsDialogDoesNotAppearWhenOffline() {
    setContent(isOnline = false)

    // Click the tips button
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify the dialog is not shown
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.TIPS_DIALOG).assertDoesNotExist()
  }

  /** Test that clicking the save button while offline shows toast and doesn't save */
  @Test
  fun saveButtonDoesNotSavePlantWhenOffline() = runTest {
    setContent(isOnline = false)

    val initialPlantCount = plantsRepo.getAllOwnedPlants().size

    // Click the save button
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify toast was shown
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot save plant while offline", toastText)

    // Verify no plant was saved
    val finalPlantCount = plantsRepo.getAllOwnedPlants().size
    assertEquals(initialPlantCount, finalPlantCount)
  }

  /** Test that back button still works when offline */
  @Test
  fun backButtonWorksWhenOffline() {
    var backPressed = false

    OfflineStateManager.setOnlineState(false)

    composeTestRule.setContent {
      MyGardenTheme {
        PlantInfosScreen(
            plant = samplePlant, onBackPressed = { backPressed = true }, onNextPlant = {})
      }
    }
    composeTestRule.waitForIdle()

    // Click the back button
    composeTestRule
        .onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Verify back action was triggered
    assert(backPressed) { "Back button should work even when offline" }
  }
}
