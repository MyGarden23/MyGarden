package com.android.mygarden.model.plant

import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlantHealthCalculatorTest {

  private lateinit var calculator: PlantHealthCalculator

  @Before
  fun setup() {
    calculator = PlantHealthCalculator()
  }

  // Helper function to create a timestamp X days ago
  private fun daysAgo(days: Double): Timestamp {
    val millisAgo = (days * TimeUnit.DAYS.toMillis(1)).toLong()
    return Timestamp(System.currentTimeMillis() - millisAgo)
  }

  // Tests WITHOUT previousLastWatered (standard behavior)

  @Test
  fun calculateHealthStatus_0PercentOfCycle_returnsSeverelyOverwatered() {
    val lastWatered = Timestamp(System.currentTimeMillis())
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.SEVERELY_OVERWATERED, status)
  }

  @Test
  fun calculateHealthStatus_15PercentOfCycle_returnsOverwatered() {
    val lastWatered = daysAgo(1.0) // 14% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.OVERWATERED, status)
  }

  @Test
  fun calculateHealthStatus_50PercentOfCycle_returnsHealthy() {
    val lastWatered = daysAgo(3.5) // 50% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.HEALTHY, status)
  }

  @Test
  fun calculateHealthStatus_85PercentOfCycle_returnsSlightlyDry() {
    val lastWatered = daysAgo(6.0) // 85% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.SLIGHTLY_DRY, status)
  }

  @Test
  fun calculateHealthStatus_110PercentOfCycle_returnsNeedsWater() {
    val lastWatered = daysAgo(7.7) // 110% of 7-day cycle
    val wateringFrequency = 7

    val status = calculator.calculateHealthStatus(lastWatered, wateringFrequency)

    assertEquals(PlantHealthStatus.NEEDS_WATER, status)
  }

  @Test
  fun calculateHealthStatus_150PercentOfCycle_returnsSeverelyDry() {
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
}
