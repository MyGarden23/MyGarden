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

  /** Double variable used to track each owned plant's "percentage" inside of its status */
  private var currentStatusPercentage = 0.0

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
      currentStatusPercentage = 0.0
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
        currentStatusPercentage =
            calculateRelativePercentage(
                x = OVERWATERED_THRESHOLD, y = HEALTHY_MAX_THRESHOLD, z = percentageOfCycle)
        return PlantHealthStatus.HEALTHY
      }
    }

    // Standard status determination based on percentage of watering cycle
    return when {
      // Watered very recently (less than 10% of cycle) - severely overwatered
      percentageOfCycle < SEVERELY_OVERWATERED_THRESHOLD -> {
        currentStatusPercentage =
            calculateRelativePercentage(
                x = 0.0, y = SEVERELY_OVERWATERED_THRESHOLD, z = percentageOfCycle)
        PlantHealthStatus.SEVERELY_OVERWATERED
      }

      // Watered too recently (10-30% of cycle) - overwatered
      percentageOfCycle < OVERWATERED_THRESHOLD -> {
        currentStatusPercentage =
            calculateRelativePercentage(
                x = SEVERELY_OVERWATERED_THRESHOLD,
                y = OVERWATERED_THRESHOLD,
                z = percentageOfCycle)
        PlantHealthStatus.OVERWATERED
      }

      // Healthy range (30-70% of cycle)
      percentageOfCycle <= HEALTHY_MAX_THRESHOLD -> {
        currentStatusPercentage =
            calculateRelativePercentage(
                x = OVERWATERED_THRESHOLD, y = HEALTHY_MAX_THRESHOLD, z = percentageOfCycle)
        PlantHealthStatus.HEALTHY
      }

      // Starting to dry out (70-100% of cycle)
      percentageOfCycle <= SLIGHTLY_DRY_MAX_THRESHOLD -> {
        currentStatusPercentage =
            calculateRelativePercentage(
                x = HEALTHY_MAX_THRESHOLD, y = SLIGHTLY_DRY_MAX_THRESHOLD, z = percentageOfCycle)
        PlantHealthStatus.SLIGHTLY_DRY
      }

      // Needs water (100-130% of cycle)
      percentageOfCycle <= NEEDS_WATER_MAX_THRESHOLD -> {
        currentStatusPercentage =
            calculateRelativePercentage(
                x = SLIGHTLY_DRY_MAX_THRESHOLD,
                y = NEEDS_WATER_MAX_THRESHOLD,
                z = percentageOfCycle)
        PlantHealthStatus.NEEDS_WATER
      }

      // Critical - severely dry (>130% of cycle)
      percentageOfCycle > NEEDS_WATER_MAX_THRESHOLD -> {
        currentStatusPercentage = 1.0
        PlantHealthStatus.SEVERELY_DRY
      }
      else -> {
        currentStatusPercentage = 0.0
        PlantHealthStatus.UNKNOWN
      }
    }
  }

  /**
   * Used to calculate where the given owned plant currently is in its watering status. This float
   * is used to display the percentage on the water level bar.
   *
   * @param lastWatered the last time this plant was watered
   * @param wateringFrequency the watering frequency of the given plant we want to water (should be
   *   bigger than 0)
   *     @param previousLastWatered the previously water field of the owned plant (used to compute
   *       the health status)
   * @param currentTime the current time (at which we watered the plant)
   * @return a Float that represents the percentage of the watering cycle that has been completed
   */
  fun calculateInStatusFloat(
      lastWatered: Timestamp,
      wateringFrequency: Int,
      previousLastWatered: Timestamp? = null,
      currentTime: Timestamp = Timestamp(System.currentTimeMillis())
  ): Float {
    calculateHealthStatus(lastWatered, wateringFrequency, previousLastWatered, currentTime)
    return 1f - currentStatusPercentage.toFloat()
  }

  /**
   * Helper function that calculates the relative position of z within a given interval [x, y].
   *
   * The result is normalized to a 0–1 range:
   * - Returns 0.0 if z == x or 1.0 if z == y
   * - Returns a proportional fraction otherwise, i.e. (z - x) / (y - x)
   *
   * @param x the lower bound of the interval (start of the range)
   * @param y the upper bound of the interval (end of the range)
   * @param z the value whose relative position is being calculated (should satisfy x ≤ z ≤ y)
   * @return the normalized value between 0.0 and 1.0 representing the relative position of z
   */
  private fun calculateRelativePercentage(x: Double, y: Double, z: Double): Double {
    return (z.coerceIn(x, y) - x) / (y - x)
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
