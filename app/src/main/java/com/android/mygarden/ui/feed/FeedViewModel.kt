package com.android.mygarden.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
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
 * @property profileRepo the profile repository used to collect the list of activities
 */
class FeedViewModel(
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(FeedUIState())
  val uiState: StateFlow<FeedUIState> = _uiState.asStateFlow()

  /** Initially call refreshUIState to fetch the infos needed from the profile repository */
  init {
    refreshUIState()
  }

  /**
   * Collects (continuously) the list of activities from the repo to update the list to display by
   * the UI
   */
  fun refreshUIState() {
    viewModelScope.launch {
      profileRepo.getActivities().collect { updatedList ->
        // Here no need to sort it by createdAt timestamp because it's already done in the profile
        // repository
        _uiState.value = _uiState.value.copy(activities = updatedList)
      }
    }
  }
}
