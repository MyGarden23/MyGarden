package com.android.mygarden.ui.friendsRequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityAddFriend
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * The whole data used by the FriendsRequestsScreen
 *
 * @property pendingRequestsUsers the list of [UserProfile] that have asked the user to be friends
 */
data class FriendsRequestsUIState(
    val pendingRequestsUsers: List<UserProfile> = emptyList(),
    val pendingRequests: List<FriendRequest> = emptyList()
)

/**
 * View Model that handles interactions between the model (repositories) and the UI (here the
 * FriendsRequestsScreen)
 *
 * @property userProfileRepo the repository of userProfile, used to retrieve more infos about the
 *   users requesting a friendship
 * @property requestsRepo the repository of requests
 * @property friendsRepo the repository of friends, used to add friends after accepting a request
 * @property activityRepo the repository of activities, used to log friendship activities
 * @property profileRepo the repository of profiles, used to get the current user's profile
 * @property achievementsRepo the repository of achievements, used to update friend count
 *   achievements
 */
class FriendsRequestsViewModel(
    private val userProfileRepo: UserProfileRepository = UserProfileRepositoryProvider.repository,
    private val requestsRepo: FriendRequestsRepository =
        FriendRequestsRepositoryProvider.repository,
    private val friendsRepo: FriendsRepository = FriendsRepositoryProvider.repository,
    private val activityRepo: ActivityRepository = ActivityRepositoryProvider.repository,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val achievementsRepo: AchievementsRepository = AchievementsRepositoryProvider.repository
) : ViewModel() {

  private val _uiState = MutableStateFlow(FriendsRequestsUIState())
  val uiState: StateFlow<FriendsRequestsUIState> = _uiState.asStateFlow()

  init {
    refreshUIState()
  }

  /** Handles the collecting of new requests */
  fun refreshUIState() {
    viewModelScope.launch {
      // collect the requests
      requestsRepo.incomingRequests().collect { incoming ->
        // transform it into UserProfiles if the users are found (still active accounts)
        val users = incoming.mapNotNull { userProfileRepo.getUserProfile(it.fromUserId) }
        _uiState.value =
            _uiState.value.copy(pendingRequests = incoming, pendingRequestsUsers = users)
      }
    }
  }

  /**
   * accept a request (called when clicked on the accept button)
   *
   * @param newFriendId the id of the user that has been accepted
   */
  fun acceptRequest(requestId: String, newFriendId: String, newFriendPseudo: String) {
    viewModelScope.launch {
      // Accept the friend request (updates status)
      val fromUserId = requestsRepo.acceptRequest(requestId)

      // Add the friend to both users' friends lists and get updated counts
      val friendCounts = friendsRepo.addFriend(fromUserId)

      // Update achievements for both users with their new friend counts
      val currentUserId = activityRepo.getCurrentUserId() ?: ""
      achievementsRepo.updateAchievementValue(
          currentUserId, AchievementType.FRIENDS_NUMBER, friendCounts.currentUserFriendCount)
      achievementsRepo.updateAchievementValue(
          fromUserId, AchievementType.FRIENDS_NUMBER, friendCounts.addedFriendCount)

      // Add the new activity that 2 users have become friends
      val currentUserPseudo = profileRepo.getProfile().firstOrNull()?.pseudo ?: ""
      activityRepo.addActivity(
          ActivityAddFriend(
              userId = currentUserId,
              pseudo = currentUserPseudo,
              friendUserId = newFriendId,
              friendPseudo = newFriendPseudo))
    }
  }

  /**
   * decline a request (called when clicked on the decline button)
   *
   * @param sadNonFriendId the id of the user that has been rejected
   */
  fun declineRequest(sadNonFriendId: String) {
    viewModelScope.launch { requestsRepo.refuseRequest(sadNonFriendId) }
  }
}
