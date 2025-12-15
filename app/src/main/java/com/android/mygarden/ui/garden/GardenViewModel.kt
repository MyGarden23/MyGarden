package com.android.mygarden.ui.garden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.mygarden.R
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.gardenactivity.activityclasses.ActivityWaterPlant
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.LikesRepository
import com.android.mygarden.model.profile.LikesRepositoryProvider
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.profile.Avatar
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Represents the UI state of the garden screen.
 *
 * @property plants the list of plants owned by the current user : empty by default
 * @property filteredAndSortedPlants the list of plants after applying filters and sorting
 * @property currentSortOption the currently selected sorting option
 * @property currentFilterOption the currently selected filtering option
 * @property errorMsg a potential error message to be displayed if one of the actions of the view
 *   model with the repository doesn't work
 * @property userName the name of the user of the app that is displayed
 * @property userAvatar the avatar of the user of the app that is displayed
 * @property likesCount the number of likes the garden has
 * @property hasLiked true if the user already liked this garden, false otherwise
 * @property isLikeUpdating true if the like button is updating
 */
data class GardenUIState(
    val plants: List<OwnedPlant> = emptyList(),
    val filteredAndSortedPlants: List<OwnedPlant> = emptyList(),
    val currentSortOption: SortOption = SortOption.PLANT_NAME,
    val currentFilterOption: FilterOption = FilterOption.ALL,
    val errorMsg: Int? = null,
    val userName: String = "",
    val userAvatar: Avatar = Avatar.A1,
    val likesCount: Int = 0,
    val hasLiked: Boolean = false,
    val isLikeUpdating: Boolean = false
)

/**
 * The view model of the garden that handles all UI interactions.
 *
 * @property plantsRepo the repository of the plants to store them
 * @property profileRepo the repository of the profiles used to fetch user information
 * @property userProfileRepo the repository used to fetch public profile info of other users
 * @property activityRepo the repository of the activities to store them
 * @property likesRepo the repository of the likes to store them
 * @property friendId optional ID of a friend whose garden to display (null for own garden)
 */
