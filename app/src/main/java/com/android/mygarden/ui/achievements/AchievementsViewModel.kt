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
 * UI state that contains the current level of each achievement category as well as the raw data
 * value (used to display the remaining value needed).
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
 * [AchievementsRepository] and transforms raw values into user-facing values and levels using the
 * logic defined in [Achievements].
 *
 * @property achievementsRepo The repository that contains the user's achievements.
 */
class AchievementsViewModel(
    private val achievementsRepo: AchievementsRepository = AchievementsRepositoryProvider.repository
) : ViewModel() {

  /** UI state as a Flow for continuous update. */
  private val _uiState = MutableStateFlow(AchievementsUIState())
  val uiState: StateFlow<AchievementsUIState> = _uiState.asStateFlow()

  /**
   * Internal mapping that maps the [AchievementType] to:
   * - how to read its level from [AchievementsUIState]
   * - how to read its raw value from [AchievementsUIState]
   * - the list of thresholds used to compute the next levels
   *
   *   Should be used with [retrieveAchievementData].
   */
  private data class AchievementDataMapper(
      val level: (AchievementsUIState) -> Int,
      val value: (AchievementsUIState) -> Int,
      val thresholds: (AchievementsUIState) -> List<Int>
  )

  /**
   * Retrieves the data mapper for the given [AchievementType]
   *
   * @param type The given type for which we want to map.
   */
  private fun retrieveAchievementData(type: AchievementType): AchievementDataMapper {
    return when (type) {
      AchievementType.PLANTS_NUMBER ->
          AchievementDataMapper(
              level = { it.plantsNumberLevel },
              value = { it.plantsNumberValue },
              thresholds = { Achievements.PLANTS_NUMBER_THRESHOLDS })
      AchievementType.FRIENDS_NUMBER ->
          AchievementDataMapper(
              level = { it.healthyStreakLevel },
              value = { it.healthyStreakValue },
              thresholds = { Achievements.FRIENDS_NUMBER_THRESHOLDS })
      AchievementType.HEALTHY_STREAK ->
          AchievementDataMapper(
              level = { it.healthyStreakLevel },
              value = { it.healthyStreakValue },
              thresholds = { Achievements.HEALTHY_STREAK_THRESHOLDS })
    }
  }

  init {
    refreshUIState()
  }

  /**
   * Returns the current level for the given [achievementType] stored in the provided [state]. This
   * function is a helper for the UI code that needs the level by achievement type.
   *
   * @param achievementType The given type for which we want to retrieve the level from.
   * @param state The current UI state.
   * @return The level of the user of the given [achievementType] in the [state].
   */
  fun getCorrespondingLevel(achievementType: AchievementType, state: AchievementsUIState): Int {
    return retrieveAchievementData(achievementType).level(state)
  }

  /**
   * Returns the current raw value for the given [achievementType] stored in the provided [state].
   * This function is a helper for the UI code that needs the raw value by achievement type.
   *
   * @param achievementType The given type for which we want to retrieve the raw value from.
   * @param state The current UI state.
   * @return The raw value of the user of the given [achievementType] in the [state].
   */
  fun getCorrespondingValue(achievementType: AchievementType, state: AchievementsUIState): Int {
    return retrieveAchievementData(achievementType).value(state)
  }

  /**
   * Returns how many additional units of progress are needed to reach the next level for the given
   * [achievementType] based on the provided [state]. This function is a helper for the UI code that
   * needs the raw value by achievement type.
   *
   * @param achievementType The given type for which we want to retrieve the needed value from.
   * @param state The current UI state.
   * @return The progress needed to be done by the user to reach the next level in the given
   *   [achievementType] in the [state] or '-1' if the user is beyond the last threshold.
   */
  fun getCorrespondingNeededForNextLevel(
      achievementType: AchievementType,
      state: AchievementsUIState
  ): Int {
    val next = getCorrespondingNextThreshold(achievementType, state)
    return if (next < 0) next else next - retrieveAchievementData(achievementType).value(state)
  }

  /**
   * Returns the next threshold value for the given [achievementType] based on the provided [state]
   * (i.e. the raw value the user must reach to obtain the next level). This function is a helper
   * for the UI code that needs the raw value by achievement type.
   *
   * @param achievementType The given type for which we want to retrieve the threshold from.
   * @param state The current UI state.
   * @return The threshold to reach for the next level according to the [state] or '-1' if there is
   *   no higher threshold in this [achievementType].
   */
  fun getCorrespondingNextThreshold(
      achievementType: AchievementType,
      state: AchievementsUIState
  ): Int {
    val data = retrieveAchievementData(achievementType)
    val value = data.value(state)
    return data.thresholds(state).firstOrNull { it > value } ?: -1
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
   * @return The UI state containing the computed level for each type and the raw values.
   */
  private fun mapToAchievementsUIState(
      progressList: List<UserAchievementProgress>
  ): AchievementsUIState {
    val byType = progressList.associateBy { it.achievementType }

    fun valueFor(type: AchievementType): Int = byType[type]?.currentValue ?: ACHIEVEMENTS_BASE_VALUE

    val plantsValue = valueFor(AchievementType.PLANTS_NUMBER)
    val friendsValue = valueFor(AchievementType.FRIENDS_NUMBER)
    val healthyStreakValue = valueFor(AchievementType.HEALTHY_STREAK)

    return AchievementsUIState(
        plantsNumberLevel = Achievements.PLANTS_NUMBER.computeLevel(plantsValue),
        friendsNumberLevel = Achievements.FRIENDS_NUMBER.computeLevel(friendsValue),
        healthyStreakLevel = Achievements.HEALTHY_STREAK.computeLevel(healthyStreakValue),
        plantsNumberValue = plantsValue,
        friendsNumberValue = friendsValue,
        healthyStreakValue = healthyStreakValue)
  }
}
