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
    /* Overwatering thresholds */
    private const val SEVERELY_OVERWATERED_MAX_THRESHOLD = 30.0
    // Watering before 70% is "too soon"
    private const val OVERWATERED_MAX_THRESHOLD = 70.0

    /* Dryness thresholds */
    private const val HEALTHY_MAX_THRESHOLD = 70.0
    private const val SLIGHTLY_DRY_MAX_THRESHOLD = 100.0
    private const val NEEDS_WATER_MAX_THRESHOLD = 130.0

    /* This threshold corresponds to the percentage of the watering frequency where the plant
    stops being overwatered when there was a overwatering that happened beforehand */
    private const val OVERWATER_STATE_RECOVERY_END_THRESHOLD = 30.0

    /* This threshold corresponds to the severity level above which the remaining overwatering is considered as severe rather than normal */
    private const val OVERWATERING_SEVERITY_LEVEL_THRESHOLD = 0.5

    private const val PERCENTAGE_CALCULATION_UTILITY = 100.0

    /** Min/max percentage values */
    private const val MIN_PERCENTAGE = 0.0
    private const val MAX_PERCENTAGE = 1.0

    /** Float conversion value */
    private const val FLOAT_CONVERSION = 1f
  }

  /** Double variable used to track each owned plant's "percentage" inside of its status */
  private var currentStatusPercentage = MIN_PERCENTAGE

  /**
   * Computes the current health status of a plant based on its watering frequency and the previous
   * waterings. The algorithm has two possible effects:
   * - Dryness, which corresponds to how long it has been since the last watering. This always
   *   progresses forward in time and determines the normal states of a plant's lifecycle (HEALTHY →
   *   SLIGHTLY_DRY → NEEDS_WATER → SEVERELY_DRY).
   * - Overwatering, which is driven by whether the last watering happened too soon compared to the
   *   previous one. If present, this "stress" dominates the dryness logic and gradually decays over
   *   time to eventually disappear.
   *
   * Overwatering severity is computed from the interval between the last two waterings (relative to
   * the watering frequency), then decays smoothly as the plant dries more and more. This guarantees
   * realistic transitions between the states: SEVERELY_OVERWATERED → OVERWATERED → HEALTHY →
   * SLIGHTLY_DRY → NEEDS_WATER → SEVERELY_DRY
   *
   * A plant with no previous watering is never considered overwatered, because it means that is has
   * just been added to the garden and without further information it is not possible to determine
   * this.
   *
   * In addition to the discrete [PlantHealthStatus], this function also updates an internal
   * percentage representing the plant's progress within the current status range, used by the UI to
   * render a smooth water level bar.
   *
   * @param lastWatered The Timestamp of the most recent watering
   * @param wateringFrequency The expected number of days between each waterings (must be > 0)
   * @param previousLastWatered The timestamp of the watering before the last one, or null if the
   *   plant has never been watered in the system
   * @param currentTime The time at which the status is evaluated (defaults to now)
   * @return The current [PlantHealthStatus] of the plant after the update
   */
  fun calculateHealthStatus(
      lastWatered: Timestamp,
      wateringFrequency: Int,
      previousLastWatered: Timestamp? = null,
      currentTime: Timestamp = Timestamp(System.currentTimeMillis())
  ): PlantHealthStatus {

    // Validation: Invalid watering frequency
    if (wateringFrequency <= 0) {
      currentStatusPercentage = MIN_PERCENTAGE
      return PlantHealthStatus.UNKNOWN
    }

    // Dryness computation (always meaningful)
    val daysSinceWatered = maxOf(calculateDaysDifference(lastWatered, currentTime), 0.0)
    val drynessPct = (daysSinceWatered / wateringFrequency) * PERCENTAGE_CALCULATION_UTILITY

    // Overwatering computation (based on the interval between last two waterings)
    val intervalPct =
        if (previousLastWatered != null) {
          val daysBetween = maxOf(calculateDaysDifference(previousLastWatered, lastWatered), 0.0)
          (daysBetween / wateringFrequency) * PERCENTAGE_CALCULATION_UTILITY
        } else {
          // Plant has no previous watering -> cannot be overwatered
          null
        }

    /* Starting overwater severity (smoothen between 0 and 1).
     *  The closer to 1, the more critically overwatered is the plant. */
    val startingOverwaterSeverity =
        if (intervalPct != null) {
          when {
            intervalPct < SEVERELY_OVERWATERED_MAX_THRESHOLD -> MAX_PERCENTAGE
            intervalPct < OVERWATERED_MAX_THRESHOLD -> {
              // Smoothen between 0 and 1
              MAX_PERCENTAGE -
                  calculateRelativePercentage(
                      x = SEVERELY_OVERWATERED_MAX_THRESHOLD,
                      y = OVERWATERED_MAX_THRESHOLD,
                      z = intervalPct)
            }
            else -> MIN_PERCENTAGE
          }
        } else {
          MIN_PERCENTAGE
        }

    // How much the overwatering is still present
    val overwaterDecay =
        (MAX_PERCENTAGE - (drynessPct / OVERWATER_STATE_RECOVERY_END_THRESHOLD)).coerceIn(
            MIN_PERCENTAGE, MAX_PERCENTAGE)

    /* Effective severity of the overwatering at the current time
     * This is 0 if there is not overwatering left and 1 if it is at maximum */
    val effectiveOverwaterSeverity = startingOverwaterSeverity * overwaterDecay

    // If there is still some overwatering, return OVERWATERED/SEVERELY_OVERWATERED
    if (effectiveOverwaterSeverity > MIN_PERCENTAGE) {
      return if (effectiveOverwaterSeverity > OVERWATERING_SEVERITY_LEVEL_THRESHOLD) {
        // If the severity is still higher than 0.5 -> SEVERELY_OVERWATERED
        currentStatusPercentage =
            MAX_PERCENTAGE -
                calculateRelativePercentage(
                    x = OVERWATERING_SEVERITY_LEVEL_THRESHOLD,
                    y = MAX_PERCENTAGE,
                    z = effectiveOverwaterSeverity)
        PlantHealthStatus.SEVERELY_OVERWATERED
      } else {
        // If the severity has decayed down to smaller than 0.5 -> OVERWATERED
        currentStatusPercentage =
            MAX_PERCENTAGE -
                calculateRelativePercentage(
                    x = MIN_PERCENTAGE,
                    y = OVERWATERING_SEVERITY_LEVEL_THRESHOLD,
                    z = effectiveOverwaterSeverity)
        PlantHealthStatus.OVERWATERED
      }
    }

    // When not overwatered anymore -> use the dryness computation
    return when {
      drynessPct <= HEALTHY_MAX_THRESHOLD -> {
        currentStatusPercentage =
            calculateRelativePercentage(
                x = MIN_PERCENTAGE, y = HEALTHY_MAX_THRESHOLD, z = drynessPct)
        PlantHealthStatus.HEALTHY
      }
      drynessPct <= SLIGHTLY_DRY_MAX_THRESHOLD -> {
        currentStatusPercentage =
            calculateRelativePercentage(
                x = HEALTHY_MAX_THRESHOLD, y = SLIGHTLY_DRY_MAX_THRESHOLD, z = drynessPct)
        PlantHealthStatus.SLIGHTLY_DRY
      }
      drynessPct <= NEEDS_WATER_MAX_THRESHOLD -> {
        currentStatusPercentage =
            calculateRelativePercentage(
                x = SLIGHTLY_DRY_MAX_THRESHOLD, y = NEEDS_WATER_MAX_THRESHOLD, z = drynessPct)
        PlantHealthStatus.NEEDS_WATER
      }
      else -> {
        currentStatusPercentage = MAX_PERCENTAGE
        PlantHealthStatus.SEVERELY_DRY
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
    return FLOAT_CONVERSION - currentStatusPercentage.toFloat()
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
