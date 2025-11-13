package com.android.mygarden.ui.plantinfos

import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantsRepository
import java.sql.Timestamp
import kotlinx.coroutines.CompletableDeferred

/**
 * "Gate" repository: blocks saveToGarden until the test calls gate.complete(Unit). Delegates all
 * other behavior to the underlying repository.
 */
class GatedPlantsRepository(private val delegate: PlantsRepository) : PlantsRepository by delegate {

  val gate = CompletableDeferred<Unit>()

  override suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant {
    gate.await()
    return delegate.saveToGarden(plant, id, lastWatered)
  }
}
