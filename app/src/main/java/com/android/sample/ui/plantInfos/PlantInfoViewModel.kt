package com.android.sample.ui.plantInfos

import android.media.Image
import androidx.lifecycle.ViewModel
import com.android.sample.model.plant.Plant
import com.android.sample.model.plant.PlantHealthStatus
import com.android.sample.model.plant.PlantsRepository
import com.android.sample.model.plant.PlantsRepositoryProvider
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
/** Enum representing which tab is currently selected in the plant info screen. */
enum class SelectedPlantInfoTab {
  DESCRIPTION,
  HEALTH_STATUS,
}

/** UI state for the plant info screen. Contains all the plant information to be displayed. */
data class PlantInfoUIState(
    val name: String = "",
    val image: Image? = null,
    val latinName: String = "",
    val description: String = "",
    val healthStatus: PlantHealthStatus = PlantHealthStatus.UNKNOWN,
    val healthStatusDescription: String = "",
    val wateringFrequency: Int = 0,
    val selectedTab: SelectedPlantInfoTab = SelectedPlantInfoTab.DESCRIPTION,
)

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
    _uiState.value =
        PlantInfoUIState(
            plant.name,
            plant.image,
            plant.latinName,
            plant.description,
            plant.healthStatus,
            plant.healthStatusDescription,
            plant.wateringFrequency,
        )
  }


}
