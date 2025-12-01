package com.android.mygarden.ui.friendsRequests

import android.util.Log
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

data class FriendRequestUiModel(val request: FriendRequest, val senderPseudo: String)

/**
 * ViewModel for emitting only *new* friend requests to the UI.
 *
 * It mirrors the structure of PopupViewModel used for plant popups:
 * - Collects a Flow<List<FriendRequest>> from the repository
 * - Detects requests that appeared since last emission
 * - Emits them through a SharedFlow to trigger pop-ups in the UI
 */
class FriendsRequestsPopupViewModel(
    private val friendsRepo: FriendRequestsRepository = FriendRequestsRepositoryProvider.repository,
    private val userProfileRepo: UserProfileRepository = UserProfileRepositoryProvider.repository
) : ViewModel() {

  private val _newRequests =
      MutableSharedFlow<FriendRequestUiModel>(replay = 0, extraBufferCapacity = 64)
  val newRequests: SharedFlow<FriendRequestUiModel> = _newRequests

  private var previousCollectedList: List<FriendRequest> = emptyList()

  init {
    viewModelScope.launch {
      friendsRepo.incomingRequests().collect { list ->
        Log.d("FriendRequestsVMIncomming", list.toString())
        val previousIds = previousCollectedList.associateBy { it.id }

        val newOnes = list.filter { it.id !in previousIds }

        newOnes.forEach { newRequest ->
          Log.d("FriendRequestsVMNewRequest", newRequest.toString())
          val profile = userProfileRepo.getUserProfile(newRequest.fromUserId)
          if (profile != null) {
            _newRequests.emit(
                FriendRequestUiModel(request = newRequest, senderPseudo = profile.pseudo))
          }
        }

        previousCollectedList = list
        Log.d("FriendRequestsVMPrevious", previousCollectedList.toString())
        Log.d("FriendRequestsVMNew", newRequests.toString())
      }
    }
  }
}
