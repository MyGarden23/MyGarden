package com.android.mygarden.model.gardenactivity

import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activitiyclasses.ActivityWaterPlant
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedAchievement
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedAddFriend
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedAddedPlant
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedWaterPlant
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
        ActivityAddedPlant(
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
    assertTrue(deserialized is ActivityAddedPlant)
    val deserializedPlant = deserialized as ActivityAddedPlant
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
        ActivityAchievement(
            userId = "user456",
            pseudo = "AchievementUser",
            timestamp = timestamp,
            achievementName = "Gained Badge")

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
    assertTrue(deserialized is ActivityAchievement)
    val deserializedActivityAchievement = deserialized as ActivityAchievement
    assertEquals("user456", deserializedActivityAchievement.userId)
    assertEquals("AchievementUser", deserializedActivityAchievement.pseudo)
    assertEquals(ActivityType.ACHIEVEMENT, deserializedActivityAchievement.type)
  }

  @Test
  fun testActivityTypesMatch() {
    val timestamp = Timestamp.now()

    val activityAddedPlant =
        ActivityAddedPlant(
            userId = "user1",
            pseudo = "User1",
            timestamp = timestamp,
            ownedPlant =
                OwnedPlant(
                    id = "p1",
                    plant = Plant(name = "Rose", latinName = "Rosa"),
                    lastWatered = SqlTimestamp(System.currentTimeMillis())))

    val activityAchievement =
        ActivityAchievement(
            userId = "user2",
            pseudo = "User2",
            timestamp = timestamp,
            achievementName = "Gained Badge")

    assertEquals(ActivityType.ADDED_PLANT, activityAddedPlant.type)
    assertEquals(ActivityType.ACHIEVEMENT, activityAchievement.type)
  }

  @Test
  fun testSerializeAndDeserializeAddFriend() {
    val timestamp = Timestamp.now()

    val activity =
        ActivityAddFriend(
            userId = "userFriend",
            pseudo = "FriendUser",
            timestamp = timestamp,
            friendId = "friend123")

    val serialized = ActivityMapper.fromActivityToSerializedActivity(activity)

    assertTrue(serialized is SerializedAddFriend)
    val serializedFriend = serialized as SerializedAddFriend
    assertEquals("userFriend", serializedFriend.userId)
    assertEquals("ADDED_FRIEND", serializedFriend.type)
    assertEquals("FriendUser", serializedFriend.pseudo)
    assertEquals("friend123", serializedFriend.friendId)

    val deserialized = ActivityMapper.fromSerializedActivityToActivity(serialized)

    assertNotNull(deserialized)
    assertTrue(deserialized is ActivityAddFriend)
    val deserializedFriend = deserialized as ActivityAddFriend
    assertEquals("userFriend", deserializedFriend.userId)
    assertEquals("FriendUser", deserializedFriend.pseudo)
    assertEquals(ActivityType.ADDED_FRIEND, deserializedFriend.type)
    assertEquals("friend123", deserializedFriend.friendId)
  }

  @Test
  fun testSerializeAndDeserializeWaterPlant() {
    val plant =
        Plant(
            name = "Watered Plant",
            latinName = "Aqua planta",
            description = "A plant used for watering test",
            location = PlantLocation.INDOOR,
            lightExposure = "Partial shade",
            healthStatus = PlantHealthStatus.HEALTHY,
            healthStatusDescription = "Hydrated",
            wateringFrequency = 5,
            isRecognized = true,
            image = "https://example.com/watered.jpg")

    val ownedPlant =
        OwnedPlant(
            id = "ownedWatered123",
            plant = plant,
            lastWatered = SqlTimestamp(System.currentTimeMillis()),
            previousLastWatered = SqlTimestamp(System.currentTimeMillis() - 3600000))

    val timestamp = Timestamp.now()

    val activity =
        ActivityWaterPlant(
            userId = "userWater",
            pseudo = "WaterUser",
            timestamp = timestamp,
            ownedPlant = ownedPlant)

    val serialized = ActivityMapper.fromActivityToSerializedActivity(activity)

    assertTrue(serialized is SerializedWaterPlant)
    val serializedWater = serialized as SerializedWaterPlant
    assertEquals("userWater", serializedWater.userId)
    assertEquals("WATERED_PLANT", serializedWater.type)
    assertEquals("WaterUser", serializedWater.pseudo)
    assertEquals("ownedWatered123", serializedWater.ownedPlant.id)
    assertEquals("Watered Plant", serializedWater.ownedPlant.plant.name)

    val deserialized = ActivityMapper.fromSerializedActivityToActivity(serialized)

    assertNotNull(deserialized)
    assertTrue(deserialized is ActivityWaterPlant)
    val deserializedWater = deserialized as ActivityWaterPlant
    assertEquals("userWater", deserializedWater.userId)
    assertEquals("WaterUser", deserializedWater.pseudo)
    assertEquals(ActivityType.WATERED_PLANT, deserializedWater.type)
    assertEquals("ownedWatered123", deserializedWater.ownedPlant.id)
    assertEquals("Watered Plant", deserializedWater.ownedPlant.plant.name)
  }

  @Test
  fun testMapTypeToSerializedClassKnownTypes() {
    assertEquals(
        SerializedAddedPlant::class.java, ActivityMapper.mapTypeToSerializedClass("ADDED_PLANT"))
    assertEquals(
        SerializedAchievement::class.java, ActivityMapper.mapTypeToSerializedClass("ACHIEVEMENT"))
  }

  @Test
  fun testMapTypeToSerializedClassUnknownType() {
    assertNull(ActivityMapper.mapTypeToSerializedClass("UNKNOWN_TYPE"))
  }
}
