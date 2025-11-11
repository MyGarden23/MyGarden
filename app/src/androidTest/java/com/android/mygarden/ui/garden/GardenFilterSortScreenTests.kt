package com.android.mygarden.ui.garden

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.profile.Avatar
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Navigation tests for verifying that sort and filter options work correctly on the Garden screen.
 */
@RunWith(AndroidJUnit4::class)
class GardenFilterSortScreenTests {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var navController: NavHostController

  /** Fake profile repository for testing */
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

  @Before
  fun setUp() {
    // Set up fake profile repository
    ProfileRepositoryProvider.repository = FakeProfileRepository()

    // Set up local plants repository for testing
    PlantsRepositoryProvider.repository = PlantsRepositoryLocal()

    // Create test plants with different health statuses and names
    val repo = PlantsRepositoryProvider.repository
    runTest {
      // Healthy plant
      repo.saveToGarden(
          id = "test-healthy-1",
          plant =
              Plant(
                  name = "Aloe Vera",
                  latinName = "Aloe barbadensis",
                  wateringFrequency = 14,
                  healthStatus = PlantHealthStatus.HEALTHY),
          lastWatered = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3)))

      // Dry plant
      repo.saveToGarden(
          id = "test-dry-1",
          plant =
              Plant(
                  name = "Cactus",
                  latinName = "Cactaceae",
                  wateringFrequency = 7,
                  healthStatus = PlantHealthStatus.NEEDS_WATER),
          lastWatered = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(8)))

      // Another healthy plant
      repo.saveToGarden(
          id = "test-healthy-2",
          plant =
              Plant(
                  name = "Bamboo",
                  latinName = "Bambusoideae",
                  wateringFrequency = 5,
                  healthStatus = PlantHealthStatus.HEALTHY),
          lastWatered = Timestamp(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))
    }

    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      AppNavHost(navController = controller, startDestination = Screen.Garden.route)
    }
    composeTestRule.waitForIdle()
  }

  @After
  fun cleanUp() {
    // Clean up test plants
    val repo = PlantsRepositoryProvider.repository
    runBlocking {
      val allPlants = repo.getAllOwnedPlants()
      allPlants.forEach { repo.deleteFromGarden(it.id) }
    }
  }

  /** Tests that all filter options are accessible via test tags. */
  @Test
  fun allFilterOptionsHaveTestTags() {
    // Verify we're on Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
    composeTestRule.waitForIdle()

    // Open filter dropdown
    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.FILTER_DROPDOWN)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Test "All Plants" filter option
    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.FILTER_ALL)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Open filter dropdown again and test "Overwatered" option
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.FILTER_DROPDOWN).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.FILTER_OVERWATERED)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Open filter dropdown again and test "Dry Plants" option
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.FILTER_DROPDOWN).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.FILTER_DRY)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Open filter dropdown again and test "Critical" option
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.FILTER_DROPDOWN).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.FILTER_CRITICAL)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Open filter dropdown again and test "Healthy" option
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.FILTER_DROPDOWN).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.FILTER_HEALTHY)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
  }

  /** Tests that all sort options are accessible via test tags. */
  @Test
  fun allSortOptionsHaveTestTags() {
    // Verify we're on Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
    composeTestRule.waitForIdle()

    // Open sort dropdown and test "Plant Name" option
    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.SORT_DROPDOWN)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.SORT_PLANT_NAME)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Open sort dropdown again and test "Latin Name" option
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.SORT_DROPDOWN).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.SORT_LATIN_NAME)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Open sort dropdown again and test "Oldest Watered" option
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.SORT_DROPDOWN).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.SORT_LAST_WATERED_ASC)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Open sort dropdown again and test "Recent Watered" option
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.SORT_DROPDOWN).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(SortFilterBarTestTags.SORT_LAST_WATERED_DESC)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
  }

  /** Tests that sort and filter dropdowns are displayed on the Garden screen. */
  @Test
  fun sortAndFilterDropdownsAreDisplayed() {
    // Verify we're on Garden screen
    composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_SCREEN).assertIsDisplayed()
    composeTestRule.waitForIdle()

    // Verify sort and filter bar is displayed
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.SORT_FILTER_BAR).assertIsDisplayed()

    // Verify sort dropdown is displayed
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.SORT_DROPDOWN).assertIsDisplayed()

    // Verify filter dropdown is displayed
    composeTestRule.onNodeWithTag(SortFilterBarTestTags.FILTER_DROPDOWN).assertIsDisplayed()
  }
}
