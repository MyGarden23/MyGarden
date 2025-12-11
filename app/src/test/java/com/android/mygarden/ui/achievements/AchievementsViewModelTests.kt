package com.android.mygarden.ui.achievements

import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.Achievements
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.utils.FakeAchievementsRepository
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AchievementsViewModelTests {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: AchievementsRepository
  private lateinit var id: String

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = FakeAchievementsRepository()
    id = "test-id"
  }

  @After
  fun clean() {
    Dispatchers.resetMain()
  }

  @Test
  fun achievementUiStateHasRightDefaultValues() {
    val data = AchievementsUIState()
    assertEquals(data.plantsNumberLevel, 1)
    assertEquals(data.friendsNumberLevel, 1)
    assertEquals(data.healthyStreakLevel, 1)
  }

  @Test
  fun uiStateDefaultsToLevelFromZeroProgress() = runTest {
    val viewModel = AchievementsViewModel(achievementsRepo = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value

    // Levels should be computed from value = 0 for each type
    val expectedPlantsLevel = Achievements.PLANTS_NUMBER.computeLevel(0)
    val expectedFriendsLevel = Achievements.FRIENDS_NUMBER.computeLevel(0)
    val expectedHealthyLevel = Achievements.HEALTHY_STREAK.computeLevel(0)

    assertEquals(expectedPlantsLevel, state.plantsNumberLevel)
    assertEquals(expectedFriendsLevel, state.friendsNumberLevel)
    assertEquals(expectedHealthyLevel, state.healthyStreakLevel)
  }

  @Test
  fun getUserAchievementProgressWorkWithInialValues() = runTest {
    repository.initializeAchievementsForNewUser(id)

    val viewModel = AchievementsViewModel(achievementsRepo = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value

    val plants = repository.getUserAchievementProgress(id, AchievementType.PLANTS_NUMBER)
    assertNotNull(plants)
    assertEquals(
        Achievements.PLANTS_NUMBER.computeLevel(plants!!.currentValue), state.plantsNumberLevel)

    val friends = repository.getUserAchievementProgress(id, AchievementType.FRIENDS_NUMBER)
    assertNotNull(friends)
    assertEquals(
        Achievements.FRIENDS_NUMBER.computeLevel(friends!!.currentValue), state.friendsNumberLevel)

    val healthyStreak = repository.getUserAchievementProgress(id, AchievementType.HEALTHY_STREAK)
    assertNotNull(healthyStreak)
    assertEquals(
        Achievements.HEALTHY_STREAK.computeLevel(healthyStreak!!.currentValue),
        state.healthyStreakLevel)
  }

  @Test
  fun uiStateUsesLevelsFromRepositoryProgress() = runTest {
    val plantsValue = 7
    val friendsValue = 3
    val healthyValue = 10

    repository.initializeAchievementsForNewUser(id)
    repository.setAchievementValue(id, AchievementType.PLANTS_NUMBER, plantsValue)
    repository.setAchievementValue(id, AchievementType.FRIENDS_NUMBER, friendsValue)
    repository.setAchievementValue(id, AchievementType.HEALTHY_STREAK, healthyValue)

    val viewModel = AchievementsViewModel(achievementsRepo = repository)
    advanceUntilIdle()

    val state = viewModel.uiState.value

    assertEquals(Achievements.PLANTS_NUMBER.computeLevel(plantsValue), state.plantsNumberLevel)
    assertEquals(Achievements.FRIENDS_NUMBER.computeLevel(friendsValue), state.friendsNumberLevel)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(healthyValue), state.healthyStreakLevel)
  }

  @Test
  fun getCorrespondingLevelReturnsLevelFromGivenState() = runTest {
    val viewModel = AchievementsViewModel(achievementsRepo = repository)

    val customState =
        AchievementsUIState(
            plantsNumberLevel = 2,
            friendsNumberLevel = 5,
            healthyStreakLevel = 7,
            plantsNumberValue = 10,
            friendsNumberValue = 20,
            healthyStreakValue = 30)

    assertEquals(2, viewModel.getCorrespondingLevel(AchievementType.PLANTS_NUMBER, customState))
    assertEquals(5, viewModel.getCorrespondingLevel(AchievementType.FRIENDS_NUMBER, customState))
    assertEquals(7, viewModel.getCorrespondingLevel(AchievementType.HEALTHY_STREAK, customState))
  }

  @Test
  fun getCorrespondingValueReturnsValueFromGivenState() = runTest {
    val viewModel = AchievementsViewModel(achievementsRepo = repository)

    val customState =
        AchievementsUIState(
            plantsNumberLevel = 1,
            friendsNumberLevel = 1,
            healthyStreakLevel = 1,
            plantsNumberValue = 42,
            friendsNumberValue = 17,
            healthyStreakValue = 99)

    assertEquals(42, viewModel.getCorrespondingValue(AchievementType.PLANTS_NUMBER, customState))
    assertEquals(17, viewModel.getCorrespondingValue(AchievementType.FRIENDS_NUMBER, customState))
    assertEquals(99, viewModel.getCorrespondingValue(AchievementType.HEALTHY_STREAK, customState))
  }

  @Test
  fun getCorrespondingNextThresholdReturnsNextStrictlyGreaterThreshold() = runTest {
    val viewModel = AchievementsViewModel(achievementsRepo = repository)

    val thresholds = Achievements.PLANTS_NUMBER_THRESHOLDS
    val currentValue = thresholds[0]
    val expectedNext = thresholds[1]

    val state =
        AchievementsUIState(
            plantsNumberLevel = 1,
            friendsNumberLevel = 1,
            healthyStreakLevel = 1,
            plantsNumberValue = currentValue,
            friendsNumberValue = 0,
            healthyStreakValue = 0)

    val next = viewModel.getCorrespondingNextThreshold(AchievementType.PLANTS_NUMBER, state)

    assertEquals(expectedNext, next)
  }

  @Test
  fun getCorrespondingNextThresholdReturnsMinusOneWhenBeyondLastThreshold() = runTest {
    val viewModel = AchievementsViewModel(achievementsRepo = repository)

    val plantsLast = Achievements.PLANTS_NUMBER_THRESHOLDS.last()
    val friendsLast = Achievements.FRIENDS_NUMBER_THRESHOLDS.last()
    val streakLast = Achievements.HEALTHY_STREAK_THRESHOLDS.last()

    val state =
        AchievementsUIState(
            plantsNumberLevel = 1,
            friendsNumberLevel = 1,
            healthyStreakLevel = 1,
            plantsNumberValue = plantsLast,
            friendsNumberValue = friendsLast,
            healthyStreakValue = streakLast)

    assertEquals(-1, viewModel.getCorrespondingNextThreshold(AchievementType.PLANTS_NUMBER, state))
    assertEquals(-1, viewModel.getCorrespondingNextThreshold(AchievementType.FRIENDS_NUMBER, state))
    assertEquals(-1, viewModel.getCorrespondingNextThreshold(AchievementType.HEALTHY_STREAK, state))
  }

  @Test
  fun getCorrespondingNeededForNextLevelReturnsDifferenceToNextThreshold() = runTest {
    val viewModel = AchievementsViewModel(achievementsRepo = repository)

    val thresholds = Achievements.PLANTS_NUMBER_THRESHOLDS
    val currentValue = thresholds[0]
    val nextThreshold = thresholds[1]
    val expectedNeeded = nextThreshold - currentValue

    val state =
        AchievementsUIState(
            plantsNumberLevel = 1,
            friendsNumberLevel = 1,
            healthyStreakLevel = 1,
            plantsNumberValue = currentValue,
            friendsNumberValue = 0,
            healthyStreakValue = 0)

    val needed = viewModel.getCorrespondingNeededForNextLevel(AchievementType.PLANTS_NUMBER, state)

    assertEquals(expectedNeeded, needed)
  }

  @Test
  fun getCorrespondingNeededForNextLevelReturnsMinusOneWhenNoHigherThreshold() = runTest {
    val viewModel = AchievementsViewModel(achievementsRepo = repository)

    val plantsLast = Achievements.PLANTS_NUMBER_THRESHOLDS.last()
    val state =
        AchievementsUIState(
            plantsNumberLevel = 1,
            friendsNumberLevel = 1,
            healthyStreakLevel = 1,
            plantsNumberValue = plantsLast,
            friendsNumberValue = 0,
            healthyStreakValue = 0)

    val needed = viewModel.getCorrespondingNeededForNextLevel(AchievementType.PLANTS_NUMBER, state)

    assertEquals(-1, needed)
  }
}
