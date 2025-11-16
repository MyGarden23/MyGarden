package com.android.mygarden.ui.plantinfos

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.R
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Enum representing which tab is currently selected in the plant info screen. */
enum class SelectedPlantInfoTab(val textRes: Int) {
  DESCRIPTION(R.string.description),
  HEALTH_STATUS(R.string.tab_health),
  LOCATION(R.string.location)
}

/** UI state for the plant info screen. Contains all the plant information to be displayed. */
data class PlantInfoUIState(
    val name: String = "",
    val image: String? = null,
    val latinName: String = "",
    val description: String = "",
    val location: PlantLocation = PlantLocation.UNKNOWN,
    val lightExposure: String = "",
    val healthStatus: PlantHealthStatus = PlantHealthStatus.UNKNOWN,
    val healthStatusDescription: String = "",
    val wateringFrequency: Int = 0,
    val selectedTab: SelectedPlantInfoTab = SelectedPlantInfoTab.DESCRIPTION,
    val isRecognized: Boolean = false,
    val isSaving: Boolean = false,
    val showCareTipsDialog: Boolean = false,
    val careTips: String = ""
) {
  fun savePlant(): Plant {
    return Plant(
        name = name,
        image = image,
        latinName = latinName,
        description = description,
        location = location,
        lightExposure = lightExposure,
        healthStatus = healthStatus,
        healthStatusDescription = healthStatusDescription,
        wateringFrequency = wateringFrequency,
        isRecognized = isRecognized)
  }
}

/** ViewModel for managing the plant information screen state. */
class PlantInfoViewModel(
    private val plantsRepository: PlantsRepository = PlantsRepositoryProvider.repository
) : ViewModel() {
  // Private mutable state flow
  private val _uiState = MutableStateFlow(PlantInfoUIState())

  // Public immutable state flow exposed to UI
  val uiState: StateFlow<PlantInfoUIState> = _uiState.asStateFlow()

  /** Initialize the UI state with plant data. Called when the screen is first displayed. */
  fun initializeUIState(plant: Plant, loadingText: String) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(description = loadingText, image = plant.image)
      // firewall to not regenerate is no picture taken
      val generatedPlant =
          if (!plant.image.isNullOrEmpty()) {
            plantsRepository.identifyPlant(plant.image)
          } else {
            plant // Wee keep the plant as it is
          }
      _uiState.value =
          PlantInfoUIState(
              generatedPlant.name,
              plant.image,
              generatedPlant.latinName,
              generatedPlant.description,
              generatedPlant.location,
              generatedPlant.lightExposure,
              generatedPlant.healthStatus,
              generatedPlant.healthStatusDescription,
              generatedPlant.wateringFrequency,
              isRecognized = generatedPlant.isRecognized,
          )
    }
  }

  fun resetUIState() {
    _uiState.value = PlantInfoUIState()
  }

  /**
   * Save the plant to the user's garden.
   *
   * @param plant The plant to save
   * @param onPlantSaved Callback called with the ID of the saved plant
   */
  fun savePlant(plant: Plant, onPlantSaved: (String) -> Unit) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSaving = true)
      val newId = plantsRepository.getNewId()
      plantsRepository.saveToGarden(plant, newId, Timestamp(System.currentTimeMillis()))
      _uiState.value = _uiState.value.copy(isSaving = false)
      onPlantSaved(newId)
    }
  }

  /** Update the selected tab (Description or Health). */
  fun setTab(tab: SelectedPlantInfoTab) {
    _uiState.value = _uiState.value.copy(selectedTab = tab)
  }

  companion object {
    // Utils for loading tips text to avoid hardcoding
    const val LOADING_TIPS_PLACEHOLDER = "__LOADING_TIPS__"

    // Placeholder value used to signal the UI that the plant is unknown
    const val UNKNOWN_PLANT_TIPS_PLACEHOLDER = "__UNKNOWN_PLANT__"
  }

  /**
   * Request care tips for a plant and show the dialog.
   *
   * @param latinName Scientific (Latin) name of the plant used to tailor the tips.
   * @param healthStatus Current health status used to adapt the advice.
   */
  fun showCareTips(latinName: String, healthStatus: PlantHealthStatus) {
    viewModelScope.launch {
      _uiState.value =
          _uiState.value.copy(showCareTipsDialog = true, careTips = LOADING_TIPS_PLACEHOLDER)

      // If the plant's latin name is missing or equals "unknown", don't attempt to
      // generate tips — show a fallback message instead.
      if (latinName.isBlank() || latinName.equals("unknown", true)) {
        _uiState.value = _uiState.value.copy(careTips = UNKNOWN_PLANT_TIPS_PLACEHOLDER)
        return@launch
      }

      val tips =
          try {
            plantsRepository.generateCareTips(latinName, healthStatus)
          } catch (e: Exception) {
            Log.e("plantInfoViewModel", "Error generating care tips for $latinName", e)
            "Impossible to generate care tips"
          }

      _uiState.value = _uiState.value.copy(careTips = tips)
    }
  }

  /** Dismiss the care tips dialog by updating the UI state. */
  fun dismissCareTips() {
    _uiState.value = _uiState.value.copy(showCareTipsDialog = false)
  }
}
