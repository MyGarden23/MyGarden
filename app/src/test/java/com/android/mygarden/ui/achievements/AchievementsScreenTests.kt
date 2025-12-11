package com.android.mygarden.ui.achievements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.utils.FakeAchievementsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AchievementsScreenTests {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var achievementsRepo: AchievementsRepository
  private lateinit var viewModel: AchievementsViewModel

  @Before
  fun setUp() = runTest {
    achievementsRepo = FakeAchievementsRepository()

    achievementsRepo.initializeAchievementsForNewUser("test-id")

    viewModel =
        AchievementsViewModel(
            friendId = null,
            achievementsRepo = achievementsRepo,
        )

    composeTestRule.setContent { AchievementsScreen(viewModel = viewModel) }
    composeTestRule.waitForIdle()
  }

  /** Base case: achievements cards are displayed but popups are not */
  @Test
  fun allAchievementsCardAreDisplayedAndPopupsAreNot() {
    for (type in AchievementType.entries) {
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagForAchievementCard(type), useUnmergedTree = true)
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagForCardLevel(type), useUnmergedTree = true)
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagForPopup(type), useUnmergedTree = true)
          .assertIsNotDisplayed()
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagForRemainingUnits(type), useUnmergedTree = true)
          .assertIsNotDisplayed()
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagClosingButton(type), useUnmergedTree = true)
          .assertIsNotDisplayed()
    }
  }

  /** The UI is responsive when clicking on achievements cards */
  @Test
  fun allAchievementsDialogDisplayCorrectlyWhenClickingOnCards() {
    for (type in AchievementType.entries) {
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagForAchievementCard(type), useUnmergedTree = true)
          .performClick()
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagForPopup(type), useUnmergedTree = true)
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagForRemainingUnits(type), useUnmergedTree = true)
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(
              AchievementsScreenTestTags.getTestTagClosingButton(type), useUnmergedTree = true)
          .assertIsDisplayed()
          .performClick()
    }
  }

  /** The UI correctly updates when the raw value is greater than the max value */
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun uiIsCorrectWhenMaxLevelIsReached() = runTest {
    composeTestRule
        .onNodeWithTag(
            AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.PLANTS_NUMBER),
            useUnmergedTree = true)
        .assertTextEquals("Level 1/10")
    achievementsRepo.setAchievementValue("test-id", AchievementType.PLANTS_NUMBER, 150)
    advanceUntilIdle()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(
            AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.PLANTS_NUMBER),
            useUnmergedTree = true)
        .assertTextEquals("Level 10/10")
    composeTestRule
        .onNodeWithTag(
            AchievementsScreenTestTags.getTestTagForAchievementCard(AchievementType.PLANTS_NUMBER),
            useUnmergedTree = true)
        .performClick()
    composeTestRule
        .onNodeWithTag(
            AchievementsScreenTestTags.getTestTagForRemainingUnits(AchievementType.PLANTS_NUMBER),
            useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  /** The UI correctly updates when the raw value changes (mid-level) */
  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun uiIsCorrectWhenMidLevelIsReached() = runTest {
    composeTestRule
        .onNodeWithTag(
            AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.FRIENDS_NUMBER),
            useUnmergedTree = true)
        .assertTextEquals("Level 1/10")
    achievementsRepo.setAchievementValue("test-id", AchievementType.FRIENDS_NUMBER, 12)
    advanceUntilIdle()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(
            AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.FRIENDS_NUMBER),
            useUnmergedTree = true)
        .assertTextEquals("Level 5/10")
    composeTestRule
        .onNodeWithTag(
            AchievementsScreenTestTags.getTestTagForAchievementCard(AchievementType.FRIENDS_NUMBER),
            useUnmergedTree = true)
        .performClick()
    composeTestRule
        .onNodeWithTag(
            AchievementsScreenTestTags.getTestTagForRemainingUnits(AchievementType.FRIENDS_NUMBER),
            useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
