package com.android.mygarden.model.caretips

import android.util.Log
import com.android.mygarden.model.plant.PlantHealthStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Online (Firestore) repository that handles the caching of plant care tips */
class CareTipsRepositoryFirestore(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : CareTipsRepository {

  companion object {
    private const val COLLECTION_TIPS = "tips"
    private const val COLLECTION_HEALTH_STATUS = "health_status"
    private const val TIP_FIELD = "tip"
  }

  /**
   * Returns the Firestore reference of the path for the tip with specific parameters
   *
   * @param latinName the latin name of the plant for which the tip was called
   * @param healthStatus the status of the plant for which the tip was called
   */
  private fun tipRef(latinName: String, healthStatus: PlantHealthStatus) =
      db.collection(COLLECTION_TIPS)
          .document(latinName)
          .collection(COLLECTION_HEALTH_STATUS)
          .document(healthStatus.name)

  override suspend fun getTip(latinName: String, healthStatus: PlantHealthStatus): String? {
    return try {
      val snapshot = tipRef(latinName, healthStatus).get().await()
      if (snapshot.exists()) {
        snapshot.getString(TIP_FIELD)
      } else null
    } catch (_: Exception) {
      Log.e("CareTipsRepositoryFirestore", "Failed to retrieve the tip from Firestore")
      null
    }
  }

  override suspend fun addTip(latinName: String, healthStatus: PlantHealthStatus, tip: String) {
    try {
      tipRef(latinName, healthStatus).set(mapOf(TIP_FIELD to tip)).await()
    } catch (_: Exception) {
      Log.e("CareTipsRepositoryFirestore", "Failed to add the tip to Firestore")
    }
  }
}
