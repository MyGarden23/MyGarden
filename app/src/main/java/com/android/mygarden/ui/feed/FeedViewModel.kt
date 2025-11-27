package com.android.mygarden.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val activityRepo: ActivityRepository = ActivityRepositoryProvider.repository,
    private val friendsRepo: FriendsRepository = FriendsRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(FeedUIState())
  val uiState: StateFlow<FeedUIState> = _uiState.asStateFlow()
  private val currentUserId: String = activityRepo.getCurrentUserId() ?: ""

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
      // listen on the flow of the list of friends of the current user
      friendsRepo
          .friendsFlow(currentUserId)
          .flatMapLatest { friends ->
            // add the current user id to the list
            val allIds = listOf(currentUserId) + friends
            // fetch all activities to display
            activityRepo.getFeedActivities(allIds)
          }
          .collect { activities -> _uiState.value = _uiState.value.copy(activities = activities) }
    }
  }
}
