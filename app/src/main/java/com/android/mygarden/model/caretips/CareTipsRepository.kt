package com.android.mygarden.model.caretips

import com.android.mygarden.model.plant.PlantHealthStatus

/** Repository interface to manage the cached care tips from Gemini */
interface CareTipsRepository {

  /**
   * Gets a tip from the repository ; returns null if it wasn't cached already
   *
   * @param latinName the latin name of the plant for which the care tip was called
   * @param healthStatus the status of the plant for which the care tip was called
   */
  suspend fun getTip(latinName: String, healthStatus: PlantHealthStatus): String?

  /**
   * Adds a tip to the repository, i.e. cache the tip for future requests
   *
   * @param latinName the latin name of the plant for which the care tip was called
   * @param healthStatus the status of the plant for which the care tip was called
   * @param tip the queried tip from the Gemini API to cache on Firestore
   */
  suspend fun addTip(latinName: String, healthStatus: PlantHealthStatus, tip: String)
}
