package com.android.mygarden.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.profile.Countries
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * UI state data class for the new profile creation screen, contains all the form fields and
 * validation state
 */
data class ProfileUIState(
    val firstName: String = "",
    val lastName: String = "",
    val gardeningSkill: GardeningSkill? = null,
    val favoritePlant: String = "",
    val country: String = "",
    val registerPressed: Boolean = false,
    val avatar: Avatar = Avatar.A1, // By default 1st avatar
) {

  /**
   * Validates that the first name is not blank
   *
   * @return true if first name is valid, false otherwise
   */
  private fun firstNameValid(): Boolean {
    return firstName.isNotBlank()
  }

  /**
   * Determines if the first name field should show an error
   *
   * @return true if register was pressed and first name is invalid
   */
  fun firstNameIsError(): Boolean {
    return registerPressed && !firstNameValid()
  }

  /**
   * Validates that the last name is not blank
   *
   * @return true if last name is valid, false otherwise
   */
  private fun lastNameValid(): Boolean {
    return lastName.isNotBlank()
  }

  /**
   * Determines if the last name field should show an error
   *
   * @return true if register was pressed and last name is invalid
   */
  fun lastNameIsError(): Boolean {
    return registerPressed && !lastNameValid()
  }

  /**
   * Validates that the selected country is in the list of valid countries
   *
   * @return true if country is valid, false otherwise
   */
  private fun countryValid(): Boolean {
    return Countries.ALL.contains(country)
  }

  /**
   * Determines if the country field should show an error
   *
   * @return true if register was pressed and country is invalid
   */
  fun countryIsError(): Boolean {
    return registerPressed && !countryValid()
  }

  /**
   * Checks if all required fields are valid for registration
   *
   * @return true if all validation passes, false otherwise
   */
  fun canRegister(): Boolean {
    return firstNameValid() && lastNameValid() && countryValid()
  }
}

/**
 * ViewModel for managing the new profile creation screen state Handles user input updates and form
 * validation
 */
class ProfileViewModel(private val repo: ProfileRepository = ProfileRepositoryProvider.repository) :
    ViewModel() {
  // Private mutable state flow for internal state management
  private val _uiState = MutableStateFlow(ProfileUIState())

  // Public immutable state flow exposed to the UI
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

  var initialized: Boolean = false

  fun initialize() {
    if (initialized) return
    viewModelScope.launch {
      // Get the first profile emission (or null if no profile exists)
      val profile = repo.getProfile().firstOrNull()

      // If a profile exists, populate the form fields with its data
      profile?.let {
        setFirstName(it.firstName)
        setLastName(it.lastName)
        setCountry(it.country)
        setGardeningSkill(it.gardeningSkill)
        setAvatar(it.avatar)
        setFavoritePlant(it.favoritePlant)
      }
      // If profile is null, the form keeps its default values from NewProfileUIState
      initialized = true
    }
  }

  /**
   * Updates the first name in the UI state
   *
   * @param firstName The new first name value
   */
  fun setFirstName(firstName: String) {
    _uiState.value = _uiState.value.copy(firstName = firstName)
  }

  /**
   * Updates the last name in the UI state
   *
   * @param lastName The new last name value
   */
  fun setLastName(lastName: String) {
    _uiState.value = _uiState.value.copy(lastName = lastName)
  }

  /**
   * Updates the gardening skill level in the UI state
   *
   * @param gardeningSkill The selected gardening skill level
   */
  fun setGardeningSkill(gardeningSkill: GardeningSkill) {
    _uiState.value = _uiState.value.copy(gardeningSkill = gardeningSkill)
  }

  /**
   * Updates the favorite plant in the UI state
   *
   * @param favoritePlant The user's favorite plant
   */
  fun setFavoritePlant(favoritePlant: String) {
    _uiState.value = _uiState.value.copy(favoritePlant = favoritePlant)
  }

  /**
   * Updates the country in the UI state with proper capitalization
   *
   * @param country The selected country name (will be capitalized)
   */
  fun setCountry(country: String) {
    // Capitalize the first letter of the country name
    val capitalizedCountry =
        country.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    _uiState.value = _uiState.value.copy(country = capitalizedCountry)
  }

  /**
   * Updates the avatar in the UI state
   *
   * @param avatar The selected avatar
   */
  fun setAvatar(avatar: Avatar) {
    _uiState.value = _uiState.value.copy(avatar = avatar)
  }

  /**
   * Updates the register button pressed state for validation display
   *
   * @param registerPressed True if the register button was pressed
   */
  fun setRegisterPressed(registerPressed: Boolean) {
    _uiState.value = _uiState.value.copy(registerPressed = registerPressed)
  }

  /** validate and save the profile in firestore through the repository. return true on success */
  fun submit(onResult: (Boolean) -> Unit) {
    // show errors if needed
    setRegisterPressed(true)
    val state = _uiState.value
    if (!state.canRegister()) {
      onResult(false)
      return
    }

    val uid = repo.getCurrentUserId()
    if (uid.isNullOrBlank()) {
      // no connected user
      onResult(false)
      return
    }

    val profile =
        Profile(
            firstName = state.firstName.trim(),
            lastName = state.lastName.trim(),
            gardeningSkill = state.gardeningSkill ?: GardeningSkill.BEGINNER,
            favoritePlant = state.favoritePlant.trim(),
            country = state.country.trim(),
            hasSignedIn = true,
            avatar = state.avatar)

    viewModelScope.launch {
      try {
        repo.saveProfile(profile)
        onResult(true)
      } catch (e: Exception) {
        // log if needed
        onResult(false)
      }
    }
  }
}
