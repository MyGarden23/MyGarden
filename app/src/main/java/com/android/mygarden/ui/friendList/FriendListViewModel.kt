package com.android.mygarden.ui.friendList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
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
    private val achievementsRepo: AchievementsRepository =
        AchievementsRepositoryProvider.repository,
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

  /**
   * - Deletes the [friend] from the current user's list
   * - Deletes the current user from the [friend] friends list
   * - Decreases both user's friend number achievements If any firebase call doesn't function,
   *   restore the previous friend list in the UI state
   *
   * @param friend the user with whom the current user doesn't want to be friend with anymore
   */
  fun deleteFriend(friend: UserProfile) {
    viewModelScope.launch {
      // update the UI State
      val previousFriends = _uiState.value.friends
      _uiState.value = _uiState.value.copy(friends = previousFriends - friend)

      try {
        // deletes the friend in both lists
        friendsRepository.deleteFriend(friend.id)

        // updates the friend numbers achievements in both user's achievements
        decreaseFriendNumber(currentUser.uid)
        decreaseFriendNumber(friend.id)
      } catch (_: Exception) {
        // if a firebase problem occurs, come back on previous list of friends (not deleted)
        _uiState.value = _uiState.value.copy(friends = previousFriends)
      }
    }
  }

  /**
   * Decreases the friend value from the friends number achievements by one (called when friend
   * deleted)
   *
   * @param id the id of the user that will see its achievement number decreased
   */
  private suspend fun decreaseFriendNumber(id: String) {
    val oldValue = achievementsRepo.getUserAchievementProgress(id, AchievementType.FRIENDS_NUMBER)
    oldValue?.let {
      // any friend deletion should come after a friend add, i.e. the currentValue should be > 0
      require(oldValue.currentValue > 0)
      achievementsRepo.updateAchievementValue(
          id, AchievementType.FRIENDS_NUMBER, oldValue.currentValue - 1)
    }
  }
}
