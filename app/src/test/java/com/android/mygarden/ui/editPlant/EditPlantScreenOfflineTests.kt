package com.android.mygarden.ui.editPlant

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.android.mygarden.model.caretips.CareTipsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeActivityRepository
import com.android.mygarden.utils.FakeCareTipsRepository
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.TestPlants
import java.sql.Timestamp
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
 * Tests for offline functionality in EditPlantScreen.
 *
 * These tests verify that:
 * 1. Save button shows toast when clicked offline
 * 2. Delete button shows toast when clicked offline
 * 3. Actions are not executed when offline
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditPlantScreenOfflineTests {

  @get:Rule val composeTestRule = createComposeRule()

  // Use gardenActivityPlant which has all required fields and is recognized
  private val testPlant = TestPlants.gardenActivityPlant
  private lateinit var plantsRepo: PlantsRepository
  private lateinit var testPlantId: String

  @Before
  fun setUp() {
    // Set up repositories
    plantsRepo = PlantsRepositoryLocal()
    ProfileRepositoryProvider.repository =
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
    PlantsRepositoryProvider.repository = plantsRepo
    ActivityRepositoryProvider.repository = FakeActivityRepository()
    CareTipsRepositoryProvider.repository = FakeCareTipsRepository()

    // Save a test plant to the garden
    runTest {
      testPlantId = plantsRepo.getNewId()
      plantsRepo.saveToGarden(testPlant, testPlantId, Timestamp(System.currentTimeMillis()))
    }

    // Start online
    OfflineStateManager.setOnlineState(true)
    ShadowToast.reset()
  }

  @After
  fun tearDown() {
    runTest { plantsRepo.getAllOwnedPlants().forEach { p -> plantsRepo.deleteFromGarden(p.id) } }
    OfflineStateManager.setOnlineState(true)
  }

  /**
   * Sets up the test content with specified online/offline state
   *
   * @param ownedPlantId the ID of the plant to edit
   * @param isOnline whether the device should be in online or offline mode
   */
  private fun setContent(
      ownedPlantId: String = testPlantId,
      isOnline: Boolean = true,
  ) {
    // Set the offline state before composing the screen
    OfflineStateManager.setOnlineState(isOnline)

    composeTestRule.setContent {
      MyGardenTheme { EditPlantScreen(ownedPlantId = ownedPlantId, onDeleted = {}, goBack = {}) }
    }
    composeTestRule.waitForIdle()
  }

  /** Test that the Save button shows a toast when clicked while offline */
  @Test
  fun saveButtonShowsToastWhenOffline() {
    setContent(isOnline = false)

    // Scroll to and click the save button
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))

    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot save plant while offline", toastText)
  }

  /** Test that the Delete button shows a toast when clicked while offline */
  @Test
  fun deleteButtonShowsToastWhenOffline() {
    setContent(isOnline = false)

    // Scroll to and click the delete button to open the popup
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))

    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Click the confirm button in the popup
    composeTestRule
        .onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot delete plant while offline", toastText)
  }

  /** Test that save button does not save plant when offline */
  @Test
  fun saveButtonDoesNotSavePlantWhenOffline() = runTest {
    setContent(isOnline = false)

    val initialPlantCount = plantsRepo.getAllOwnedPlants().size

    // Scroll to and click the save button
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))

    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()

    composeTestRule.waitForIdle()

    // Verify toast was shown
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot save plant while offline", toastText)

    // Verify plant count hasn't changed (no new plants added, existing not removed)
    val finalPlantCount = plantsRepo.getAllOwnedPlants().size
    assertEquals(initialPlantCount, finalPlantCount)
  }

  /** Test that delete button does not delete plant when offline */
  @Test
  fun deleteButtonDoesNotDeletePlantWhenOffline() = runTest {
    setContent(isOnline = false)

    val initialPlantCount = plantsRepo.getAllOwnedPlants().size

    // Scroll to and click the delete button to open the popup
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))

    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()

    composeTestRule.waitForIdle()

    // Click the confirm button in the popup
    composeTestRule
        .onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Verify toast was shown
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot delete plant while offline", toastText)

    // Verify plant was not deleted (count remains the same)
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
        EditPlantScreen(ownedPlantId = testPlantId, onDeleted = {}, goBack = { backPressed = true })
      }
    }
    composeTestRule.waitForIdle()

    // Click the back button (it's in the TopBar)
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Verify back action was triggered
    assert(backPressed) { "Back button should work even when offline" }
  }
}
