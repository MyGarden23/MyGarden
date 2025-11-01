package com.android.mygarden.ui.garden

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the garden
 *
 * @property plants the list of plants owned by the current user : empty by default
 * @property errorMsg a potential error message to be displayed if one of the actions of the view
 *   model with the repository doesn't work
 */
data class GardenUIState(val plants: List<OwnedPlant> = emptyList(), val errorMsg: String? = null)

/**
 * The view model of the garden that handles all UI interactions
 *
 * @property plantsRepo the repository of the plants to store them
 */
class GardenViewModel(
    private val plantsRepo: PlantsRepository = PlantsRepositoryProvider.repository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(GardenUIState())
  val uiState: StateFlow<GardenUIState> = _uiState.asStateFlow()

  init {
    refreshUIState()
  }

  /** Refresh the UI state by fetching all the owned plants */
  fun refreshUIState() {
    getAllPlants()
  }

  /** Clears the error message of the UI state */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets the error message of the UI state
   *
   * @param msg the error message
   */
  fun setErrorMsg(msg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = msg)
  }

  /** Fetches all the plants from the [plantsRepo] and set an error message if the fetch failed */
  private fun getAllPlants() {
    viewModelScope.launch {
      try {
        val plants = plantsRepo.getAllOwnedPlants()
        _uiState.value = GardenUIState(plants = plants)
      } catch (e: Exception) {
        Log.e("GardenViewModel", "Owned plants couldn't be retrieved from repository", e)
        setErrorMsg("getAllPlants failed : Owned plants couldn't be retrieved from repository")
      }
    }
  }

  /**
   * Wrapper for the repository homologous function that update the given plant last time watered to
   * the current time of the device.
   *
   * @param ownedPlant the plant that is being watered
   */
  fun waterPlant(ownedPlant: OwnedPlant) {
    viewModelScope.launch {
      plantsRepo.waterPlant(ownedPlant.id, Timestamp(System.currentTimeMillis()))
      // Refresh UI state
      // TODO: update when the health status update is implemented
      getAllPlants()
    }
  }
}
