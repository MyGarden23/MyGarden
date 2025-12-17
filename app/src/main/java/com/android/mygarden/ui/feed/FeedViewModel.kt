package com.android.mygarden.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Represents the whole data to be used by the FeedScreen.
 *
 * @property activities the list of activities
 * @property hasRequests boolean to tell if the current user has requests
 * @property isWatchingFriendsActivity whether the user is currently watching friends' activity
 *   details
 * @property watchedUser1 the first watched user profile, or null
 * @property relationWithWatchedUser1 the relationship between the current user and watchedUser1
 * @property watchedUser2 the second watched user profile, or null
 * @property relationWithWatchedUser2 the relationship between the current user and watchedUser2
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

/**
 * Enum representing the relationship between the current user and a watched user.
 *
 * @property FRIEND The watched user is a friend of the current user.
 * @property NOT_FRIEND The watched user has no relationship with the current user.
 * @property REQUEST_SENT The current user has sent a friend request to the watched user.
 * @property REQUEST_RECEIVED The current user has received a friend request from the watched user.
 * @property SELF The watched user is the current user themselves.
 */
enum class RelationWithWatchedUser {
  FRIEND,
  NOT_FRIEND,
  REQUEST_SENT,
  REQUEST_RECEIVED,
  SELF,
}

/**
 * Callbacks for handling user interactions in the FeedViewModel.
 *
 * @property onAddPlantActivityClicked callback invoked when an "add plant" activity is clicked
 * @property onWaterPlantActivityClicked callback invoked when a "water plant" activity is clicked
 * @property goToFriendGardenPopupClick callback invoked when navigating to a friend's garden popup
 * @property onSelfActivityClick callback invoked when the current user's own activity is clicked
 */
data class FeedViewModelCallbacks(
    val onAddPlantActivityClicked: (ActivityAddedPlant) -> Unit = {},
    val onWaterPlantActivityClicked: (ActivityWaterPlant) -> Unit = {},
    val goToFriendGardenPopupClick: (friendID: String) -> Unit = {},
    val onSelfActivityClick: () -> Unit = {},
)

