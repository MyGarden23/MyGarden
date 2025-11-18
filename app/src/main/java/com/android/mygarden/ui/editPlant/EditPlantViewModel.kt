package com.android.mygarden.ui.editPlant

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.R
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the Edit Plant screen.
 *
 * This state contains all the editable data for a plant, such as its name, latin name, description,
 * image, last watered date, and error messages. It also keeps track of whether the plant was
 * recognized by the AI or not.
 *
 * @property name The common name of the plant (e.g., "Rose", "Tomato").
 * @property latinName The scientific/botanical name of the plant (e.g., "Rosa rubiginosa").
 * @property description A detailed text description of the plant, including care instructions.
 * @property errorMsg The resource ID of an error message to display (nullable).
 * @property lastWatered The timestamp of the last watering date.
 * @property image The visual representation of the plant, in String? because it is either a path in
 *     * local or an URL to find the actual image.
 *
 * @property location The location of the plant in the house (inside/outside).
 * @property isRecognized Whether the plant was recognized by the AI or not.
 */
data class EditPlantUIState(
    val name: String = "",
    val latinName: String = "",
    val description: String = "",
    val errorMsg: Int? = null,
    val lastWatered: Timestamp? = null,
    val image: String? = null,
    val isRecognized: Boolean = false,
    val location: PlantLocation = PlantLocation.UNKNOWN
)

/**
 * ViewModel for the Edit Plant screen.
 *
 * Handles all logic related to loading, editing, and deleting a plant. This class implements
 * [EditPlantViewModelInterface].
 *
 * @param repository The [PlantsRepository] used for plant's data access.
 */
class EditPlantViewModel(
    private val repository: PlantsRepository = PlantsRepositoryProvider.repository,
) : ViewModel(), EditPlantViewModelInterface {
  private val _uiState = MutableStateFlow(EditPlantUIState())
  override val uiState: StateFlow<EditPlantUIState> = _uiState.asStateFlow()

  private var newOwnedPlant: OwnedPlant? = null

  override fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  override fun setErrorMsg(resId: Int) {
    _uiState.value = _uiState.value.copy(errorMsg = resId)
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
                image = plant.image,
                isRecognized = plant.isRecognized,
                location = plant.location)
      } catch (e: Exception) {
        Log.e("EditPlantViewModel", "Error loading Plant by ID. $ownedPlantId", e)
        setErrorMsg(R.string.error_failed_load_plant_edit)
      }
    }
  }

  override fun setLastWatered(timestamp: Timestamp) {
    _uiState.value = _uiState.value.copy(lastWatered = timestamp)
  }

  override fun setName(newName: String) {
    _uiState.value = _uiState.value.copy(name = newName)
  }

  override fun setLatinName(newLatinName: String) {
    _uiState.value = _uiState.value.copy(latinName = newLatinName)
  }

  override fun setDescription(newDescription: String) {
    _uiState.value = _uiState.value.copy(description = newDescription)
  }

  override fun setLocation(newLocation: PlantLocation) {
    _uiState.value = _uiState.value.copy(location = newLocation)
  }

  override fun deletePlant(ownedPlantId: String) {
    viewModelScope.launch {
      try {
        repository.deleteFromGarden(ownedPlantId)
      } catch (e: Exception) {
        Log.e("EditPlantViewModel", "Failed to delete plant.", e)
        setErrorMsg(R.string.error_failed_delete_plant_edit)
      }
    }
  }

  override fun editPlant(ownedPlantId: String) {
    val state = _uiState.value

    val newPlant = newOwnedPlant
    val watered = state.lastWatered
    val description = state.description
    val name = state.name
    val latinName = state.latinName
    val location = state.location

    if (newPlant == null) {
      Log.e("EditPlantViewModel", "Failed to edit plant (Plant not loaded).")
      setErrorMsg(R.string.error_plant_not_loaded)
      return
    }

    // Cannot have a blank description
    if (description.isBlank()) {
      Log.e("EditPlantViewModel", "Failed to edit plant. (no description selected).")
      setErrorMsg(R.string.error_description_blank)
      return
    }

    // Cannot have a blank name
    if (name.isBlank()) {
      Log.e("EditPlantViewModel", "Failed to edit plant. (no name selected).")
      setErrorMsg(R.string.error_name_blank)
      return
    }

    // Cannot have a blank latin name
    if (latinName.isBlank()) {
      Log.e("EditPlantViewModel", "Failed to edit plant. (no latin name selected).")
      setErrorMsg(R.string.error_latin_name_blank)
      return
    }

    // Not suppose to occur with the workflow of the app.
    if (watered == null) {
      Log.e("EditPlantViewModel", "Failed to edit plant. (no last time watered selected).")
      setErrorMsg(R.string.error_last_watered_missing)
      return
    }

    val updated =
        newPlant.copy(
            lastWatered = watered,
            plant =
                newPlant.plant.copy(
                    description = description,
                    name = name,
                    latinName = latinName,
                    location = location))

    viewModelScope.launch {
      try {
        repository.editOwnedPlant(ownedPlantId, updated)
      } catch (e: Exception) {
        Log.e("EditPlantViewModel", "Failed to save changes", e)
        _uiState.value = _uiState.value.copy(errorMsg = R.string.error_failed_save_changes_edit)
      }
    }
  }
}
