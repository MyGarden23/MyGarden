package com.android.mygarden.ui.friendsRequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The whole data used by the FriendsRequestsScreen
 *
 * @property pendingRequestsUsers the list of [UserProfile] that have asked the user to be friends
 */
data class FriendsRequestsUIState(val pendingRequestsUsers: List<UserProfile> = emptyList())

/**
 * View Model that handles interactions between the model (both repositories below) and the UI (here
 * the FriendsRequestsScreen)
 *
 * @property userProfileRepo the repository of userProfile, used to retrieve more infos about the
 *   users requesting a friendship
 * @property requestsRepo the repository of requests
 */
class FriendsRequestsViewModel(
    private val userProfileRepo: UserProfileRepository = UserProfileRepositoryProvider.repository,
    private val requestsRepo: FriendRequestsRepository = FriendRequestsRepositoryProvider.repository
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
        _uiState.value = _uiState.value.copy(pendingRequestsUsers = users)
      }
    }
  }

  /**
   * accept a request (called when clicked on the accept button)
   *
   * @param newFriendId the id of the user that has been accepted
   */
  fun acceptRequest(newFriendId: String) {
    viewModelScope.launch { requestsRepo.acceptRequest(newFriendId) }
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
