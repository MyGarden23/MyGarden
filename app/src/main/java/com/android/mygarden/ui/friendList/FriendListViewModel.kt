package com.android.mygarden.ui.friendList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FriendListUiState(
    val isLoading: Boolean = false,
    val friends: List<UserProfile> = emptyList<UserProfile>()
)

class FriendListViewModel(
    private val friendsRepository: FriendsRepository = FriendsRepositoryProvider.repository,
    private val userProfileRepository: UserProfileRepository =
        UserProfileRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(FriendListUiState())
  val uiState: StateFlow<FriendListUiState> = _uiState.asStateFlow()

  val currentUser = auth.currentUser!!

  fun getFriends(onError: () -> Unit) {
    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isLoading = true)
      try {
        val friendsUids = friendsRepository.getFriends(currentUser.uid)
        val friendsProfile = mutableListOf<UserProfile>()
        for (friendUid in friendsUids) {
          val friendProfile = userProfileRepository.getUserProfile(friendUid) ?: continue
          friendsProfile.add(friendProfile)
        }
        _uiState.value = _uiState.value.copy(friends = friendsProfile, isLoading = false)
      } catch (_: Exception) {
        onError()
        _uiState.value = _uiState.value.copy(isLoading = false)
      }
    }
  }
}
