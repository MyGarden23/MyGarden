package com.android.mygarden.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.achievements.ACHIEVEMENTS_BASE_VALUE
import com.android.mygarden.model.achievements.ACHIEVEMENTS_FIRST_LEVEL
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.Achievements
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.android.mygarden.model.achievements.UserAchievementProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state representing the current level of each achievement category.
 *
 * The values correspond to computed levels in each achievement, NOT the raw stored values.
 */
data class AchievementsUIState(
    val plantsNumberLevel: Int = ACHIEVEMENTS_FIRST_LEVEL,
    val friendsNumberLevel: Int = ACHIEVEMENTS_FIRST_LEVEL,
    val healthyStreakLevel: Int = ACHIEVEMENTS_FIRST_LEVEL,
    val plantsNumberValue: Int = ACHIEVEMENTS_BASE_VALUE,
    val friendsNumberValue: Int = ACHIEVEMENTS_BASE_VALUE,
    val healthyStreakValue: Int = ACHIEVEMENTS_BASE_VALUE,
)

/**
 * ViewModel responsible for exposing achievement levels to the UI.
 *
 * This ViewModel observes the achievement progress stored in Firestore through
 * [AchievementsRepository] and transforms raw values into user-facing levels using the logic
 * defined in [Achievements].
 *
 * @param achievementsRepo The repository that contains the user's achievements.
 */
class AchievementsViewModel(
    private val achievementsRepo: AchievementsRepository = AchievementsRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(AchievementsUIState())
  val uiState: StateFlow<AchievementsUIState> = _uiState.asStateFlow()

  init {
    refreshUIState()
  }

  fun getCorrespondingLevel(achievementType: AchievementType, state: AchievementsUIState): Int {
    return when (achievementType) {
      AchievementType.PLANTS_NUMBER -> state.plantsNumberLevel
      AchievementType.FRIENDS_NUMBER -> state.friendsNumberLevel
      AchievementType.HEALTHY_STREAK -> state.healthyStreakLevel
    }
  }

  fun getCorrespondingValue(achievementType: AchievementType, state: AchievementsUIState): Int {
    return when (achievementType) {
      AchievementType.PLANTS_NUMBER -> state.plantsNumberValue
      AchievementType.FRIENDS_NUMBER -> state.friendsNumberValue
      AchievementType.HEALTHY_STREAK -> state.healthyStreakValue
    }
  }

  fun getCorrespondingNeededForNextLevel(
      achievementType: AchievementType,
      state: AchievementsUIState
  ): Int {
    return when (achievementType) {
      AchievementType.PLANTS_NUMBER ->
          Achievements.PLANTS_NUMBER_THRESHOLDS.first { it > state.plantsNumberValue } -
              state.plantsNumberValue
      AchievementType.FRIENDS_NUMBER ->
          Achievements.FRIENDS_NUMBER_THRESHOLDS.first { it > state.friendsNumberValue } -
              state.friendsNumberValue
      AchievementType.HEALTHY_STREAK ->
          Achievements.HEALTHY_STREAK_THRESHOLDS.first { it > state.healthyStreakValue } -
              state.healthyStreakValue
    }
  }

  fun getCorrespondingNextThreshold(
      achievementType: AchievementType,
      state: AchievementsUIState
  ): Int {
    return try {
      when (achievementType) {
        AchievementType.PLANTS_NUMBER ->
            Achievements.PLANTS_NUMBER_THRESHOLDS.first { it > state.plantsNumberValue }
        AchievementType.FRIENDS_NUMBER ->
            Achievements.FRIENDS_NUMBER_THRESHOLDS.first { it > state.friendsNumberValue }
        AchievementType.HEALTHY_STREAK ->
            Achievements.HEALTHY_STREAK_THRESHOLDS.first { it > state.healthyStreakValue }
      }
    } catch (_: NoSuchElementException) {
      -1
    }
  }
  /**
   * Starts collecting the user's achievement progress from Firestore. Whenever values change, the
   * UI state is updated accordingly directly as it uses Flows.
   */
  private fun refreshUIState() {
    val userId = achievementsRepo.getCurrentUserId() ?: return

    viewModelScope.launch {
      achievementsRepo.getAllUserAchievementProgress(userId).collect { list ->
        _uiState.value = mapToAchievementsUIState(list)
      }
    }
  }

  /**
   * Converts a list of raw achievement progress values into the [AchievementsUIState] used by the
   * UI.
   *
   * @param progressList The list of achievements fetched from the repository.
   * @return The UI state containing the computed level for each type.
   */
  private fun mapToAchievementsUIState(
      progressList: List<UserAchievementProgress>
  ): AchievementsUIState {
    val byType = progressList.associateBy { it.achievementType }

    val plantsValue = byType[AchievementType.PLANTS_NUMBER]?.currentValue ?: ACHIEVEMENTS_BASE_VALUE
    val friendsValue =
        byType[AchievementType.FRIENDS_NUMBER]?.currentValue ?: ACHIEVEMENTS_BASE_VALUE
    val healthyStreakValue =
        byType[AchievementType.HEALTHY_STREAK]?.currentValue ?: ACHIEVEMENTS_BASE_VALUE

    return AchievementsUIState(
        plantsNumberLevel = Achievements.PLANTS_NUMBER.computeLevel(plantsValue),
        friendsNumberLevel = Achievements.FRIENDS_NUMBER.computeLevel(friendsValue),
        healthyStreakLevel = Achievements.HEALTHY_STREAK.computeLevel(healthyStreakValue),
        plantsNumberValue = plantsValue,
        friendsNumberValue = friendsValue,
        healthyStreakValue = healthyStreakValue)
  }
}
