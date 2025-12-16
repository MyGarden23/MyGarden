package com.android.mygarden.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAchievement
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddFriend
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddedPlant
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.gardenactivity.activityclasses.GardenActivity
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileLoading
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.navigation.NavHostUtils
import com.android.mygarden.ui.navigation.NavigationActions
import com.android.mygarden.ui.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Represents the whole data to be used by the FeedScreen.
 *
 * @property activities the list of activities
 * @property hasRequests boolean to tell if the current user has requests
 */
data class FeedUIState(
    val activities: List<GardenActivity> = emptyList(),
    val hasRequests: Boolean = false,
    val isWatchingFriendsActivity: Boolean = false,
    val watchedUser1: UserProfile? = null,
    val relationWithWatchedUser1: RelationWithWatchedUser = RelationWithWatchedUser.SELF,
    val watchedUser2: UserProfile? = null,
    val relationWithWatchedUser2: RelationWithWatchedUser = RelationWithWatchedUser.SELF,
)

enum class RelationWithWatchedUser {
  FRIEND,
  NOT_FRIEND,
  REQUEST_SENT,
  SELF,
}

/**
 * The view model of the feed that handles UI interactions and repository updates
 *
 * @property activityRepo the activity repository used to collect the list of activities
 * @property friendsRepo the friends repository used to collect the list of friends
 * @property friendsRequestsRepo the friends requests repository used to know if the user have
 *   incoming requests
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val activityRepo: ActivityRepository = ActivityRepositoryProvider.repository,
    private val friendsRepo: FriendsRepository = FriendsRepositoryProvider.repository,
    private val friendsRequestsRepo: FriendRequestsRepository =
        FriendRequestsRepositoryProvider.repository,
    private val userProfileRepo: UserProfileRepository = UserProfileRepositoryProvider.repository,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val navigationActions: NavigationActions? = null
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
      val activitiesFlow =
          friendsRepo.friendsFlow(currentUserId).flatMapLatest { friends ->
            // add the current user id to the list
            val allIds = listOf(currentUserId) + friends
            // fetch all activities to display
            activityRepo.getFeedActivities(allIds, limit = 500)
          }

      val hasRequestsFlow = friendsRequestsRepo.incomingRequests().map { it.isNotEmpty() }

      combine(activitiesFlow, hasRequestsFlow) { activities, hasRequests ->
            FeedUIState(activities = activities, hasRequests = hasRequests)
          }
          .collect { newState -> _uiState.value = newState }
    }
  }

  fun setIsWatchingFriendsActivity(isWatchingFriendsActivity: Boolean) {
    _uiState.value = _uiState.value.copy(isWatchingFriendsActivity = isWatchingFriendsActivity)
  }

  suspend fun setWatchedFriends(watchedFriend1: UserProfile?, watchedFriend2: UserProfile?) {
    _uiState.value = _uiState.value.copy(watchedUser1 = watchedFriend1)
    _uiState.value = _uiState.value.copy(watchedUser2 = watchedFriend2)

    if (profileRepo.isCurrentUserPseudo(watchedFriend1?.pseudo ?: "")) {
      _uiState.value = _uiState.value.copy(relationWithWatchedUser1 = RelationWithWatchedUser.SELF)
    } else {
      if (friendsRepo.isFriend(watchedFriend1?.id ?: "")) {
        _uiState.value =
            _uiState.value.copy(relationWithWatchedUser1 = RelationWithWatchedUser.FRIEND)
      } else {
        if (friendsRequestsRepo.isInOutgoingRequests(watchedFriend1?.id ?: "")) {
          _uiState.value =
              _uiState.value.copy(relationWithWatchedUser1 = RelationWithWatchedUser.REQUEST_SENT)
        } else {
          _uiState.value =
              _uiState.value.copy(relationWithWatchedUser1 = RelationWithWatchedUser.NOT_FRIEND)
        }
      }
    }

    if (profileRepo.isCurrentUserPseudo(watchedFriend2?.pseudo ?: "")) {
      _uiState.value = _uiState.value.copy(relationWithWatchedUser2 = RelationWithWatchedUser.SELF)
    } else {
      if (friendsRepo.isFriend(watchedFriend2?.id ?: "")) {
        _uiState.value =
            _uiState.value.copy(relationWithWatchedUser2 = RelationWithWatchedUser.FRIEND)
      } else {
        if (friendsRequestsRepo.isInOutgoingRequests(watchedFriend2?.id ?: "")) {
          _uiState.value =
              _uiState.value.copy(relationWithWatchedUser2 = RelationWithWatchedUser.REQUEST_SENT)
        } else {
          _uiState.value =
              _uiState.value.copy(relationWithWatchedUser2 = RelationWithWatchedUser.NOT_FRIEND)
        }
      }
    }
  }

  fun resetWatchedFriends(watchedFriend1: UserProfile?, watchedFriend2: UserProfile?) {
    _uiState.value = _uiState.value.copy(watchedUser1 = watchedFriend1)
    _uiState.value = _uiState.value.copy(watchedUser2 = watchedFriend2)
    _uiState.value = _uiState.value.copy(relationWithWatchedUser1 = RelationWithWatchedUser.SELF)
    _uiState.value = _uiState.value.copy(relationWithWatchedUser2 = RelationWithWatchedUser.SELF)
    _uiState.value = _uiState.value.copy(watchedUser1 = null)
    _uiState.value = _uiState.value.copy(watchedUser2 = null)
  }

  fun updateWatchedFriends(watchedFriend1: UserProfile?, watchedFriend2: UserProfile?) {
    viewModelScope.launch { setWatchedFriends(watchedFriend1, watchedFriend2) }
  }
  /**
   * Function that handles the click on an activity card
   *
   * @param activity the activity that was clicked
   * @param navigationActions
   * @param navController
   */
  fun handleActivityClick(
      activity: GardenActivity,
      navigationActions: NavigationActions,
      navController: NavHostController
  ) {
    when (activity) {
      is ActivityAddedPlant -> {
        NavHostUtils.navigateToPlantInfoFromGarden(
            navController, navigationActions, activity.ownedPlant.id, true, activity.userId)
      }
      is ActivityWaterPlant -> {
        NavHostUtils.navigateToPlantInfoFromGarden(
            navController, navigationActions, activity.ownedPlant.id, true, activity.userId)
      }
      is ActivityAchievement -> {}
      is ActivityAddFriend -> {
        setIsWatchingFriendsActivity(true)
        // So it resets everytime while loading
        resetWatchedFriends(UserProfileLoading.profile, UserProfileLoading.profile)
        viewModelScope.launch {
          val watchedUser1 = userProfileRepo.getUserProfile(activity.userId)
          val watchedUser2 = userProfileRepo.getUserProfile(activity.friendUserId)
          setWatchedFriends(watchedUser1, watchedUser2)
        }
      }
    }
  }

  fun handleFriendActivityClick(friendId: String) {
    navigationActions?.navTo(Screen.FriendGarden(friendId))
  }

  fun handleNotFriendActivityClick(friendId: String) {
    viewModelScope.launch { friendsRequestsRepo.askFriend(friendId) }
    updateWatchedFriends(_uiState.value.watchedUser1, _uiState.value.watchedUser2)
  }

  fun handleSelfActivityClick() {
    navigationActions?.navTo(Screen.Garden)
  }
}

class FeedViewModelFactory(
    private val navigationActions: NavigationActions,
    private val navController: NavHostController
) : ViewModelProvider.Factory {
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    return FeedViewModel(navigationActions = navigationActions) as T
  }
}
