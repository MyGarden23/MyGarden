package com.android.mygarden.model.achievements

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AchievementsTests {

  // Test that the correct value is required for the threshold lists
  @Test
  fun achievementDefinitionNeedsTheRightNumberOfThresholds() {
    // Too few thresholds
    assertThrows(IllegalArgumentException::class.java) { PlantsNumberAchievement(listOf(1, 3)) }
    // Too many thresholds
    assertThrows(IllegalArgumentException::class.java) {
      FriendsNumberAchievement(List(ACHIEVEMENTS_LEVEL_NUMBER) { it })
    }
    // Correct size should not throw
    PlantsNumberAchievement(List(ACHIEVEMENTS_LEVEL_NUMBER - 1) { it })
  }

  // Test that the level is 1 for each achievement with value < first threshold
  @Test
  fun computeLevelReturnsOneWhenValueIsBelowFirstThreshold() {
    assertEquals(Achievements.PLANTS_NUMBER.computeLevel(0), 1)
    assertEquals(Achievements.FRIENDS_NUMBER.computeLevel(0), 1)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(0), 1)
  }

  // Test that the healthy streak computeLevel function works with arbitrary values
  @Test
  fun computeLevelReturnsRightValueForArbitraryTestCasesForHealthyStreak() {
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(0), 1)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(1), 2)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(3), 3)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(5), 4)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(8), 5)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(15), 6)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(25), 7)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(30), 8)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(40), 9)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(50), 10)
    assertEquals(Achievements.HEALTHY_STREAK.computeLevel(150), 10)
  }
}
