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
}
