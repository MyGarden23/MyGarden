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

  // Tests with a NULL previousLastWatered (plant has bee just added to garden)
  @Test
  fun calculateHealthStatus_cannotReturnOverwatered_whenJustAddedToGarden() {
    // Plant watered very recently should be healthy if just added to garden (no previous info)
    val lastWatered = daysAgo(1.0)
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)
    assertEquals(PlantHealthStatus.HEALTHY, status)

    val lastWatered2 = daysAgo(2.0)
    val status2 = calculator.calculateHealthStatus(lastWatered2, wateringFrequency)
    assertEquals(PlantHealthStatus.HEALTHY, status2)
  }

  @Test
  fun calculateHealthStatus_50PercentOfCycle_returnsHealthy_whenJustAddedToGarden() {
    // Plant watered at 50% should be healthy if just added to garden (no previous info)
    val lastWatered = daysAgo(3.5)
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_85PercentOfCycle_returnsSlightlyDry_whenJustAddedToGarden() {
    // Dryness should still be computed when just added to garden
    // Setup for slightly dry
    val lastWatered = daysAgo(6.0)
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, status)
  }

  @Test
  fun calculateHealthStatus_110PercentOfCycle_returnsNeedsWater_whenJustAddedToGarden() {
    // Dryness should still be computed when just added to garden
    // Setup for needs water
    val lastWatered = daysAgo(7.7)
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.NEEDS_WATER, status)
  }

  @Test
  fun calculateHealthStatus_150PercentOfCycle_returnsSeverelyDry_whenJustAddedToGarden() {
    // Dryness should still be computed when just added to garden
    // Setup for severly dry
    val lastWatered = daysAgo(10.5)
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.SEVERELY_DRY, status)
  }

  // Boundary tests (exactly the threshold) when just added
  @Test
  fun calculateHealthStatus_boundary70Percent_returnsHealthy_whenJustAddedToGarden() {
    val lastWatered = daysAgo(4.9) // Exactly 70% of 7 days
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_boundary100Percent_returnsSlightlyDry_whenJustAddedToGarden() {
    val lastWatered = daysAgo(7.0) // Exactly 100% of 7 days
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, status)
  }

  @Test
  fun calculateHealthStatus_boundary130Percent_returnsNeedsWater_whenJustAddedToGarden() {
    val lastWatered = daysAgo(9.1) // Exactly 130% of 7 days
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.NEEDS_WATER, status)
  }

  @Test
  fun calculateHealthStatus_justAfter130Percent_returnsSeverelyDry_whenJustAddedToGarden() {
    val lastWatered = daysAgo(9.2) // More than 130%
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.SEVERELY_DRY, status)
  }

  // Tests WITH previousLastWatered to test the overwatering part

  @Test
  fun calculateHealthStatus_justWateredAfterPlantNeededWater_returnsHealthy() {
    // Watering a plant when needed should return HEALTHY (not overwatered)
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
    // Watering too early in the cycle (at 30%) should be severy overwatered
    val wateringFrequency = 7
    val previousWatering = daysAgo(2.1) // 30% of cycle
    val currentWatering = Timestamp(System.currentTimeMillis())

    val status =
        calculator.calculateHealthStatus(
            lastWatered = currentWatering,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previousWatering)

    assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, status)
  }

  @Test
  fun calculateHealthStatus_waterPlantWhenNeedsWater_decayOverTime() {
    // Plant watered 6 hours ago
    val wateringFrequency = 7
    val last = daysAgo(0.25)
    val previous = daysAgo(8.0)

    // 8 days ago then watered again less than 1 day ago
    val status =
        calculator.calculateHealthStatus(
            lastWatered = last,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous)

    assertEquals(PlantHealthStatus.HEALTHY, status)

    // 60% of watering cycle later still healthy
    val status2 =
        calculator.calculateHealthStatus(
            lastWatered = last,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-4.2))

    assertEquals(PlantHealthStatus.HEALTHY, status2)

    // 80% of watering cycle later slightly dry
    val status3 =
        calculator.calculateHealthStatus(
            lastWatered = last,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-5.6))

    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, status3)

    // 110% of watering cycle later needs water
    val status4 =
        calculator.calculateHealthStatus(
            lastWatered = last,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-7.7))

    assertEquals(PlantHealthStatus.NEEDS_WATER, status4)

    // 140% of watering cycle later severely dry
    val status5 =
        calculator.calculateHealthStatus(
            lastWatered = last,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-9.8))

    assertEquals(PlantHealthStatus.SEVERELY_DRY, status5)
  }

  // Test for adding a plant with watering being today
  @Test
  fun calculateHealthStatus_firstWateringToday_returnsHealthy_whenJustAddedToGarden() {
    // Plant added for the first time, watered today (no previousLastWatered)
    val lastWatered = Timestamp(System.currentTimeMillis())
    val wateringFrequency = 7

    val status =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = null)

    // Should be HEALTHY initially
    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  // Different watering frequencies with a previous watering
  @Test
  fun calculateHealthStatus_shortFrequency_calculatesCorrectly() {
    val lastWatered = daysAgo(1.0)
    val previous = daysAgo(3.0) // Last water was correct (i.e the length of the frequency)
    val wateringFrequency = 2

    val status =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_longFrequency_calculatesCorrectly() {
    val lastWatered = daysAgo(15.0)
    val previous = daysAgo(45.0) // Last water was correct (i.e the length of the frequency)
    val wateringFrequency = 30

    val status =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  // OVERWATER/SEVERELY_OVERWATERED tests
  @Test
  fun calculateHealthStatus_averageFrequency_overwateringWorksAndDecay() {
    val lastWatered = daysAgo(2.0)
    val previous = daysAgo(5.5) // watered at ~50% of the cycle before
    val wateringFrequency = 7

    // Right after overwatering is overwatered because the "overwaterness"
    // has occurred late in the cycle (hence not severely overwatered)
    val statusOverwatered =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(0.5))

    // Decays to healthy after 30% of the cycle has passed
    val statusHealthy =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-0.5))

    // Decays normally after
    val statusSlightlyDry =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-3.0))

    val statusNeedsWater =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-5.1))

    val statusSeverelyDry =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-7.8))

    // assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, statusSeverelyOverwatered)
    assertEquals(PlantHealthStatus.OVERWATERED, statusOverwatered)
    assertEquals(PlantHealthStatus.HEALTHY, statusHealthy)
    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, statusSlightlyDry)
    assertEquals(PlantHealthStatus.NEEDS_WATER, statusNeedsWater)
    assertEquals(PlantHealthStatus.SEVERELY_DRY, statusSeverelyDry)
  }

  @Test
  fun calculateHealthStatus_averageFrequency_goesToSeverelyOverwatered_whenDoubleWatering() {
    val lastWatered = daysAgo(1.0)
    val previous = daysAgo(2.0) // Watered 2 days in a row
    val wateringFrequency = 7

    // Right after overwatering is severely overwatered
    val status =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous)

    assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, status)
  }

  @Test
  fun calculateHealthStatus_lowFrequency_severelyOverwateredThenDecay() {
    val wateringFrequency = 3

    // Make the last watering happen very soon after the previous one: ~10% of the cycle
    val lastWatered = daysAgo(1.0)
    val previous = daysAgo(1.3)

    // Really soon overwatering must be severe
    val statusSeverelyOverwatered =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(0.7))

    // Past severe but still overwatered between 15% and 30% of cycle
    val statusOverwatered =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(0.55))

    // After 30% of cycle HEALTHY
    val statusHealthy =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(0.1))

    // Continue normal drying progression
    val statusSlightlyDry =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-1.11))

    val statusNeedsWater =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-2.1))

    val statusSeverelyDry =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-3.0))

    assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, statusSeverelyOverwatered)
    assertEquals(PlantHealthStatus.OVERWATERED, statusOverwatered)
    assertEquals(PlantHealthStatus.HEALTHY, statusHealthy)
    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, statusSlightlyDry)
    assertEquals(PlantHealthStatus.NEEDS_WATER, statusNeedsWater)
    assertEquals(PlantHealthStatus.SEVERELY_DRY, statusSeverelyDry)
  }

  @Test
  fun calculateHealthStatus_longFrequency_severelyOverwateredThenDecay() {
    val wateringFrequency = 20

    // Same as previous test with a long freq (20 days)
    val lastWatered = daysAgo(6.0)
    val previous = daysAgo(8.0)

    val statusSeverelyOverwatered =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(4.0))

    val statusOverwatered =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(3.0))

    val statusHealthy =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(0.0))

    val statusSlightlyDry =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-8.1))

    val statusNeedsWater =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-14.1))

    val statusSeverelyDry =
        calculator.calculateHealthStatus(
            lastWatered = lastWatered,
            wateringFrequency = wateringFrequency,
            previousLastWatered = previous,
            currentTime = daysAgo(-20.1))

    assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, statusSeverelyOverwatered)
    assertEquals(PlantHealthStatus.OVERWATERED, statusOverwatered)
    assertEquals(PlantHealthStatus.HEALTHY, statusHealthy)
    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, statusSlightlyDry)
    assertEquals(PlantHealthStatus.NEEDS_WATER, statusNeedsWater)
    assertEquals(PlantHealthStatus.SEVERELY_DRY, statusSeverelyDry)
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

  /** Water Level Bar should be full (1.0) right after watering */
  @Test
  fun wateringFloatIsCorrectRightAfterWatering_withPreviousWatering() {
    val wateringFrequency = 7
    val previousWatering = daysAgo(5.6) // 80% of cycle
    val currentWatering = daysAgo(0.0)

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

  /**
   * Test that the bar is correctly mapped in the healthy case for the edge cases (approximately)
   */
  @Test
  fun wateringFloatIsCorrectlyForBoundariesInHealthyMode_withPreviousWatering() {
    val wateringFrequency = 10

    val at70 = daysAgo(7.0)
    val at100 = daysAgo(9.9999999) // More or less at the boundaries
    val at130 = daysAgo(12.999999)

    val f70 = calculator.calculateInStatusFloat(at70, wateringFrequency)
    val f100 = calculator.calculateInStatusFloat(at100, wateringFrequency)
    val f130 = calculator.calculateInStatusFloat(at130, wateringFrequency)

    assertEquals(0.0f, f70, 1e-6f)
    assertEquals(0.0f, f100, 1e-6f)
    assertEquals(0.0f, f130, 1e-6f)
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
