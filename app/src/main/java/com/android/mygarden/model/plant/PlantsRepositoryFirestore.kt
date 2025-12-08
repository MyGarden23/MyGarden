package com.android.mygarden.model.plant

import android.net.Uri
import android.util.Log
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.android.mygarden.model.plant.FirestoreMapper.fromOwnedPlantToSerializedOwnedPlant
import com.android.mygarden.model.plant.FirestoreMapper.fromSerializedOwnedPlantToOwnedPlant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.sql.Timestamp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

/** Constant value for the collection of users in firestore. */
private const val usersCollection = "users"

/** Constant value for the collection of plants in firestore. */
private const val plantsCollection = "plants"

/** Constant value for the authentication error message. */
private const val AUTH_ERR_MSG = "User not authenticated"

/** Constant value for the plant achievement decrement step. */
private const val PLANT_ACHIEVEMENT_DECREMENT_STEP = 1

/** File extension for plant images stored in Cloud Storage. */
private const val IMAGE_FILE_EXTENSION = ".jpg"

/** Log tag for PlantsRepositoryFirestore. */
private const val LOG_TAG_PLANTS_REPO = "PlantsRepositoryFirestore"

/** Log tag for Cloud Storage operations. */
private const val LOG_TAG_CLOUD_STORAGE = "Cloud Storage"

/** Error message template for plant not found. */
private fun plantNotFoundErrMsg(id: String) = "OwnedPlant with id $id not found"

/** Error message template for parsing failure. */
private fun parsingFailedErrMsg(id: String) = "Failed to parse SerializedOwnedPlant with id $id"

/** Error message template for different IDs. */
private fun differentIdsErrMsg(id1: String, id2: String) =
    "The id : $id1 is not the same as the id of the owned plant : $id2"

/** Error message for failed local file deletion. */
private fun failedToDeleteFileMsg(path: String) = "Failed to delete the local file image: $path "

/** Error message for image not found in Cloud Storage. */
private fun imageNotFoundMsg(message: String?) = "Image not found or already deleted: $message"

