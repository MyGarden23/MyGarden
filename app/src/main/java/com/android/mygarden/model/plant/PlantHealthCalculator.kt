package com.android.mygarden.model.plant

import java.sql.Timestamp
import java.util.concurrent.TimeUnit

/**
 * Calculator for determining plant health status based on watering patterns.
 *
 * This class analyzes the time since last watering to determine if a plant is healthy, needs water,
 * or is being overwatered.
 */
class PlantHealthCalculator {

  companion object {
    /** Threshold percentages for watering cycle status determination */
    private const val SEVERELY_OVERWATERED_THRESHOLD = 10.0
    private const val OVERWATERED_THRESHOLD = 30.0
    private const val HEALTHY_MAX_THRESHOLD = 70.0
    private const val SLIGHTLY_DRY_MAX_THRESHOLD = 100.0
    private const val NEEDS_WATER_MAX_THRESHOLD = 130.0
    private const val JUST_WATERED_GRACE_PERIOD_DAYS = 0.5 // 12 hours

    private const val PERCENTAGE_CALCULATION_UTILITY = 100.0
  }

  /**
   * Calculates the current health status of a plant based on watering cycle.
   *
   * Status is determined by the percentage of watering cycle completed:
   * - 0-10%: Severely overwatered | 10-30%: Overwatered | 30-70%: Healthy
   * - 70-100%: Slightly dry | 100-130%: Needs water | 130%+: Severely dry
   *
   * Grace Period: If watered within 12h and plant was at ≥70% of cycle (needed water), status is
   * HEALTHY instead of SEVERELY_OVERWATERED. This requires previousLastWatered to determine if
   * watering was appropriate or premature.
   *
   * @param lastWatered Timestamp of when the plant was last watered
   * @param wateringFrequency Expected number of days between waterings
   * @param previousLastWatered Optional: Previous watering timestamp (for grace period calculation)
   * @param currentTime Current time (defaults to now, can be overridden for testing)
   * @return The calculated PlantHealthStatus
   */
  fun calculateHealthStatus(
      lastWatered: Timestamp,
      wateringFrequency: Int,
      previousLastWatered: Timestamp? = null,
      currentTime: Timestamp = Timestamp(System.currentTimeMillis())
  ): PlantHealthStatus {

    // Validation: Invalid watering frequency
    if (wateringFrequency <= 0) {
      return PlantHealthStatus.UNKNOWN
    }

    // Calculate days since last watered
    val daysSinceWatered = calculateDaysDifference(lastWatered, currentTime)
    // Calculate percentage of watering cycle completed
    val percentageOfCycle = (daysSinceWatered / wateringFrequency) * PERCENTAGE_CALCULATION_UTILITY
    // Special handling: Grace period after appropriate watering
    if (daysSinceWatered <= JUST_WATERED_GRACE_PERIOD_DAYS && previousLastWatered != null) {
      val daysSincePreviousWatering = calculateDaysDifference(previousLastWatered, lastWatered)
      val previousPercentage =
          (daysSincePreviousWatering / wateringFrequency) * PERCENTAGE_CALCULATION_UTILITY

      if (previousPercentage >= HEALTHY_MAX_THRESHOLD) {
        return PlantHealthStatus.HEALTHY
      }
    }

    // Standard status determination based on percentage of watering cycle
    return when {
      // Watered very recently (less than 10% of cycle) - severely overwatered
      percentageOfCycle < SEVERELY_OVERWATERED_THRESHOLD -> PlantHealthStatus.SEVERELY_OVERWATERED

      // Watered too recently (10-30% of cycle) - overwatered
      percentageOfCycle < OVERWATERED_THRESHOLD -> PlantHealthStatus.OVERWATERED

      // Healthy range (30-70% of cycle)
      percentageOfCycle <= HEALTHY_MAX_THRESHOLD -> PlantHealthStatus.HEALTHY

      // Starting to dry out (70-100% of cycle)
      percentageOfCycle <= SLIGHTLY_DRY_MAX_THRESHOLD -> PlantHealthStatus.SLIGHTLY_DRY

      // Needs water (100-130% of cycle)
      percentageOfCycle <= NEEDS_WATER_MAX_THRESHOLD -> PlantHealthStatus.NEEDS_WATER

      // Critical - severely dry (>130% of cycle)
      percentageOfCycle > NEEDS_WATER_MAX_THRESHOLD -> PlantHealthStatus.SEVERELY_DRY
      else -> PlantHealthStatus.UNKNOWN
    }
  }

  /**
   * Calculates the difference in days between two timestamps.
   *
   * @param earlier The earlier timestamp
   * @param later The later timestamp
   * @return The number of days between the timestamps (can be fractional)
   */
  private fun calculateDaysDifference(earlier: Timestamp, later: Timestamp): Double {
    val diffInMillis = later.time - earlier.time
    return diffInMillis.toDouble() / TimeUnit.DAYS.toMillis(1)
  }
}
