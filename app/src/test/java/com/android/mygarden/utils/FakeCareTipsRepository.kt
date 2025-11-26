package com.android.mygarden.utils

import com.android.mygarden.model.caretips.CareTipsRepository
import com.android.mygarden.model.plant.PlantHealthStatus

/** data class Tips used to easily implement the functions when needed */
data class Tips(val latinName: String, val healthStatus: PlantHealthStatus, val tip: String?)
/**
 * Fake implementation of the CareTips repository for testing
 *
 * @param working to say whether the functions actually do something or not (ensuring correct
 *   behaviour of previous implementation)
 */
class FakeCareTipsRepository(val working: Boolean = false) : CareTipsRepository {

  private val tipList = mutableListOf<Tips>()

  override suspend fun getTip(latinName: String, healthStatus: PlantHealthStatus): String? {
    return if (working) {
      val tip = tipList.firstOrNull { it.latinName == latinName && it.healthStatus == healthStatus }
      tip?.tip
    } else null
  }

  override suspend fun addTip(latinName: String, healthStatus: PlantHealthStatus, tip: String) {
    val newTip = Tips(latinName, healthStatus, tip)
    tipList.add(newTip)
  }
}
