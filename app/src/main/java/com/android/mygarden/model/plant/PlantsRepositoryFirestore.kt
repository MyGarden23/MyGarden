package com.android.mygarden.model.plant

import com.android.mygarden.model.plant.FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant
import com.android.mygarden.model.plant.FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.sql.Timestamp
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await

/** Constant value for the collection of users in firestore. */
private const val usersCollection = "users"

/** Constant value for the collection of plants in firestore. */
private const val plantsCollection = "plants"

private const val AUTH_ERR_MSG = "User not authenticated"

private fun plantNotFoundErrMsg(id: String) = "OwnedPlant with id $id not found"

private fun parsingFailedErrMsg(id: String) = "Failed to parse SerializedOwnedPlant with id $id"

private fun differentIdsErrMsg(id1: String, id2: String) =
    "The id : $id1 is not the same as the id of the owned plant : $id2"

/** Repository that implements PlantsRepository but stores the data in Firestore. */
class PlantsRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : PlantsRepository {
  private val healthCalculator = PlantHealthCalculator()

  // This flow emit something (a boolean here) each time a list update could make the list contain a
  // thirsty plant
  private val _plantsUpdate = MutableSharedFlow<Boolean>()

  val scope = CoroutineScope(Job())

  // This flow emit something (a boolean here) at a fixed period of time ; to be collected by the
  // single flow below
  private val ticks: Flow<Boolean> = flow {
    while (currentCoroutineContext().isActive) {
      emit(true)
      delay(30.minutes)
    }
  }

  /**
   * This flow collects the user's plant list from Firebase with updated health status either when
   * 1) the list of plants is updated and this could trigger a plant to be thirsty
   * 2) a tick is emitted then emit the updated list ; to be collected by the pop-up VM
   */
  override val plantsFlow: StateFlow<List<OwnedPlant>> =
      combine(_plantsUpdate, ticks) { update, time ->
            // ensures that a user is authenticated to get all of his plants
            if (auth.currentUser != null) {
              getAllOwnedPlants()
            } else emptyList()
          }
          .distinctUntilChanged()
          .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

  /** The list of plants owned by the user, in the repository of the user. */
  private fun userPlantsCollection() =
      firestore.collection(usersCollection).document(currentUserId()).collection(plantsCollection)

  /** The id of the current user. Throw IllegalStateException if the user is not authenticated. */
  private fun currentUserId(): String {
    return auth.currentUser?.uid ?: throw IllegalStateException(AUTH_ERR_MSG)
  }

  override fun getNewId(): String {
    // We create a new empty document and it automatically creates an id associated to it.
    return userPlantsCollection().document().id
  }

  override suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant {
    val ownedPlant = OwnedPlant(id, plant, lastWatered)

    val serializedOwnedPlant = fromOwnedPlantToSerializedOwnedPlant(ownedPlant)

    userPlantsCollection().document(id).set(serializedOwnedPlant).await()
    // trigger the plantsFlow to collect the updated list from Firebase and ensure no plant are
    // thirsty
    _plantsUpdate.emit(true)
    return ownedPlant
  }

  override suspend fun getAllOwnedPlants(): List<OwnedPlant> {
    val snapshot = userPlantsCollection().get().await()
    val serializedList = snapshot.toObjects(SerializedOwnedPlant::class.java)
    val ownedPlantList = serializedList.map(FirestoreMapper::fromSerializedOwnedPlantToOwnedPlant)

    // trigger the plantsFlow to collect the updated list from Firebase and ensure no plant are
    // thirsty
    _plantsUpdate.emit(true)
    return ownedPlantList.map { p -> updatePlantHealthStatus(p) }
  }

  override suspend fun getOwnedPlant(id: String): OwnedPlant {
    val document = userPlantsCollection().document(id).get().await()
    if (!document.exists()) throw IllegalArgumentException(plantNotFoundErrMsg(id))

    val serializedOwnedPlant =
        document.toObject(SerializedOwnedPlant::class.java)
            ?: throw IllegalArgumentException(parsingFailedErrMsg(id))

    val ownedPlant = fromSerializedOwnedPlantToOwnedPlant(serializedOwnedPlant)

    // trigger the plantsFlow to collect the updated list from Firebase and ensure no plant are
    // thirsty
    _plantsUpdate.emit(true)
    return updatePlantHealthStatus(ownedPlant)
  }

  override suspend fun deleteFromGarden(id: String) {
    val document = userPlantsCollection().document(id).get().await()
    if (!document.exists()) throw IllegalArgumentException(plantNotFoundErrMsg(id))
    userPlantsCollection().document(id).delete().await()
  }

  override suspend fun editOwnedPlant(id: String, newOwnedPlant: OwnedPlant) {
    if (id != newOwnedPlant.id)
        throw IllegalArgumentException(differentIdsErrMsg(id, newOwnedPlant.id))

    userPlantsCollection()
        .document(id)
        .set(fromOwnedPlantToSerializedOwnedPlant(newOwnedPlant), SetOptions.merge())
        .await()
    // trigger the plantsFlow to collect the updated list from Firebase and ensure no plant are
    // thirsty
    _plantsUpdate.emit(true)
  }

  override suspend fun waterPlant(id: String, wateringTime: Timestamp) {
    val docRef = userPlantsCollection().document(id)
    val document = docRef.get().await()
    if (!document.exists()) throw IllegalArgumentException(plantNotFoundErrMsg(id))

    val serializedOwnedPlant: SerializedOwnedPlant =
        document.toObject(SerializedOwnedPlant::class.java)
            ?: throw IllegalArgumentException(parsingFailedErrMsg(id))

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
