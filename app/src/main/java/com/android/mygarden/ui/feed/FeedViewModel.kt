package com.android.mygarden.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the whole data to be used by the FeedScreen. For now only a list of activities is
 * used.
 *
 * @property activities the list of activities
 */
data class FeedUIState(val activities: List<GardenActivity> = emptyList())

/**
 * The view model of the feed that handles UI interactions and repository updates
 *
 * @property activityRepo the activity repository used to collect the list of activities
 */
class FeedViewModel(
    private val activityRepo: ActivityRepository = ActivityRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(FeedUIState())
  val uiState: StateFlow<FeedUIState> = _uiState.asStateFlow()

  /** Initially call refreshUIState to fetch the infos needed from the activity repository */
  init {
    refreshUIState()
  }

  /**
   * Collects (continuously) the list of activities from the repo to update the list to display by
   * the UI
   */
  fun refreshUIState() {
    viewModelScope.launch {
      activityRepo.getActivities().collect { updatedList ->
        // Here no need to sort it by createdAt timestamp because it's already done in the activity
        // repository
        _uiState.value = _uiState.value.copy(activities = updatedList)
      }
    }
  }
}