/** Repository that implements PlantsRepository but stores the data in Firestore. */
class PlantsRepositoryFirestore(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val achievementsRepo: AchievementsRepository = AchievementsRepositoryProvider.repository
) : PlantsRepositoryBase() {
  private val healthCalculator = PlantHealthCalculator()

  override val tickDelay: Duration = 30.minutes

  // This flow emit something (a boolean here) each time a list update could make the list contain a
  // thirsty plant
  private val _plantsUpdate = MutableSharedFlow<Boolean>()

  private var scope = CoroutineScope(Job())

  /**
   * This flow collects the user's plant list from Firebase with updated health status either when
   * 1) the list of plants is updated and this could trigger a plant to be thirsty
   * 2) a tick is emitted then emit the updated list ; to be collected by the pop-up VM
   */
  private var _plantsFlow: StateFlow<List<OwnedPlant>> = createPlantsFlow()

  override val plantsFlow: StateFlow<List<OwnedPlant>>
    get() = _plantsFlow

  private fun createPlantsFlow(): StateFlow<List<OwnedPlant>> {
    return combine(_plantsUpdate, ticks) { _, _ ->
          // ensures that a user is authenticated to get all of his plants
          if (auth.currentUser != null) {
            getAllOwnedPlants()
          } else emptyList()
        }
        .distinctUntilChanged()
        .stateIn(
            scope, SharingStarted.WhileSubscribed(plantsFlowTimeoutWhenNoSubscribers), emptyList())
  }

  /** The list of plants owned by the user, in the repository of the user. */
  private fun userPlantsCollection() =
      firestore.collection(usersCollection).document(currentUserId()).collection(plantsCollection)

  /**
   * Gives the reference to the image associated to the id given in argument in the current user's
   * plants collection in Cloud Storage. All the images are stored in JPG format : plantId.jpg
   *
   * @param plantId the name of the plant associated to the image
   * @return the storage reference in Cloud Storage of the image
   */
  private fun storageRef(plantId: String): StorageReference {
    return storage.reference.child(
        "$usersCollection/${currentUserId()}/$plantsCollection/$plantId$IMAGE_FILE_EXTENSION")
  }

  /** The id of the current user. Throw IllegalStateException if the user is not authenticated. */
  private fun currentUserId(): String {
    return auth.currentUser?.uid ?: throw IllegalStateException(AUTH_ERR_MSG)
  }

  override fun getNewId(): String {
    // We create a new empty document and it automatically creates an id associated to it.
    return userPlantsCollection().document().id
  }

  override suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant {
    // Upload the image to Cloud Storage
    val imageUrl: String? = uploadLocalImageToCloudStorageAndDeleteImageLocally(plant.image, id)

    // Creates an OwnedPlant with the arguments and change the image field of the plant to store the
    // URL in Cloud Storage or null if there is no image.
    val healthySince =
        if (plant.healthStatus == PlantHealthStatus.HEALTHY ||
            plant.healthStatus == PlantHealthStatus.SLIGHTLY_DRY)
            Timestamp(System.currentTimeMillis())
        else null
    val ownedPlant =
        OwnedPlant(
            id = id,
            plant = plant.copy(image = imageUrl),
            lastWatered = lastWatered,
            healthySince = healthySince)
    val serializedOwnedPlant = fromOwnedPlantToSerializedOwnedPlant(ownedPlant)

    userPlantsCollection().document(id).set(serializedOwnedPlant).await()

    // Update the achievements
    achievementsRepo.updateAchievementValue(
        currentUserId(), AchievementType.PLANTS_NUMBER, plantsFlow.value.size)

    // Trigger the plantsFlow to collect the updated list from Firebase and ensure no plant are
    // thirsty
    _plantsUpdate.emit(true)
    return ownedPlant
  }

  override suspend fun getAllOwnedPlants(): List<OwnedPlant> {
    val snapshot = userPlantsCollection().get().await()
    val serializedList = snapshot.toObjects(SerializedOwnedPlant::class.java)
    val ownedPlantList = serializedList.map(FirestoreMapper::fromSerializedOwnedPlantToOwnedPlant)

    // Trigger the plantsFlow to collect the updated list from Firebase and ensure no plant are
    // thirsty
    _plantsUpdate.emit(true)
    return ownedPlantList.map { p -> updatePlantHealthStatus(p) }
  }

  override suspend fun getAllOwnedPlantsByUserId(userId: String): List<OwnedPlant> {
    val snapshot =
        firestore
            .collection(usersCollection)
            .document(userId)
            .collection(plantsCollection)
            .get()
            .await()
    val serializedList = snapshot.toObjects(SerializedOwnedPlant::class.java)
    val ownedPlantList = serializedList.map(FirestoreMapper::fromSerializedOwnedPlantToOwnedPlant)
    return ownedPlantList.map { p -> updatePlantHealthStatus(p) }
  }

  override suspend fun getOwnedPlant(id: String): OwnedPlant {
    val document = userPlantsCollection().document(id).get().await()
    require(document.exists()) { plantNotFoundErrMsg(id) }

    val serializedOwnedPlant =
        document.toObject(SerializedOwnedPlant::class.java)
            ?: throw IllegalArgumentException(parsingFailedErrMsg(id))

    val ownedPlant = fromSerializedOwnedPlantToOwnedPlant(serializedOwnedPlant)

    // Trigger the plantsFlow to collect the updated list from Firebase and ensure no plant are
    // thirsty
    _plantsUpdate.emit(true)
    return updatePlantHealthStatus(ownedPlant)
  }

  override suspend fun deleteFromGarden(id: String) {
    // Delete the data in Firestore
    val document = userPlantsCollection().document(id).get().await()
    require(document.exists()) { plantNotFoundErrMsg(id) }

    userPlantsCollection().document(id).delete().await()
    // Delete the image in Cloud Storage
    deleteImageInCloudStorage(id)

    // Update the achievements
    achievementsRepo.updateAchievementValue(
        currentUserId(),
        AchievementType.PLANTS_NUMBER,
        plantsFlow.value.size - PLANT_ACHIEVEMENT_DECREMENT_STEP)
  }

  override suspend fun editOwnedPlant(id: String, newOwnedPlant: OwnedPlant) {
    require(id == newOwnedPlant.id) { differentIdsErrMsg(id, newOwnedPlant.id) }

    userPlantsCollection()
        .document(id)
        .set(fromOwnedPlantToSerializedOwnedPlant(newOwnedPlant), SetOptions.merge())
        .await()
    // Trigger the plantsFlow to collect the updated list from Firebase and ensure no plant are
    // thirsty
    _plantsUpdate.emit(true)
  }

  override suspend fun waterPlant(id: String, wateringTime: Timestamp) {
    val docRef = userPlantsCollection().document(id)
    val document = docRef.get().await()
    require(document.exists()) { plantNotFoundErrMsg(id) }

    val serializedOwnedPlant: SerializedOwnedPlant =
        document.toObject(SerializedOwnedPlant::class.java)
            ?: throw IllegalArgumentException(parsingFailedErrMsg(id))

    val ownedPlant = fromSerializedOwnedPlantToOwnedPlant(serializedOwnedPlant)
    val newOwnedPlant =
        ownedPlant.copy(previousLastWatered = ownedPlant.lastWatered, lastWatered = wateringTime)
    docRef.set(fromOwnedPlantToSerializedOwnedPlant(newOwnedPlant), SetOptions.merge()).await()
  }

  /**
   * Uploads the image of the plant (stored locally) to Cloud Storage for Firestore and delete the
   * image stored locally. return the URL of where this image is stored or null if there was no
   * image to store.
   *
   * @param imagePath the path of the image stored locally or null if there is no image to store
   * @param plantId the id of the plant associated to the image to know where to store the image in
   *   Cloud Storage
   * @return the URL of where the image is stored in Cloud Storage or null if there was no image to
   *   store.
   */
  private suspend fun uploadLocalImageToCloudStorageAndDeleteImageLocally(
      imagePath: String?,
      plantId: String
  ): String? {
    if (imagePath == null) return null
    val imageFile = File(imagePath)
    // Create a reference in Cloud Storage
    val storageReference = storageRef(plantId)
    val fileUri = Uri.fromFile(imageFile)

    // Upload the File
    storageReference.putFile(fileUri).await()

    // Delete the image locally
    if (imageFile.exists()) {
      val deleted = imageFile.delete()
      if (!deleted) {
        Log.e(LOG_TAG_PLANTS_REPO, failedToDeleteFileMsg(imageFile.path))
      }
    }

    // Get the URL
    return storageReference.downloadUrl.await().toString()
  }

  /**
   * Delete an image referred by the id of the plant it is associated to from Cloud Storage
   *
   * @param plantId the name of the image in Cloud Storage
   */
  private suspend fun deleteImageInCloudStorage(plantId: String) {
    val storageReference = storageRef(plantId)
    try {
      storageReference.delete().await()
    } catch (e: Exception) {
      Log.e(LOG_TAG_CLOUD_STORAGE, imageNotFoundMsg(e.message))
    }
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

    // Handle the transition to and from HEALTHY/SLIGHTLY_DRY for the healthy streak achievement
    val isNowHealthy =
        calculatedStatus == PlantHealthStatus.HEALTHY ||
            calculatedStatus == PlantHealthStatus.SLIGHTLY_DRY
    val wasHealthy =
        ownedPlant.plant.healthStatus == PlantHealthStatus.HEALTHY ||
            ownedPlant.plant.healthStatus == PlantHealthStatus.SLIGHTLY_DRY

    // Set the healthySince if the plant goes from HEALTHY/SLIGHTLY_DRY to another status
    return if (!wasHealthy && isNowHealthy) {
      ownedPlant.copy(plant = updatedPlant, healthySince = Timestamp(System.currentTimeMillis()))
    } else if (wasHealthy && !isNowHealthy) {
      ownedPlant.copy(plant = updatedPlant, healthySince = null)
    } else {
      ownedPlant.copy(plant = updatedPlant)
    }
  }

  /**
   * Cleans up resources before logout. Cancels the coroutine scope to stop all flows and prevent
   * PERMISSION_DENIED errors.
   */
  override fun cleanup() {
    scope.cancel()
    scope = CoroutineScope(Job())
    _plantsFlow = createPlantsFlow()
  }
}
