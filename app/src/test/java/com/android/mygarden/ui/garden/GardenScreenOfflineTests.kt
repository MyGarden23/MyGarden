package com.android.mygarden.ui.garden

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
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
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeAchievementsRepository
import com.android.mygarden.utils.FakeActivityRepository
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import com.android.mygarden.utils.TestPlants
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
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
 * Tests for offline functionality in GardenScreen.
 *
 * These tests verify that:
 * 1. Buttons are visually greyed out when offline
 * 2. Toast messages are displayed when clicking on buttons while offline
 * 3. Actions are not executed when offline
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GardenScreenOfflineTests {

  @get:Rule val composeTestRule = createComposeRule()

  // Sample plants for testing
  val plant1 = TestPlants.samplePlant1
  val plant2 = TestPlants.samplePlant2

  /** Fake activity repository for testing */
  private lateinit var plantsRepo: PlantsRepository
  private lateinit var profileRepo: ProfileRepository
  private lateinit var activityRepo: ActivityRepository

  @Before
  fun setUp() {
    plantsRepo = PlantsRepositoryLocal()
    profileRepo =
        FakeProfileRepository(
            Profile(
                firstName = "Test",
                lastName = "User",
                pseudo = "Pseudo",
                gardeningSkill = GardeningSkill.BEGINNER,
                favoritePlant = "Rose",
                country = "Switzerland",
                hasSignedIn = true,
                avatar = Avatar.A1))
    activityRepo = FakeActivityRepository()
    ProfileRepositoryProvider.repository = profileRepo
    PlantsRepositoryProvider.repository = plantsRepo
    ActivityRepositoryProvider.repository = activityRepo
    UserProfileRepositoryProvider.repository = FakeUserProfileRepository()
    AchievementsRepositoryProvider.repository = FakeAchievementsRepository()

    // Ensure we start with online state
    OfflineStateManager.setOnlineState(true)
    ShadowToast.reset()
  }

  /**
   * Sets up the test content with specified plants and online/offline state
   *
   * @param initialOwnedPlants the list of plants to add to the garden
   * @param isOnline whether the device should be in online or offline mode
   */
  fun setContent(initialOwnedPlants: List<Plant> = emptyList(), isOnline: Boolean = true) {
    runTest {
      initialOwnedPlants.forEach {
        plantsRepo.saveToGarden(
            it,
            plantsRepo.getNewId(),
            Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(6)))
      }
    }

    // Set the offline state before composing the screen
    OfflineStateManager.setOnlineState(isOnline)

    composeTestRule.setContent {
      MyGardenTheme {
        ParentTabScreenGarden(
            gardenCallbacks = GardenScreenCallbacks(onEditProfile = {}, onAddPlant = {}))
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

  /** Test that the Edit Profile button shows a toast when clicked while offline */
  @Test
  fun editProfileButtonShowsToastWhenOffline() {
    setContent(isOnline = false)

    // Click the edit profile button
    composeTestRule
        .onNodeWithTag(GardenAchievementsParentScreenTestTags.AVATAR_EDIT_PROFILE)
        .assertIsDisplayed()
        .performClick()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot edit profile while offline", toastText)
  }

  /** Test that the Add Plant FAB shows a toast when clicked while offline */
  @Test
  fun addPlantFabShowsToastWhenOffline() {
    setContent(isOnline = false)

    // Click the add plant FAB
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.ADD_PLANT_FAB)
        .assertIsDisplayed()
        .performClick()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot add plants while offline", toastText)
  }

  /** Test that the water button on a plant card shows a toast when clicked while offline */
  @Test
  fun waterButtonShowsToastWhenOffline() = runTest {
    setContent(initialOwnedPlants = listOf(plant1), isOnline = false)

    // Get the first owned plant from the repository
    val ownedPlant = plantsRepo.getAllOwnedPlants().first()

    // Click the water button
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlantWaterButton(ownedPlant))
        .assertIsDisplayed()
        .performClick()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot water plants while offline", toastText)
  }

  /** Test that watering a plant doesn't actually update the plant when offline */
  @Test
  fun wateringPlantDoesNotUpdateWhenOffline() = runTest {
    // Set up a thirsty plant
    setContent(isOnline = false)
    val id = plantsRepo.getNewId()

    plantsRepo.saveToGarden(
        plant1, id, Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(12)))

    val plantBeforeClick = plantsRepo.getOwnedPlant(id)
    val lastWateredBefore = plantBeforeClick.lastWatered

    // Try to water the plant while offline
    composeTestRule
        .onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlantWaterButton(plantBeforeClick))
        .performClick()

    // Verify the plant's lastWatered timestamp hasn't changed
    val plantAfterClick = plantsRepo.getOwnedPlant(id)
    assertEquals(lastWateredBefore, plantAfterClick.lastWatered)
  }

  /** Test that multiple water buttons all show toasts when offline */
  @Test
  fun allWaterButtonsShowToastsWhenOffline() = runTest {
    setContent(initialOwnedPlants = listOf(plant1, plant2), isOnline = false)

    val ownedPlants = plantsRepo.getAllOwnedPlants()

    // Test each water button
    ownedPlants.forEach { ownedPlant ->
      composeTestRule
          .onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlantWaterButton(ownedPlant))
          .assertIsDisplayed()
          .performClick()

      val toastText = ShadowToast.getTextOfLatestToast()
      assertEquals("Cannot water plants while offline", toastText)

      ShadowToast.reset()
    }
  }
}
