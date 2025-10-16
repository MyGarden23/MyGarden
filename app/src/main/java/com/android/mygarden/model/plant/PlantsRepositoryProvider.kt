package com.android.mygarden.model.plant

/** Provides a singleton instance of the PlantsRepository. */
object PlantsRepositoryProvider {
  private val _plantsRepository: PlantsRepository_ = PlantsRepositoryLocal()
  var repository: PlantsRepository_ = _plantsRepository
}
