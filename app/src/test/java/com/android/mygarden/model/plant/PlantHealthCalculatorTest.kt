package com.android.mygarden.model.plant

import java.sql.Timestamp
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlantHealthCalculatorTest {

  private lateinit var calculator: PlantHealthCalculator

  @Before
  fun setup() {
    calculator = PlantHealthCalculator()
  }

  // Tests WITHOUT previousLastWatered (standard behavior)

  @Test
  fun calculateHealthStatus_15PercentOfCycle_returnsOverwatered() {
    // Plant watered too recently (14% of cycle) should be overwatered
    val lastWatered = daysAgo(1.0) // 14% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.OVERWATERED, status)
  }

  @Test
  fun calculateHealthStatus_50PercentOfCycle_returnsHealthy() {
    // Plant in middle of watering cycle should be healthy
    val lastWatered = daysAgo(3.5) // 50% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_85PercentOfCycle_returnsSlightlyDry() {
    // Plant approaching next watering should be slightly dry
    val lastWatered = daysAgo(6.0) // 85% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, status)
  }

  @Test
  fun calculateHealthStatus_110PercentOfCycle_returnsNeedsWater() {
    // Plant overdue for watering should need water
    val lastWatered = daysAgo(7.7) // 110% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.NEEDS_WATER, status)
  }

  @Test
  fun calculateHealthStatus_150PercentOfCycle_returnsSeverelyDry() {
    // Plant neglected for too long should be severely dry
    val lastWatered = daysAgo(10.5) // 150% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.SEVERELY_DRY, status)
  }

  // Boundary tests

  @Test
  fun calculateHealthStatus_boundary10Percent_returnsOverwatered() {
    val lastWatered = daysAgo(0.71) // Exactly 10% of 7 days
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.OVERWATERED, status)
  }

  @Test
  fun calculateHealthStatus_boundary30Percent_returnsHealthy() {
    val lastWatered = daysAgo(2.1) // Exactly 30% of 7 days
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_boundary70Percent_returnsHealthy() {
    val lastWatered = daysAgo(4.9) // Exactly 70% of 7 days
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  // Tests WITH previousLastWatered (grace period logic)

  @Test
  fun calculateHealthStatus_justWateredAfterPlantNeededWater_returnsHealthy() {
    // Grace period: watering when plant needed it should return HEALTHY (not overwatered)
    // Plant was at 80% (needed water) → watered → should be HEALTHY (grace period)
    val wateringFrequency = 7
    val previousWatering = daysAgo(5.6) // 80% of cycle
    val currentWatering = Timestamp(System.currentTimeMillis())

    val status =
        calculator.calculateHealthStatus(
            lastWatered = currentWatering,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previousWatering)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_justWateredTooEarly_returnsSeverelyOverwatered() {
    // Watering too early (at 40%) should still be overwatered despite grace period
    // Plant was at 40% (too early) → watered → should be SEVERELY_OVERWATERED
    val wateringFrequency = 7
    val previousWatering = daysAgo(2.8) // 40% of cycle
    val currentWatering = Timestamp(System.currentTimeMillis())

    val status =
        calculator.calculateHealthStatus(
            lastWatered = currentWatering,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previousWatering)

    assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, status)
  }

  @Test
  fun calculateHealthStatus_after12Hours_gracePeriodExpires() {
    // Grace period only lasts 12 hours
    // Plant watered 13 hours ago → grace period expired
    val wateringFrequency = 7
    val previousWatering = daysAgo(8.0) // Needed water
    val currentWatering = daysAgo(0.54) // 13 hours ago

    val status =
        calculator.calculateHealthStatus(
            lastWatered = currentWatering,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previousWatering)

    assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, status)
  }

  @Test
  fun calculateHealthStatus_within12Hours_gracePeriodActive() {
    // Within 12 hours of watering, grace period protects from overwatered status
    // Plant watered 6 hours ago → grace period still active
    val wateringFrequency = 7
    val previousWatering = daysAgo(8.0) // Needed water
    val currentWatering = daysAgo(0.25) // 6 hours ago

    val status =
        calculator.calculateHealthStatus(
            lastWatered = currentWatering,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previousWatering)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  // Tests for initial watering grace period (first time adding a plant)

  @Test
  fun calculateHealthStatus_firstWateringToday_returnsHealthy() {
    // First watering gets grace period too (no previous watering)
    // Plant added for the first time, watered today (no previousLastWatered)
    val lastWatered = Timestamp(System.currentTimeMillis())
    val wateringFrequency = 7

    val status =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = null)

    // Should be HEALTHY (initial watering grace period), not SEVERELY_OVERWATERED
    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_firstWateringWithin12Hours_returnsHealthy() {
    // First watering within 12 hours also gets the initial watering grace period
    // Plant added recently, watered 6 hours ago (no previousLastWatered)
    val lastWatered = daysAgo(0.25) // 6 hours ago
    val wateringFrequency = 7

    val status =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = null)

    // Should be HEALTHY (initial watering grace period)
    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_firstWateringAfter12Hours_notInGracePeriod() {
    // Initial watering grace period only lasts 12 hours
    // Plant added, watered 1 day ago (no previousLastWatered, grace period expired)
    val lastWatered = daysAgo(1.0) // 14% of 7-day cycle
    val wateringFrequency = 7

    val status =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = null)

    // Grace period expired, should follow normal rules (OVERWATERED at 14%)
    assertEquals(PlantHealthStatus.OVERWATERED, status)
  }

  // Different watering frequencies

  @Test
  fun calculateHealthStatus_shortFrequency_calculatesCorrectly() {
    val lastWatered = daysAgo(1.0) // 50% of 2-day cycle
    val wateringFrequency = 2

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_longFrequency_calculatesCorrectly() {
    val lastWatered = daysAgo(15.0) // 50% of 30-day cycle
    val wateringFrequency = 30

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  /** Water Level Bar should be full (1.0) right after watering */
  @Test
  fun wateringFloatIsCorrectRightAfterWatering() {
    // Bar should be full immediately after watering
    val lastWatered = Timestamp(System.currentTimeMillis())
    val wateringFrequency = 7

    val fraction =
        calculator.calculateInStatusFloat(
            lastWatered = lastWatered, wateringFrequency = wateringFrequency)

    assertEquals(1.0f, fraction, 1e-6f)
  }

  /** Water Level Bar should be full (1.0) right after watering, even in the grace period */
  @Test
  fun wateringFloatIsCorrectRightAfterWateringInGracePeriod() {
    // Grace period doesn't affect the water bar visualization
    val wateringFrequency = 7
    val previousWatering = daysAgo(5.6) // 80% of cycle
    val currentWatering = daysAgo(0.1) // within 12h

    val status =
        calculator.calculateHealthStatus(
            lastWatered = currentWatering,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previousWatering)

    assertEquals(PlantHealthStatus.HEALTHY, status)

    val fraction =
        calculator.calculateInStatusFloat(
            lastWatered = currentWatering,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previousWatering)

    assertEquals(1.0f, fraction, 1e-6f)
  }

  /** Test that the bar is correctly mapped in the healthy case for the edge cases (30% and 70%) */
  @Test
  fun wateringFloatIsCorrectlyForBoundariesInHealthyMode() {
    // At 30% (healthy start): bar is full; at 70% (healthy end): bar is empty
    val wateringFrequency = 10

    val at30 = daysAgo(3.0) // 30% of cycle
    val at70 = daysAgo(7.0) // 70% of cycle

    val f30 = calculator.calculateInStatusFloat(at30, wateringFrequency)
    val f70 = calculator.calculateInStatusFloat(at70, wateringFrequency)

    assertEquals(1.0f, f30, 1e-6f)
    assertEquals(0.0f, f70, 1e-6f)
  }

  /** Every float should be decreasing (if it stays in the same status) as the time increases */
  @Test
  fun wateringFloatDecreasesCorrectly() {
    // Water bar should decrease over time
    val wateringFrequency = 10
    val at35 = daysAgo(3.5) // 35%
    val at50 = daysAgo(5.0) // 50%
    val at65 = daysAgo(6.5) // 65%

    val f35 = calculator.calculateInStatusFloat(at35, wateringFrequency)
    val f50 = calculator.calculateInStatusFloat(at50, wateringFrequency)
    val f65 = calculator.calculateInStatusFloat(at65, wateringFrequency)

    assertTrue("f35 should be > f50", f35 > f50)
    assertTrue("f50 should be > f65", f50 > f65)
    assertTrue("All should be within [0,1]", f35 in 0f..1f && f50 in 0f..1f && f65 in 0f..1f)
  }

  /**
   * Checks that when it transitions from one status to another, the fraction goes from low to high
   */
  @Test
  fun wateringFloatAdaptsWithStateTransition() {
    val wateringFrequency = 10
    val lastWateredHealthy = daysAgo(6.0)
    val healthyFraction = calculator.calculateInStatusFloat(lastWateredHealthy, wateringFrequency)
    val lastWateredNeedsWater = daysAgo(8.0)
    val needWaterFraction =
        calculator.calculateInStatusFloat(lastWateredNeedsWater, wateringFrequency)
    assertTrue(needWaterFraction >= healthyFraction) // Should be bigger in the next "state"
  }

  /** A plant abandoned for too long has a zero float */
  @Test
  fun wateringFloatIsCorrectForSeverelyDryPlant() {
    val wateringFrequency = 10
    val lastWatered = daysAgo(15.0) // 150%

    val fraction = calculator.calculateInStatusFloat(lastWatered, wateringFrequency)

    assertEquals(0.0f, fraction, 1e-6f)
  }

  /** A unknown state plant has a full watering bar by convention */
  @Test
  fun negativeWateringFrequencyDoesNotCrashAndTheWateringFloatIsOne() {
    val wateringFrequency = -5
    val lastWatered = daysAgo(100.0)

    val fraction = calculator.calculateInStatusFloat(lastWatered, wateringFrequency)

    assertEquals(1.0f, fraction, 1e-6f)
  }
}