class GardenViewModel(
    private val plantsRepo: PlantsRepository = PlantsRepositoryProvider.repository,
    private val profileRepo: ProfileRepository = ProfileRepositoryProvider.repository,
    private val userProfileRepo: UserProfileRepository = UserProfileRepositoryProvider.repository,
    private val activityRepo: ActivityRepository = ActivityRepositoryProvider.repository,
    private val likesRepo: LikesRepository = LikesRepositoryProvider.repository,
    private val friendId: String? = null
) : ViewModel() {
  private val _uiState = MutableStateFlow(GardenUIState())
  val uiState: StateFlow<GardenUIState> = _uiState.asStateFlow()

  /**
   * The value of the list now changes everytime it receives an update from the plants repository in
   * order to display the correct list of owned plants of the user
   */
  init {
    refreshUIState()

    // Likes: observe count for the displayed garden (own or friend's)
    viewModelScope.launch {
      val targetUid = friendId ?: profileRepo.getCurrentUserId()!!
      likesRepo.observeLikesCount(targetUid).collect { count ->
        _uiState.value = _uiState.value.copy(likesCount = count)
      }
    }

    // Likes: only relevant when viewing a friend's garden
    if (friendId != null) {
      viewModelScope.launch {
        val myUid = profileRepo.getCurrentUserId()
        if (myUid != null) {
          likesRepo.observeHasLiked(friendId, myUid).collect { liked ->
            _uiState.value = _uiState.value.copy(hasLiked = liked)
          }
        }
      }
    }
    // Only subscribe to plantsFlow if viewing own garden (not a friend's)
    if (friendId == null) {
      viewModelScope.launch {
        plantsRepo.plantsFlow.collect { newList ->
          _uiState.value = _uiState.value.copy(plants = newList)
          applyFiltersAndSorting()
        }
      }
    }
  }

  /** Refresh the UI state by fetching all the owned plants and user information. */
  fun refreshUIState() {
    fetchProfileInfos()
    viewModelScope.launch {
      try {
        if (friendId != null) {
          // Load friend's plants and update state (not covered by plantsFlow)
          val plants = plantsRepo.getAllOwnedPlantsByUserId(friendId)
          _uiState.value = _uiState.value.copy(plants = plants)
          applyFiltersAndSorting()
        } else {
          // Load own plants - plantsFlow subscription will handle the state update
          plantsRepo.getAllOwnedPlants()
        }
      } catch (e: Exception) {
        setErrorMsg(R.string.error_failed_load_plant_edit)
      }
    }
  }

  fun toggleLike() {
    if (friendId == null) return

    viewModelScope.launch {
      if (_uiState.value.isLikeUpdating) return@launch
      _uiState.value = _uiState.value.copy(isLikeUpdating = true)

      try {
        val myUid = profileRepo.getCurrentUserId() ?: return@launch
        likesRepo.toggleLike(friendId, myUid)
      } catch (_: Exception) {
        setErrorMsg(R.string.error_failed_get_profile_garden)
      } finally {
        _uiState.value = _uiState.value.copy(isLikeUpdating = false)
      }
    }
  }

  /** Clears the error message of the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /**
   * Sets the error message of the UI state.
   *
   * @param msg the error message
   */
  fun setErrorMsg(resId: Int) {
    _uiState.value = _uiState.value.copy(errorMsg = resId)
  }

  /**
   * Wrapper for the repository homologous function that update the given plant last time watered to
   * the current time of the device.
   *
   * @param ownedPlant the plant that is being watered
   */
  fun waterPlant(ownedPlant: OwnedPlant) {
    viewModelScope.launch {
      val wateringTime = Timestamp(System.currentTimeMillis())
      val pseudo = profileRepo.getProfile().firstOrNull()?.pseudo
      val userId = activityRepo.getCurrentUserId()

      // Water the plant first
      plantsRepo.waterPlant(ownedPlant.id, wateringTime)

      // Then create the activity if we have the necessary information
      if (pseudo != null && userId != null) {
        activityRepo.addActivity(
            ActivityWaterPlant(
                userId = userId,
                pseudo = pseudo,
                createdAt = wateringTime,
                ownedPlant = ownedPlant))
      }
    }
  }

  /**
   * Sets the current sort option and re-applies filters and sorting.
   *
   * @param option the new sorting option to apply
   */
  fun setSortOption(option: SortOption) {
    applyFiltersAndSorting(sortOption = option)
  }

  /**
   * Sets the current filter option and re-applies filters and sorting.
   *
   * @param option the new filtering option to apply
   */
  fun setFilterOption(option: FilterOption) {
    applyFiltersAndSorting(filterOption = option)
  }

  /**
   * Applies the current filter and sort options to the plants list.
   *
   * This function chains filtering and sorting operations to produce the final list that should be
   * displayed in the UI.
   *
   * @param sortOption the sorting option to apply (defaults to current option in UI state)
   * @param filterOption the filtering option to apply (defaults to current option in UI state)
   */
  private fun applyFiltersAndSorting(
      sortOption: SortOption = _uiState.value.currentSortOption,
      filterOption: FilterOption = _uiState.value.currentFilterOption
  ) {
    val currentState = _uiState.value
    val filtered = filterPlants(currentState.plants, filterOption)
    val sorted = sortPlants(filtered, sortOption)
    _uiState.value =
        _uiState.value.copy(
            filteredAndSortedPlants = sorted,
            currentSortOption = sortOption,
            currentFilterOption = filterOption)
  }

  /**
   * Sorts a list of plants according to the specified sorting option.
   *
   * @param plants the list of plants to sort
   * @param option the sorting option to apply
   * @return the sorted list of plants
   */
  private fun sortPlants(plants: List<OwnedPlant>, option: SortOption): List<OwnedPlant> {
    return when (option) {
      SortOption.PLANT_NAME -> plants.sortedBy { it.plant.name.lowercase() }
      SortOption.LATIN_NAME -> plants.sortedBy { it.plant.latinName.lowercase() }
      SortOption.LAST_WATERED_ASC -> plants.sortedBy { it.lastWatered.time }
      SortOption.LAST_WATERED_DESC -> plants.sortedByDescending { it.lastWatered.time }
    }
  }

  /**
   * Filters a list of plants according to the specified filtering option.
   *
   * @param plants the list of plants to filter
   * @param option the filtering option to apply
   * @return the filtered list of plants
   */
  private fun filterPlants(plants: List<OwnedPlant>, option: FilterOption): List<OwnedPlant> {
    return when (option) {
      // No filtering - return all plants
      FilterOption.ALL -> plants
      // Keep only overwatered plants
      FilterOption.OVERWATERED_ONLY ->
          plants.filter {
            it.plant.healthStatus == PlantHealthStatus.OVERWATERED ||
                it.plant.healthStatus == PlantHealthStatus.SEVERELY_OVERWATERED
          }
      // Keep only dry plants that need water
      FilterOption.DRY_PLANTS ->
          plants.filter {
            it.plant.healthStatus == PlantHealthStatus.NEEDS_WATER ||
                it.plant.healthStatus == PlantHealthStatus.SLIGHTLY_DRY ||
                it.plant.healthStatus == PlantHealthStatus.SEVERELY_DRY
          }
      // Keep only critically unhealthy plants (severely dry or severely overwatered)
      FilterOption.CRITICAL_ONLY ->
          plants.filter {
            it.plant.healthStatus == PlantHealthStatus.SEVERELY_DRY ||
                it.plant.healthStatus == PlantHealthStatus.SEVERELY_OVERWATERED
          }
      // Keep only healthy plants
      FilterOption.HEALTHY_ONLY ->
          plants.filter { it.plant.healthStatus == PlantHealthStatus.HEALTHY }
    }
  }

  /**
   * Fetches all needed user information from the appropriate repository or set an error message if
   * the fetch failed.
   */
  private fun fetchProfileInfos() {
    viewModelScope.launch {
      try {
        if (friendId != null) {
          // Load friend's profile using UserProfileRepository
          val userProfile = userProfileRepo.getUserProfile(friendId)
          if (userProfile != null) {
            _uiState.value =
                _uiState.value.copy(userName = userProfile.pseudo, userAvatar = userProfile.avatar)
          } else {
            setErrorMsg(R.string.error_failed_get_profile_garden)
          }
        } else {
          // Load own profile using ProfileRepository
          profileRepo.getProfile().collect { profile ->
            if (profile != null) {
              _uiState.value =
                  _uiState.value.copy(userName = profile.pseudo, userAvatar = profile.avatar)
            } else {
              setErrorMsg(R.string.error_failed_get_profile_garden)
            }
          }
        }
      } catch (_: Exception) {
        setErrorMsg(R.string.error_failed_get_profile_garden)
      }
    }
  }
}

/**
 * Factory for creating GardenViewModel instances with custom parameters.
 *
 * @param friendId optional ID of a friend whose garden to display (null for own garden)
 */
class GardenViewModelFactory(
    private val friendId: String? = null,
) : ViewModelProvider.Factory {
  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(GardenViewModel::class.java)) {
      return GardenViewModel(
          plantsRepo = PlantsRepositoryProvider.repository,
          profileRepo = ProfileRepositoryProvider.repository,
          userProfileRepo = UserProfileRepositoryProvider.repository,
          activityRepo = ActivityRepositoryProvider.repository,
          likesRepo = LikesRepositoryProvider.repository,
          friendId = friendId)
          as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
