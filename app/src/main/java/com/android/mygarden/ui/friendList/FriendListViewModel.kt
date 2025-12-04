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

/**
 * UI state for the FriendList screen.
 *
 * @property isLoading indicates whether friend data is currently being loaded
 * @property friends the list of fully-resolved [UserProfile] objects of the user's friends
 */
data class FriendListUiState(
    val isLoading: Boolean = false,
    val friends: List<UserProfile> = emptyList()
)

/**
 * ViewModel responsible for loading and exposing the user's list of friends.
 *
 * @property friendsRepository repository used to retrieve friend UIDs
 * @property userProfileRepository repository that provides detailed user profiles
 * @property auth Firebase authentication instance used to obtain the current user
 */
class FriendListViewModel(
    private val friendsRepository: FriendsRepository = FriendsRepositoryProvider.repository,
    private val userProfileRepository: UserProfileRepository =
        UserProfileRepositoryProvider.repository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

  private val _uiState = MutableStateFlow(FriendListUiState())
  val uiState: StateFlow<FriendListUiState> = _uiState.asStateFlow()

  /** The currently authenticated Firebase user. */
  val currentUser = auth.currentUser!!

  /**
   * Loads the list of friends for the current user.
   *
   * @param onError callback invoked if the friend list could not be loaded
   */
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
