package com.android.mygarden.ui.editPlant

import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.sql.Timestamp


data class EditPlantUIState(
    val name: String = "",
    val latinName: String = "",
    val description: String = "",
    val errorMsg: String? = null,
    val lastWatered: Timestamp? = null,
    val image: ImageBitmap? = null
)


class EditPlantViewModel(
    private val repository: PlantsRepository = PlantsRepositoryProvider.repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditPlantUIState())
    val uiState: StateFlow<EditPlantUIState> = _uiState.asStateFlow()

    private var newOwnedPlant: OwnedPlant? = null


    fun clearErrorMsg() {
        _uiState.value = _uiState.value.copy(errorMsg = null)
    }

    fun setErrorMsg(e: String) {
        _uiState.value = _uiState.value.copy(errorMsg = e)
    }

    fun loadPlant(ownedPlantId: String) {
        viewModelScope.launch {
            try {
                val owned = repository.getOwnedPlant(ownedPlantId)
                newOwnedPlant = owned
                val plant = owned.plant
                _uiState.value = EditPlantUIState(
                    name = plant.name,
                    latinName = plant.latinName,
                    description = plant.description,
                    lastWatered = owned.lastWatered,
                    image = plant.image
                )
            } catch (e: Exception) {
                Log.e("EditPlantViewModel", "Error loading Plant by ID. $ownedPlantId", e)
                setErrorMsg("Failed to load plant")
            }
        }
    }

    fun setLastWatered(timestamp: Timestamp) {
        _uiState.value = _uiState.value.copy(lastWatered = timestamp)
    }

    fun setDescription(newDescription: String) {
        _uiState.value = _uiState.value.copy(description = newDescription)
    }

    fun deletePlant(ownedPlantId: String) {
        viewModelScope.launch {
            try {

                repository.deleteFromGarden(ownedPlantId)

            } catch (e: Exception) {
                Log.e("EditPlantViewModel", "Failed to delete plant.", e)
                setErrorMsg("Failed to delete plant")
            }
        }
    }

    fun editPlant(ownedPlantId: String) {
        val newPlant = newOwnedPlant
        val waterd = _uiState.value.lastWatered

        if (newPlant == null) {
            Log.e("EditPlantViewModel", "Failed to edit plant (Plant not loaded).")
            setErrorMsg("Plant not loaded yet")
            return
        }

        if (waterd == null) {
            Log.e("EditPlantViewModel", "Failed to edit plant. (no last time watered selected).")
            setErrorMsg("Please select the last time watered")
            return
        }

        val updated = newPlant.copy(
            lastWatered = waterd,
            plant = newPlant.plant.copy(
                description = _uiState.value.description
            )
        )

        viewModelScope.launch {
            try {
                repository.editOwnedPlant(ownedPlantId, updated)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMsg = e.message ?: "Failed to save changes")
            }
        }
    }

}
