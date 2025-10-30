package com.android.mygarden.ui.popup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class PopupViewModel(
    private val plantsRepo: PlantsRepository = PlantsRepositoryProvider.repository
) : ViewModel() {

  private val _thirstyPlants = MutableSharedFlow<OwnedPlant>(replay = 0, extraBufferCapacity = 64)
  val thirstyPlants: SharedFlow<OwnedPlant> = _thirstyPlants

  var previousCollectedList: List<OwnedPlant> = emptyList()

  init {
    viewModelScope.launch {
      // collect the updated list of owned plants that the Flow of the repo emitted
      plantsRepo.plantsFlow.collect { list ->
        val previousIds = previousCollectedList.associateBy { it.id }
        list
            // filter the list to keep only the plants that were not in status [NEEDS_WATER] but are
            // now
            .filter { ownedPlant ->
              val previousOwnedPlant = previousIds[ownedPlant.id]
              // current plant needs water now AND didn't before (or simply didn't exist before)
              (ownedPlant.plant.healthStatus == PlantHealthStatus.NEEDS_WATER) &&
                  previousOwnedPlant?.plant?.healthStatus != PlantHealthStatus.NEEDS_WATER
            }
            // for each of these plants, emit it through the SharedFlow that will be collected by
            // the UI to display a pop-up
            .forEach { becameThirsty -> _thirstyPlants.tryEmit(becameThirsty) }

        // update the previous collected list to the one just received for future collections of the
        // Flow
        previousCollectedList = list.toList()
      }
    }
  }
}
