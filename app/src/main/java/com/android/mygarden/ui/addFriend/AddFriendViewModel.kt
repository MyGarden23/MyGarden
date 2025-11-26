package com.android.mygarden.ui.addFriend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.profile.PseudoRepository
import com.android.mygarden.model.profile.PseudoRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state container for the *Add Friend* screen.
 *
 * @property query The current pseudo typed by the user in the search bar.
 * @property isSearching Indicates whether a search request is in progress.
 * @property searchResults The list of public user profiles matching the search query.
 *
 * This state is kept intentionally minimal because error and success messages are handled through
 * callback parameters passed from the UI to the ViewModel.
 */
data class AddFriendUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<UserProfile> = emptyList(),
)

/**
 * ViewModel responsible for managing the logic of the *Add Friend* screen.
 *
 * This ViewModel coordinates three repository layers:
 * - [PseudoRepository] for searching users by pseudo prefix,
 * - [UserProfileRepository] for retrieving public user profiles (pseudo + avatar),
 * - [FriendsRepository] for adding a user to the current user's friend list.
 *
 * The ViewModel maintains a small UI state ([AddFriendUiState]) containing the search query,
 * loading status, and search results. The logic for showing visual feedback is delegated to the UI
 * for improved flexibility and consistency.
 */
class AddFriendViewModel(
    private val friendsRepository: FriendsRepository = FriendsRepositoryProvider.repository,
    private val userProfileRepository: UserProfileRepository =
        UserProfileRepositoryProvider.repository,
    private val pseudoRepository: PseudoRepository = PseudoRepositoryProvider.repository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AddFriendUiState())
  val uiState: StateFlow<AddFriendUiState> = _uiState.asStateFlow()

  /** Updates the search query typed by the user. */
  fun onQueryChange(newQuery: String) {
    _uiState.value = _uiState.value.copy(query = newQuery)
  }
  /**
   * Searches for users whose pseudo starts with the current query.
   *
   * Errors (e.g., Firestore failures) do not throw directly. Instead, [onError] is invoked so the
   * UI can display a localized message.
   *
   * @param onError A callback executed if the search fails or the query is invalid.
   */
  fun onSearch(onError: () -> Unit) {
    val rawQuery = _uiState.value.query.trim().lowercase()
    if (rawQuery.length < 2) {
      onError() // create xml with "Please type at least 2 characters."
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSearching = true)

      try {
        val pseudos = pseudoRepository.searchPseudoStartingWith(rawQuery)
        val profiles = mutableListOf<UserProfile>()

        for (pseudo in pseudos) {
          val userId = pseudoRepository.getUidFromPseudo(pseudo) ?: continue
          val userProfile = userProfileRepository.getUserProfile(userId) ?: continue
          profiles.add(userProfile)
        }

        _uiState.value = _uiState.value.copy(isSearching = false, searchResults = profiles)
      } catch (_: Exception) {
        onError()
        _uiState.value = _uiState.value.copy(isSearching = false)
      }
    }
  }

  /**
   * Adds the user with the given [userId] to the current user's friend list.
   *
   * @param userId The Firestore ID of the user to add as a friend.
   * @param onError Invoked if the operation fails for any reason.
   * @param onSuccess Invoked if the friend was added successfully.
   */
  fun onAdd(userId: String, onError: () -> Unit, onSuccess: () -> Unit) {

    viewModelScope.launch {
      try {
        friendsRepository.addFriend(userId)
        onSuccess() // create xml with "Friend added successfully."
        _uiState.value =
            _uiState.value.copy(
                searchResults =
                    _uiState.value.searchResults.updateRelation(userId, FriendRelation.ADDED))
      } catch (_: Exception) {
        onError() // create xml with "Failed to add friend."
      }
    }
  }

  /**
   * Returns a new list where the [UserProfile] with the given [id] has its [friendRelation]
   * replaced by [newRelation] if it was not already [newRelation].
   *
   * This function does not mutate the original list. If no element matches the provided [id], the
   * original list is returned unchanged.
   *
   * @param id The identifier of the user whose relation should be updated.
   * @param newRelation The new [FriendRelation] to assign to the matching user.
   * @return A new `List<UserProfile>` with the updated relation applied.
   */
  fun List<UserProfile>.updateRelation(id: String, newRelation: FriendRelation) = map {
    if (it.id == id && it.friendRelation != newRelation) it.copy(friendRelation = newRelation)
    else it
  }
}
