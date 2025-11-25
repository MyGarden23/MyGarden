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

/** Overall UI state for the Add Friend screen. */
data class AddFriendUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<UserProfile> = emptyList(),
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

class AddFriendViewModel(
    private val friendsRepository: FriendsRepository = FriendsRepositoryProvider.repository,
    private val userProfileRepository: UserProfileRepository =
        UserProfileRepositoryProvider.repository,
    private val pseudoRepository: PseudoRepository = PseudoRepositoryProvider.repository,
) : ViewModel() {
  private val _uiState = MutableStateFlow(AddFriendUiState())
  val uiState: StateFlow<AddFriendUiState> = _uiState.asStateFlow()

  fun onQueryChange(newQuery: String) {
    _uiState.value =
        _uiState.value.copy(
            query = newQuery, errorMessage = null, infoMessage = null, searchResults = emptyList())
  }

  fun onSearch() {
    val rawQuery = _uiState.value.query.trim().lowercase()
    if (rawQuery.length < 2) {
      _uiState.value = _uiState.value.copy(errorMessage = "Please type at least 2 characters.")
      return
    }

    viewModelScope.launch {
      _uiState.value = _uiState.value.copy(isSearching = true, errorMessage = null)

      try {
        val pseudos = pseudoRepository.searchPseudoStartingWith(rawQuery)
        val profiles = mutableListOf<UserProfile>()

        for (pseudo in pseudos) {
          val userId = pseudoRepository.getUidFromPseudo(pseudo) ?: continue
          val userProfile = userProfileRepository.getUserProfile(userId) ?: continue
          profiles.add(userProfile)
        }

        _uiState.value = _uiState.value.copy(isSearching = false, searchResults = profiles)
      } catch (e: Exception) {
        _uiState.value =
            _uiState.value.copy(
                isSearching = false, errorMessage = e.message ?: "Failed to search users.")
      }
    }
  }

  fun onAdd(userId: String) {

    viewModelScope.launch {
      try {
        friendsRepository.addFriend(userId)
        _uiState.value =
            _uiState.value.copy(infoMessage = "Friend added successfully.", errorMessage = null)
      } catch (e: Exception) {
        _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Failed to add friend.")
      }
    }
  }

  fun onMessageShown() {
    _uiState.value = _uiState.value.copy(errorMessage = null, infoMessage = null)
  }
}
