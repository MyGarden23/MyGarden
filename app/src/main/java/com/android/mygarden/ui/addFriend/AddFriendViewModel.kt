package com.android.mygarden.ui.addFriend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.notifications.FirebaseFriendRequestNotifier
import com.android.mygarden.model.notifications.FriendRequestNotifier
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.profile.PseudoRepository
import com.android.mygarden.model.profile.PseudoRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Minimum number of characters required for a search query */
private const val MIN_QUERY_LENGTH = 2

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
    val alreadyFriend: List<UserProfile> = emptyList(),
    val relations: Map<String, FriendRelation> = emptyMap(),
)

/**
 * ViewModel responsible for managing the logic of the *Add Friend* screen.
 *
 * This ViewModel coordinates three repository layers:
 * - [PseudoRepository] for searching users by pseudo prefix,
 * - [UserProfileRepository] for retrieving public user profiles (pseudo + avatar),
 * - [FriendsRepository] for adding a user to the current user's friend list.
 * - [FriendRequestsRepository] for sending a friend request to a user.
 * - [ProfileRepository] for retrieving the current user's profile and pseudo.
 * - [FriendRequestNotifier] for sending a friend request notification to a user.
 *
 * The ViewModel maintains a small UI state ([AddFriendUiState]) containing the search query,
 * loading status, and search results. The logic for showing visual feedback is delegated to the UI
 * for improved flexibility and consistency.
 */
class AddFriendViewModel(
    private val friendsRepository: FriendsRepository = FriendsRepositoryProvider.repository,
    private val requestsRepository: FriendRequestsRepository =
        FriendRequestsRepositoryProvider.repository,
    private val userProfileRepository: UserProfileRepository =
        UserProfileRepositoryProvider.repository,
    private val pseudoRepository: PseudoRepository = PseudoRepositoryProvider.repository,
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val friendRequestNotifier: FriendRequestNotifier = FirebaseFriendRequestNotifier()
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
    if (rawQuery.length < MIN_QUERY_LENGTH) {
      onError() // create xml with "Please type at least $MIN_QUERY_LENGTH characters."
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
        refreshRelations()
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
        onSuccess()
      } catch (_: Exception) {
        onError()
      }
    }
  }

  /**
   * Sends a friend request to [userId] and updates the local relation state immediately.
   *
   * If the current relation is ADDBACK, the user is marked as ADDED; otherwise the state becomes
   * PENDING. On success, [onSuccess] is called; on failure, the relation reverts to ADD and
   * [onError] is invoked.
   *
   * @param userId The Firestore ID of the user to send a friend request to.
   * @param onError Invoked if the operation fails for any reason.
   * @param onSuccess Invoked if the friend request was sent successfully.
   *
   * This function uses the [FriendRequestNotifier] to send a notification to the user.
   */
  fun onAsk(userId: String, onError: () -> Unit, onSuccess: () -> Unit) {

    viewModelScope.launch {
      _uiState.value =
          _uiState.value.let { state ->
            if (state.relations.get(userId) == FriendRelation.ADDBACK) {
              state.copy(relations = state.relations + (userId to FriendRelation.ADDED))
            } else {
              state.copy(relations = state.relations + (userId to FriendRelation.PENDING))
            }
          }
      try {
        requestsRepository.askFriend(userId)
        val currentUserId = profileRepository.getCurrentUserId()
        if (currentUserId == null) {
          onError()
          return@launch
        }

        val currentUserProfile = userProfileRepository.getUserProfile(currentUserId)
        val fromPseudo = currentUserProfile?.pseudo

        if (fromPseudo == null) {
          onError()
          return@launch
        }

        friendRequestNotifier.notifyRequestSent(userId, fromPseudo)
        onSuccess()
      } catch (_: Exception) {
        _uiState.value =
            _uiState.value.let { state ->
              state.copy(relations = state.relations + (userId to FriendRelation.ADD))
            }
        onError()
      }
    }
  }

  /**
   * Updates the friend-relation state for all users in the current search results. Each user's
   * relation is recomputed (ADD, ADDED, or PENDING) based on repository data.
   */
  private suspend fun refreshRelations() {
    val currentFriends = _uiState.value.searchResults

    val newRelations =
        currentFriends.associate { friend ->
          val relation =
              when {
                requestsRepository.isInOutgoingRequests(friend.id) -> FriendRelation.PENDING
                requestsRepository.isInIncomingRequests(friend.id) -> FriendRelation.ADDBACK
                friendsRepository.isFriend(friend.id) -> FriendRelation.ADDED
                else -> FriendRelation.ADD
              }
          friend.id to relation
        }

    _uiState.value = _uiState.value.copy(relations = newRelations)
  }
}
