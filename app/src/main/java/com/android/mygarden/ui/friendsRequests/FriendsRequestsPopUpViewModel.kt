package com.android.mygarden.ui.friendsRequests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Data class representing a friend request with additional information.
 *
 * @property request The original friend request.
 * @property senderPseudo The pseudo of the sender of the friend request.
 */
data class FriendRequestUiModel(val request: FriendRequest, val senderPseudo: String)

/**
 * ViewModel for the friends requests popup.
 *
 * @param friendsRepo The repository for friend requests.
 * @param userProfileRepo The repository for user profiles.
 */
class FriendsRequestsPopupViewModel(
    private val friendsRepo: FriendRequestsRepository = FriendRequestsRepositoryProvider.repository,
    private val userProfileRepo: UserProfileRepository = UserProfileRepositoryProvider.repository
) : ViewModel() {

  private val _newRequests =
      MutableSharedFlow<FriendRequestUiModel>(replay = 0, extraBufferCapacity = 64)
  val newRequests: SharedFlow<FriendRequestUiModel> = _newRequests

  init {
    viewModelScope.launch {
      friendsRepo.incomingRequests().collect { list ->
        list.forEach { newRequest ->
          val profile = userProfileRepo.getUserProfile(newRequest.fromUserId)
          if (profile != null) {
            _newRequests.emit(
                FriendRequestUiModel(request = newRequest, senderPseudo = profile.pseudo))
            friendsRepo.markRequestAsSeen(newRequest.id)
          }
        }
      }
    }
  }
}
