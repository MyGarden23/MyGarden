package com.android.mygarden.ui.editPlant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantsRepository_
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditPlantUIState(
    val name: String = "",
    val latinName: String = "",
    val description: String = "",
    val errorMsg: String? = null,
    val lastWatered: Timestamp? = null,
    val image: String? = null
)

class EditPlantViewModel(
    private val repository: PlantsRepository_ = PlantsRepositoryProvider.repository,
) : ViewModel(), EditPlantViewModelInterface_ {
  private val _uiState = MutableStateFlow(EditPlantUIState())
  override val uiState: StateFlow<EditPlantUIState> = _uiState.asStateFlow()

  private var newOwnedPlant: OwnedPlant? = null

  override fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  override fun setErrorMsg(e: String) {
    _uiState.value = _uiState.value.copy(errorMsg = e)
  }

  override fun loadPlant(ownedPlantId: String) {
    viewModelScope.launch {
      try {
        val owned = repository.getOwnedPlant(ownedPlantId)
        newOwnedPlant = owned
        val plant = owned.plant
        _uiState.value =
            EditPlantUIState(
                name = plant.name,
                latinName = plant.latinName,
                description = plant.description,
                lastWatered = owned.lastWatered,
                image = plant.image)
      } catch (e: Exception) {
        Log.e("EditPlantViewModel", "Error loading Plant by ID. $ownedPlantId", e)
        setErrorMsg("Failed to load plant")
      }
    }
  }

  override fun setLastWatered(timestamp: Timestamp) {
    _uiState.value = _uiState.value.copy(lastWatered = timestamp)
  }

  override fun setDescription(newDescription: String) {
    _uiState.value = _uiState.value.copy(description = newDescription)
  }

  override fun deletePlant(ownedPlantId: String) {
    viewModelScope.launch {
      try {

        repository.deleteFromGarden(ownedPlantId)
      } catch (e: Exception) {
        Log.e("EditPlantViewModel", "Failed to delete plant.", e)
        setErrorMsg("Failed to delete plant")
      }
    }
  }

  override fun editPlant(ownedPlantId: String) {
    val newPlant = newOwnedPlant
    val watered = _uiState.value.lastWatered
    val description = _uiState.value.description

    if (newPlant == null) {
      Log.e("EditPlantViewModel", "Failed to edit plant (Plant not loaded).")
      setErrorMsg("Plant not loaded yet")
      return
    }

    if (description.isBlank()) {
      Log.e("EditPlantViewModel", "Failed to edit plant. (no description selected).")
      setErrorMsg("Please put a description")
      return
    }

    // Not suppose to occur with the workflow of the app.
    if (watered == null) {
      Log.e("EditPlantViewModel", "Failed to edit plant. (no last time watered selected).")
      setErrorMsg("Please select the last time watered")
      return
    }

    val updated =
        newPlant.copy(
            lastWatered = watered,
            plant = newPlant.plant.copy(description = _uiState.value.description))

    viewModelScope.launch {
      try {
        repository.editOwnedPlant(ownedPlantId, updated)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMsg = e.message ?: "Failed to save changes")
      }
    }
  }
}
