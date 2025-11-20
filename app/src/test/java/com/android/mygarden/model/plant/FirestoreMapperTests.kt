package com.android.mygarden.model.plant

import java.sql.Timestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Simple unit test that tests the Firestore Mapper object behaviour. They test that
 * plants/owedPlants are the same when serialized then deserialized.
 */
class FirestoreMapperTests {

  private val recognizedPlant =
      Plant(
          name = "Rose",
          image = null,
          latinName = "Rosalia",
          location = PlantLocation.OUTDOOR,
          lightExposure = "Light at least 6 hours a day.",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "This is a good description.",
          wateringFrequency = 3,
          isRecognized = true)

  private val unrecognizedPlant = Plant(isRecognized = false)

  private val ownedPlantRecognized =
      OwnedPlant(
          id = "1", plant = recognizedPlant, lastWatered = Timestamp(System.currentTimeMillis()))

  private val ownedPlantUnrecognized =
      OwnedPlant(
          id = "2", plant = unrecognizedPlant, lastWatered = Timestamp(System.currentTimeMillis()))

  @Test
  fun firestoreMapper_correctly_maps_recognized_plant() = runTest {
    val sPlant = FirestoreMapper.fromPlantToSerializedPlant(recognizedPlant)
    val out = FirestoreMapper.fromSerializedPlantToPlant(sPlant)
    assertEquals(recognizedPlant, out)
  }

  @Test
  fun firestoreMapper_correctly_maps_unrecognized_plant() = runTest {
    val sPlant = FirestoreMapper.fromPlantToSerializedPlant(unrecognizedPlant)
    val out = FirestoreMapper.fromSerializedPlantToPlant(sPlant)
    assertEquals(unrecognizedPlant, out)
  }

  @Test
  fun firestoreMapper_correctly_maps_recognized_ownedPlant() = runTest {
    val sOwnedPlant = FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant(ownedPlantRecognized)
    val out = FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant(sOwnedPlant)
    assertEquals(ownedPlantRecognized, out)
  }

  @Test
  fun firestoreMapper_correctly_maps_unrecognized_ownedPlant() = runTest {
    val sOwnedPlant = FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant(ownedPlantUnrecognized)
    val out = FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant(sOwnedPlant)
    assertEquals(ownedPlantUnrecognized, out)
  }
}
