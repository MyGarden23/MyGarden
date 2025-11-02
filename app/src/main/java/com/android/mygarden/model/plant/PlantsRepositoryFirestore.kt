package com.android.mygarden.model.plant

import com.android.mygarden.model.plant.FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant
import com.android.mygarden.model.plant.FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.sql.Timestamp
import kotlinx.coroutines.tasks.await

/** Constant value for the collection of users in firestore. */
private const val usersCollection = "users"

/** Constant value for the collection of plants in firestore. */
private const val plantsCollection = "plants"

/** Repository that implements PlantsRepository but stores the data in Firestore. */
class PlantsRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : PlantsRepository {
  private val healthCalculator = PlantHealthCalculator()

  /** The list of plants owned by the user, in the repository of the user. */
  private fun userPlantsCollection() =
      firestore.collection(usersCollection).document(currentUserId()).collection(plantsCollection)

  /** The id of the current user. Throw IllegalStateException if the user is not authenticated. */
  private fun currentUserId(): String {
    return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
  }

  override fun getNewId(): String {
    // We create a new empty document and it automatically creates an id associated to it.
    return userPlantsCollection().document().id
  }

  override suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant {
    val ownedPlant = OwnedPlant(id, plant, lastWatered)

    val serializedOwnedPlant = fromOwnedPlantToSerializedOwnedPlant(ownedPlant)

    userPlantsCollection().document(id).set(serializedOwnedPlant).await()
    return ownedPlant
  }

  override suspend fun getAllOwnedPlants(): List<OwnedPlant> {
    val snapshot = userPlantsCollection().get().await()
    val serializedList = snapshot.toObjects(SerializedOwnedPlant::class.java)
    val ownedPlantList = serializedList.map(FirestoreMapper::fromSerializedOwnedPlantToOwnedPlant)

    return ownedPlantList.map { p -> updatePlantHealthStatus(p) }
  }

  override suspend fun getOwnedPlant(id: String): OwnedPlant {
    val document = userPlantsCollection().document(id).get().await()
    if (!document.exists()) throw IllegalArgumentException("OwnedPlant with id $id not found")

    val serializedOwnedPlant =
        document.toObject(SerializedOwnedPlant::class.java)
            ?: throw IllegalArgumentException("Failed to parse SerializedOwnedPlant with id $id")

    val ownedPlant = fromSerializedOwnedPlantToOwnedPlant(serializedOwnedPlant)

    return updatePlantHealthStatus(ownedPlant)
  }

  override suspend fun deleteFromGarden(id: String) {
    val document = userPlantsCollection().document(id).get().await()
    if (!document.exists()) throw IllegalArgumentException("OwnedPlant with id $id not found")
    userPlantsCollection().document(id).delete().await()
  }

  override suspend fun editOwnedPlant(id: String, newOwnedPlant: OwnedPlant) {
    if (id != newOwnedPlant.id)
        throw IllegalArgumentException(
            "The id : $id is not the same as the id of the owned plant : ${newOwnedPlant.id}")

    userPlantsCollection()
        .document(id)
        .set(fromOwnedPlantToSerializedOwnedPlant(newOwnedPlant), SetOptions.merge())
        .await()
  }

  override suspend fun waterPlant(id: String, wateringTime: Timestamp) {
    val docRef = userPlantsCollection().document(id)
    val document = docRef.get().await()
    if (!document.exists()) throw IllegalArgumentException("OwnedPlant with id $id not found")

    val serializedOwnedPlant: SerializedOwnedPlant =
        document.toObject(SerializedOwnedPlant::class.java)
            ?: throw IllegalArgumentException("Failed to parse SerializedOwnedPlant with id $id")

    val ownedPlant = fromSerializedOwnedPlantToOwnedPlant(serializedOwnedPlant)
    val newOwnedPlant =
        ownedPlant.copy(previousLastWatered = ownedPlant.lastWatered, lastWatered = wateringTime)
    docRef.set(fromOwnedPlantToSerializedOwnedPlant(newOwnedPlant), SetOptions.merge()).await()
  }

  /**
   * Updates the health status of a plant based on current watering cycle.
   *
   * @param ownedPlant The plant to update
   * @return A copy of the plant with updated health status
   */
  private fun updatePlantHealthStatus(ownedPlant: OwnedPlant): OwnedPlant {
    val calculatedStatus =
        healthCalculator.calculateHealthStatus(
            lastWatered = ownedPlant.lastWatered,
            wateringFrequency = ownedPlant.plant.wateringFrequency,
            previousLastWatered = ownedPlant.previousLastWatered)
    val updatedPlant = ownedPlant.plant.copy(healthStatus = calculatedStatus)
    return ownedPlant.copy(plant = updatedPlant)
  }
}
