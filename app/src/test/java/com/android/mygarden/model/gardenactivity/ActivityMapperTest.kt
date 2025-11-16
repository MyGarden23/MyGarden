package com.android.mygarden.model.gardenactivity

import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.google.firebase.Timestamp
import java.sql.Timestamp as SqlTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityMapperTest {

  @Test
  fun testSerializeAndDeserializeAddedPlant() {
    // Create a test plant
    val plant =
        Plant(
            name = "Test Rose",
            latinName = "Rosa test",
            description = "A test rose",
            location = PlantLocation.OUTDOOR,
            lightExposure = "Full sun",
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "Looking good",
            wateringFrequency = 3,
            isRecognized = true,
            image = "https://example.com/rose.jpg")

    val ownedPlant =
        OwnedPlant(
            id = "plant123",
            plant = plant,
            lastWatered = SqlTimestamp(System.currentTimeMillis()),
            previousLastWatered = SqlTimestamp(System.currentTimeMillis() - 86400000))

    val timestamp = Timestamp.now()

    // Create activity
    val activity =
        GardenActivity.AddedPlant(
            userId = "user123", pseudo = "TestUser", timestamp = timestamp, ownedPlant = ownedPlant)

    // Serialize
    val serialized = ActivityMapper.fromActivityToSerializedActivity(activity)

    // Verify serialization
    assertTrue(serialized is SerializedAddedPlant)
    val serializedPlant = serialized as SerializedAddedPlant
    assertEquals("user123", serializedPlant.userId)
    assertEquals("ADDED_PLANT", serializedPlant.type)
    assertEquals("TestUser", serializedPlant.pseudo)
    assertEquals("plant123", serializedPlant.ownedPlant.id)
    assertEquals("Test Rose", serializedPlant.ownedPlant.plant.name)

    // Deserialize
    val deserialized = ActivityMapper.fromSerializedActivityToActivity(serialized)

    // Verify deserialization
    assertNotNull(deserialized)
    assertTrue(deserialized is GardenActivity.AddedPlant)
    val deserializedPlant = deserialized as GardenActivity.AddedPlant
    assertEquals("user123", deserializedPlant.userId)
    assertEquals("TestUser", deserializedPlant.pseudo)
    assertEquals(ActivityType.ADDED_PLANT, deserializedPlant.type)
    assertEquals("plant123", deserializedPlant.ownedPlant.id)
    assertEquals("Test Rose", deserializedPlant.ownedPlant.plant.name)
    assertEquals("Rosa test", deserializedPlant.ownedPlant.plant.latinName)
  }

  @Test
  fun testSerializeAndDeserializeAchievement() {
    val timestamp = Timestamp.now()

    // Create achievement activity
    val activity =
        GardenActivity.Achievement(
            userId = "user456", pseudo = "AchievementUser", timestamp = timestamp)

    // Serialize
    val serialized = ActivityMapper.fromActivityToSerializedActivity(activity)

    // Verify serialization
    assertTrue(serialized is SerializedAchievement)
    val serializedAchievement = serialized as SerializedAchievement
    assertEquals("user456", serializedAchievement.userId)
    assertEquals("ACHIEVEMENT", serializedAchievement.type)
    assertEquals("AchievementUser", serializedAchievement.pseudo)

    // Deserialize
    val deserialized = ActivityMapper.fromSerializedActivityToActivity(serialized)

    // Verify deserialization
    assertNotNull(deserialized)
    assertTrue(deserialized is GardenActivity.Achievement)
    val deserializedAchievement = deserialized as GardenActivity.Achievement
    assertEquals("user456", deserializedAchievement.userId)
    assertEquals("AchievementUser", deserializedAchievement.pseudo)
    assertEquals(ActivityType.ACHIEVEMENT, deserializedAchievement.type)
  }

  @Test
  fun testDeserializeUnknownActivityType() {
    // Create a serialized activity with unknown type
    val unknownActivity =
        object : SerializedActivity() {
          override val userId = "user789"
          override val type = "UNKNOWN_TYPE"
          override val pseudo = "UnknownUser"
          override val timestamp = Timestamp.now()
        }

    // Should return null for unknown types
    val deserialized = ActivityMapper.fromSerializedActivityToActivity(unknownActivity)
    assertNull(deserialized)
  }

  @Test
  fun testActivityTypesMatch() {
    val timestamp = Timestamp.now()

    val addedPlant =
        GardenActivity.AddedPlant(
            userId = "user1",
            pseudo = "User1",
            timestamp = timestamp,
            ownedPlant =
                OwnedPlant(
                    id = "p1",
                    plant = Plant(name = "Rose", latinName = "Rosa"),
                    lastWatered = SqlTimestamp(System.currentTimeMillis())))

    val achievement =
        GardenActivity.Achievement(userId = "user2", pseudo = "User2", timestamp = timestamp)

    assertEquals(ActivityType.ADDED_PLANT, addedPlant.type)
    assertEquals(ActivityType.ACHIEVEMENT, achievement.type)
  }
}
