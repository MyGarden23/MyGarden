package com.android.mygarden.model.achievements

import androidx.annotation.Keep

/**
 * Enum class that represents every type of achievement a user can have.
 * - [AchievementType.PLANTS_NUMBER] : users have reached a certain amount of plants in their
 *   garden.
 * - [AchievementType.FRIENDS_NUMBER] : users have reached a certain amount of friends in their
 *   friends list.
 * - [AchievementType.HEALTHY_STREAK]: users have have managed to have a plant that has been through
 *   its watering frequency while staying in a HEALTHY or SLIGHTLY_DRY status.
 */
@Keep
enum class AchievementType {
  PLANTS_NUMBER,
  FRIENDS_NUMBER,
  HEALTHY_STREAK;

  override fun toString(): String {
    return when (this) {
      PLANTS_NUMBER -> "Plants number"
      FRIENDS_NUMBER -> "Friends number"
      HEALTHY_STREAK -> "Healthy streak"
    }
  }
}

/** The number of levels in the achievements */
const val ACHIEVEMENTS_LEVEL_NUMBER = 10

/** The first level of the achievements */
const val ACHIEVEMENTS_FIRST_LEVEL = 1

/** The increment used for gaining an achievement level */
const val ACHIEVEMENTS_LEVEL_INCREMENT_STEP = 1

/** The base value hold for every achievement */
const val ACHIEVEMENTS_BASE_VALUE = 0

/**
 * Definition of an achievement category.
 *
 * This class describes:
 * - Which [achievementType] it represents.
 * - The list of [levelThresholds] that define when each level is reached.
 *
 * The semantics of [levelThresholds] are:
 * - The list contains thresholds for levels 2 through [ACHIEVEMENTS_LEVEL_NUMBER].
 * - If a value is below the first threshold, the user is on level 1.
 * - When the value becomes greater than or equal to a threshold, the corresponding level is
 *   reached.
 *
 * @param levelThresholds the threshold values defining the point at which each level beyond Level 1
 *   is reached.
 * @throws IllegalArgumentException if [levelThresholds] does not contain exactly
 *   `ACHIEVEMENTS_LEVEL_NUMBER - 1` entries.
 */
sealed class AchievementDefinition(val levelThresholds: List<Int>) {

  init {
    require(levelThresholds.size == ACHIEVEMENTS_LEVEL_NUMBER - 1) {
      "The number of thresholds should be equal to the number of levels - 1."
    }
  }
  /** The type of the achievement */
  abstract val achievementType: AchievementType

  /**
   * Computes the achievement level corresponding to the given [value].
   *
   * The returned level is always in the range `1..ACHIEVEMENTS_LEVEL_NUMBER`.
   *
   * @param value the current numeric value associated with this achievement.
   * @return the current level for the provided [value] in this achievement.
   */
  fun computeLevel(value: Int): Int {
    val index = levelThresholds.indexOfFirst { value < it }
    return if (index == -1) ACHIEVEMENTS_LEVEL_NUMBER else index + ACHIEVEMENTS_LEVEL_INCREMENT_STEP
  }
}

/**
 * Definition of the "number of plants" achievement.
 *
 * @property levelThresholds thresholds for each level beyond level 1.
 */
data class PlantsNumberAchievement(val thresholds: List<Int>) :
    AchievementDefinition(levelThresholds = thresholds) {
  override val achievementType = AchievementType.PLANTS_NUMBER
}

/**
 * Definition of the "number of friends" achievement.
 *
 * @property levelThresholds thresholds for each level beyond level 1.
 */
data class FriendsNumberAchievement(val thresholds: List<Int>) :
    AchievementDefinition(levelThresholds = thresholds) {
  override val achievementType = AchievementType.FRIENDS_NUMBER
}

/**
 * Definition of the "healthy streak" achievement.
 *
 * @property levelThresholds thresholds for each level beyond level 1.
 */
data class HealthyStreakAchievement(val thresholds: List<Int>) :
    AchievementDefinition(levelThresholds = thresholds) {
  override val achievementType = AchievementType.HEALTHY_STREAK
}

/** Central object that stores all achievements definitions and their threshold values. */
object Achievements {

  /** Thresholds used by the [AchievementType.PLANTS_NUMBER] achievement. */
  val PLANTS_NUMBER_THRESHOLDS: List<Int> = listOf(1, 3, 5, 10, 15, 20, 30, 40, 50)

  /** Thresholds used by the [AchievementType.FRIENDS_NUMBER] achievement. */
  val FRIENDS_NUMBER_THRESHOLDS: List<Int> = listOf(1, 3, 5, 10, 15, 20, 25, 30, 40)

  /** Thresholds used by the [AchievementType.HEALTHY_STREAK] achievement. */
  val HEALTHY_STREAK_THRESHOLDS: List<Int> = listOf(1, 3, 5, 7, 10, 20, 30, 40, 50)

  /** Definition instance for the [AchievementType.PLANTS_NUMBER] achievement. */
  val PLANTS_NUMBER: PlantsNumberAchievement = PlantsNumberAchievement(PLANTS_NUMBER_THRESHOLDS)

  /** Definition instance for the [AchievementType.FRIENDS_NUMBER] achievement. */
  val FRIENDS_NUMBER: FriendsNumberAchievement = FriendsNumberAchievement(FRIENDS_NUMBER_THRESHOLDS)

  /** Definition instance for the [AchievementType.HEALTHY_STREAK] achievement. */
  val HEALTHY_STREAK: HealthyStreakAchievement = HealthyStreakAchievement(HEALTHY_STREAK_THRESHOLDS)
}

/**
 * Data class that represents per-user progress for a specific [AchievementType].
 *
 * It is intended to be stored in a repository and combined with [AchievementDefinition] on the
 * client to compute the current level.
 *
 * The [currentValue] stored value has a different meaning depending on the [AchievementType].
 * - [AchievementType.PLANTS_NUMBER]: the maximum number of plants the user ever had simultaneously.
 * - [AchievementType.FRIENDS_NUMBER]: the maximum number of friends the user ever had
 *   simultaneously.
 * - [AchievementType.HEALTHY_STREAK]: the longest streak the user ever had for one of their plant.
 *
 * @property achievementType the type of achievement this progress refers to.
 * @property currentValue the current numeric value associated with this achievement for the user.
 *   The value
 */
@Keep
data class UserAchievementProgress(
    val achievementType: AchievementType = AchievementType.PLANTS_NUMBER,
    val currentValue: Int = ACHIEVEMENTS_BASE_VALUE
)
