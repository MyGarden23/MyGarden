package com.android.sample.model.plant

/**
 * Provides a singleton instance of the PlantsRepository.
 *

 */
object PlantsRepositoryProvider {
    private val _plantsRepository: PlantsRepository = PlantsRepositoryLocal()
    var repository: PlantsRepository = _plantsRepository
}