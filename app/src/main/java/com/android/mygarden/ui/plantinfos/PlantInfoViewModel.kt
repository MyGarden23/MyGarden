package com.android.mygarden.ui.plantinfos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Enum representing which tab is currently selected in the plant info screen. */
enum class SelectedPlantInfoTab(val text: String) {
  DESCRIPTION("Description"),
  HEALTH_STATUS("Health"),
}

/** UI state for the plant info screen. Contains all the plant information to be displayed. */
data class PlantInfoUIState(
    val name: String = "",
    val image: String? = null,
    val latinName: String = "",
    val description: String = "",
    val healthStatus: PlantHealthStatus = PlantHealthStatus.UNKNOWN,
    val healthStatusDescription: String = "",
    val wateringFrequency: Int = 0,
    val selectedTab: SelectedPlantInfoTab = SelectedPlantInfoTab.DESCRIPTION,
) {
  fun savePlant(): Plant {
    return Plant(
        name = name,
        image = image,
        latinName = latinName,
        description = description,
        healthStatus = healthStatus,
        healthStatusDescription = healthStatusDescription,
        wateringFrequency = wateringFrequency)
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
  fun initializeUIState(plant: Plant) {
    viewModelScope.launch {
      _uiState.value =
          _uiState.value.copy(description = "Loading Plant Infos...", image = plant.image)
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
              generatedPlant.healthStatus,
              generatedPlant.healthStatusDescription,
              generatedPlant.wateringFrequency,
          )
    }
  }

  fun resetUIState() {
    _uiState.value = PlantInfoUIState()
  }

  /**
   * Save the plant to the user's garden.
   *
   * TODO: Update to allow user to specify last watered time.
   */
  fun savePlant(plant: Plant) {
    viewModelScope.launch {
      plantsRepository.saveToGarden(
          plant, plantsRepository.getNewId(), Timestamp(3000000000000L)) // TODO Change lastwatered
    }
  }

  /** Update the selected tab (Description or Health). */
  fun setTab(tab: SelectedPlantInfoTab) {
    _uiState.value = _uiState.value.copy(selectedTab = tab)
  }
}