/**
 * The view model of the feed that handles UI interactions and repository updates
 *
 * @property activityRepo the activity repository used to collect the list of activities
 * @property friendsRepo the friends repository used to collect the list of friends
 * @property friendsRequestsRepo the friends requests repository used to know if the user have
 *   incoming requests
 * @property userProfileRepo the user profile repository used to fetch user profiles
 * @property profileRepo the profile repository used to check if a user is the current user
 * @property navigationActions the navigation actions for handling navigation, or null
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val activityRepo: ActivityRepository = ActivityRepositoryProvider.repository,
    private val friendsRepo: FriendsRepository = FriendsRepositoryProvider.repository,
    private val friendsRequestsRepo: FriendRequestsRepository =
        FriendRequestsRepositoryProvider.repository,
    private val userProfileRepo: UserProfileRepository = UserProfileRepositoryProvider.repository,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val feedViewModelCallbacks: FeedViewModelCallbacks? = null
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
            activityRepo.getFeedActivities(allIds, limit = 100)
          }

      val hasRequestsFlow = friendsRequestsRepo.incomingRequests().map { it.isNotEmpty() }

      combine(activitiesFlow, hasRequestsFlow) { activities, hasRequests ->
            FeedUIState(activities = activities, hasRequests = hasRequests)
          }
          .collect { newState -> _uiState.value = newState }
    }
  }

  /**
   * Sets whether the user is currently watching friends' activity details.
   *
   * @param isWatchingFriendsActivity true if watching friends' activity, false otherwise
   */
  fun setIsWatchingFriendsActivity(isWatchingFriendsActivity: Boolean) {
    _uiState.value = _uiState.value.copy(isWatchingFriendsActivity = isWatchingFriendsActivity)
  }

  /**
   * Determines the relationship between the current user and a watched user.
   *
   * @param userProfile the profile of the user to check the relationship with
   * @return the [RelationWithWatchedUser] enum value representing the relationship
   */
  private suspend fun determineRelationWithUser(
      userProfile: UserProfile?
  ): RelationWithWatchedUser {
    if (profileRepo.isCurrentUserPseudo(userProfile?.pseudo ?: "")) {
      return RelationWithWatchedUser.SELF
    }

    val userId = userProfile?.id ?: ""

    return when {
      friendsRepo.isFriend(userId) -> RelationWithWatchedUser.FRIEND
      friendsRequestsRepo.isInOutgoingRequests(userId) -> RelationWithWatchedUser.REQUEST_SENT
      friendsRequestsRepo.isInIncomingRequests(userId) -> RelationWithWatchedUser.REQUEST_RECEIVED
      else -> RelationWithWatchedUser.NOT_FRIEND
    }
  }

  /**
   * Sets the watched friends and updates their relationship status with the current user.
   *
   * @param watchedFriend1 the first friend to watch, or null
   * @param watchedFriend2 the second friend to watch, or null
   */
  suspend fun setWatchedFriends(watchedFriend1: UserProfile?, watchedFriend2: UserProfile?) {
    val relation1 = determineRelationWithUser(watchedFriend1)
    val relation2 = determineRelationWithUser(watchedFriend2)

    _uiState.value =
        _uiState.value.copy(
            watchedUser1 = watchedFriend1,
            watchedUser2 = watchedFriend2,
            relationWithWatchedUser1 = relation1,
            relationWithWatchedUser2 = relation2)
  }

  /**
   * Resets the watched friends to null and their relationship status to SELF.
   *
   * @param watchedFriend1 ignored parameter (kept for API compatibility)
   * @param watchedFriend2 ignored parameter (kept for API compatibility)
   */
  fun resetWatchedFriends(watchedFriend1: UserProfile?, watchedFriend2: UserProfile?) {
    _uiState.value =
        _uiState.value.copy(
            watchedUser1 = null,
            watchedUser2 = null,
            relationWithWatchedUser1 = RelationWithWatchedUser.SELF,
            relationWithWatchedUser2 = RelationWithWatchedUser.SELF)
  }

  /**
   * Updates the watched friends asynchronously in a coroutine scope.
   *
   * @param watchedFriend1 the first friend to watch, or null
   * @param watchedFriend2 the second friend to watch, or null
   */
  fun updateWatchedFriends(watchedFriend1: UserProfile?, watchedFriend2: UserProfile?) {
    viewModelScope.launch { setWatchedFriends(watchedFriend1, watchedFriend2) }
  }

  /**
   * Handles the click event when a user clicks on an activity card in the feed. Depending on the
   * activity type, navigates to the relevant screen or updates watched users.
   * - For [ActivityAddedPlant], navigates to the plant info screen.
   * - For [ActivityWaterPlant], navigates to the plant info screen.
   * - For [ActivityAchievement], does nothing.
   * - For [ActivityAddFriend], sets watched users and relationship state.
   *
   * @param activity the activity that was clicked
   */
  fun handleActivityClick(activity: GardenActivity) {
    when (activity) {
      is ActivityAddedPlant -> feedViewModelCallbacks?.onAddPlantActivityClicked(activity)
      is ActivityWaterPlant -> feedViewModelCallbacks?.onWaterPlantActivityClicked(activity)
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

  /**
   * Handles the click event on a friend's activity card. Navigates to the friend's garden screen.
   *
   * @param friendId the ID of the friend whose activity was clicked
   */
  fun handleFriendActivityClick(friendId: String) {
    feedViewModelCallbacks?.goToFriendGardenPopupClick(friendId)
  }

  /**
   * Handles the click event on a non-friend's activity card. Sends a friend request to the user and
   * updates watched friends list.
   *
   * @param friendId the ID of the user to send a friend request to
   */
  fun handleNotFriendActivityClick(friendId: String) {
    viewModelScope.launch { friendsRequestsRepo.askFriend(friendId) }
    updateWatchedFriends(_uiState.value.watchedUser1, _uiState.value.watchedUser2)
  }

  /**
   * Handles the click event on an activity card from someone who sent a friend request. Accepts the
   * incoming friend request.
   *
   * @param friendId the ID of the user who sent the friend request
   */
  fun handleRequestReceivedActivityClick(friendId: String) {
    viewModelScope.launch {
      val request =
          friendsRequestsRepo.incomingRequests().first().first { it.fromUserId == friendId }
      friendsRequestsRepo.acceptRequest(request.id)
    }
  }

  /**
   * Handles the click event on the current user's own activity card. Navigates to the user's own
   * garden screen.
   */
  fun handleSelfActivityClick() {
    feedViewModelCallbacks?.onSelfActivityClick()
  }
}

/**
 * Factory class for creating [FeedViewModel] instances with custom navigation actions.
 *
 * @property navigationActions the navigation actions to be used by the ViewModel, or null
 */
class FeedViewModelFactory(private val feedViewModelCallbacks: FeedViewModelCallbacks?) :
    ViewModelProvider.Factory {
  /**
   * Creates a new instance of the specified ViewModel class.
   *
   * @param modelClass the class of the ViewModel to create
   * @return a new instance of [FeedViewModel]
   * @throws IllegalArgumentException if the modelClass is not [FeedViewModel]
   */
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return FeedViewModel(feedViewModelCallbacks = feedViewModelCallbacks) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
  }
}
