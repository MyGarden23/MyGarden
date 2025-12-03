package com.android.mygarden.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.profile.PseudoRepository
import com.android.mygarden.model.profile.PseudoRepositoryProvider
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
    val pseudo: String = "",
    val previousPseudo: String = "",
    val gardeningSkill: GardeningSkill? = null,
    val favoritePlant: String = "",
    val country: String = "",
    val registerPressed: Boolean = false,
    val avatar: Avatar = Avatar.A1, // By default 1st avatar
)

/**
 * ViewModel for managing the new profile creation screen state Handles user input updates and form
 * validation.
 */
class ProfileViewModel(
    private val profileRepository: ProfileRepository = ProfileRepositoryProvider.repository,
    private val pseudoRepository: PseudoRepository = PseudoRepositoryProvider.repository,
    private val achievementsRepository: AchievementsRepository =
        AchievementsRepositoryProvider.repository,
) : ViewModel() {
  // Private mutable state flow for internal state management
  private val _uiState = MutableStateFlow(ProfileUIState())

  // Public immutable state flow exposed to the UI
  val uiState: StateFlow<ProfileUIState> = _uiState.asStateFlow()

  // Private mutable state flow for internal pseudo availability management
  private val _pseudoAvailable = MutableStateFlow(true)
  val pseudoAvailable: StateFlow<Boolean> = _pseudoAvailable.asStateFlow()

  /** List of valid country names used for validation */
  private var countries: List<String> = emptyList()

  var initialized: Boolean = false

  /**
   * Initializes the profile form state.
   *
   * This function is called when the form needs to be prefilled by the current profile values or
   * defualts values if there is no current profile
   */
  fun initialize() {
    if (initialized) return
    viewModelScope.launch {
      // Get the first profile emission (or null if no profile exists)
      val profile = profileRepository.getProfile().firstOrNull()

      // If a profile exists, populate the form fields with its data
      profile?.let {
        setFirstName(it.firstName)
        setPseudo(it.pseudo, true)
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

  // ======== SETTERS to update the state =========

  /**
   * Updates the list of valid countries
   *
   * @param list The new list of valid country names
   */
  fun setCountries(list: List<String>) {
    countries = list
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
   * Updates the pseudo in the UI state
   *
   * @param pseudo The new pseudo value
   * @param previous True if we need to also set [previousPseudo] (at the moment of the init)
   */
  fun setPseudo(pseudo: String, previous: Boolean) {
    if (previous) _uiState.value = _uiState.value.copy(previousPseudo = pseudo, pseudo = pseudo)
    else _uiState.value = _uiState.value.copy(pseudo = pseudo)

    _pseudoAvailable.value = true
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

  /**
   * Public function so that the UI can check if the pseudo is available
   *
   * @return true if the pseudo is available, false otherwise
   */
  fun checkAvailabilityNow() {
    viewModelScope.launch { checkPseudoAvailability() }
  }

  /** Checks if the pseudo is available and updates the UI state accordingly */
  private suspend fun checkPseudoAvailability() {
    val pseudo = _uiState.value.pseudo.trim()

    if (pseudo.isBlank() || pseudo == _uiState.value.previousPseudo) {
      _pseudoAvailable.value = true
      return
    }
    _pseudoAvailable.value = pseudoRepository.isPseudoAvailable(pseudo)
  }

  // ======== VALIDATORS to validate all the filed of the profile =========

  /**
   * Validates that the first name is not blank
   *
   * @return true if first name is valid, false otherwise
   */
  private fun firstNameValid(): Boolean {
    return _uiState.value.firstName.isNotBlank()
  }

  /**
   * Validates that the last name is not blank
   *
   * @return true if last name is valid, false otherwise
   */
  private fun lastNameValid(): Boolean {
    return _uiState.value.lastName.isNotBlank()
  }

  /**
   * Validates that the selected country is in the list of valid countries
   *
   * @return true if country is valid, false otherwise
   */
  private fun countryValid(): Boolean {
    return countries.contains(_uiState.value.country)
  }

  /**
   * Validates that the pseudo is not blank
   *
   * @return true if pseudo is valid, false otherwise
   */
  private fun pseudoValid(): Boolean {
    return _uiState.value.pseudo.isNotBlank() && _pseudoAvailable.value
  }

  /**
   * Checks if all required fields are valid for registration
   *
   * @return true if all validation passes, false otherwise
   */
  fun canRegister(): Boolean {
    return firstNameValid() && lastNameValid() && countryValid() && pseudoValid()
  }

  // ======== IS ERROR to pass to the UI if it is an error =========

  /**
   * Determines if the first name field should show an error
   *
   * @return true if register was pressed and first name is invalid
   */
  fun firstNameIsError(): Boolean {
    return _uiState.value.registerPressed && !firstNameValid()
  }

  /**
   * Determines if the last name field should show an error
   *
   * @return true if register was pressed and last name is invalid
   */
  fun lastNameIsError(): Boolean {
    return _uiState.value.registerPressed && !lastNameValid()
  }

  /**
   * Determines if the pseudo field should show an error
   *
   * @return true if register was pressed and pseudo is invalid
   */
  fun pseudoIsError(): Boolean {
    return (!pseudoValid() && _uiState.value.registerPressed) ||
        !_pseudoAvailable.value ||
        _uiState.value.pseudo.isBlank()
  }

  /**
   * Determines if the country field should show an error
   *
   * @return true if register was pressed and country is invalid
   */
  fun countryIsError(): Boolean {
    return _uiState.value.registerPressed && !countryValid()
  }

  // ======== SUBMISSION to update firebase repositories =========

  /**
   * Validate and save the profile in firestore through the repository.
   *
   * @param onResult the callback called with as argument the success (true) or fail (false) of the
   *   saving operation on the Profile repo
   */
  fun submit(onResult: (Boolean) -> Unit) {
    // show errors if needed
    setRegisterPressed(true)
    viewModelScope.launch {
      checkPseudoAvailability()
      val state = _uiState.value

      if (!canRegister()) {
        onResult(false)
        return@launch
      }

      val uid = profileRepository.getCurrentUserId()
      if (uid.isNullOrBlank()) {
        // no connected user
        onResult(false)
        return@launch
      }

      val profile =
          Profile(
              pseudo = state.pseudo.trim(),
              firstName = state.firstName.trim(),
              lastName = state.lastName.trim(),
              gardeningSkill = state.gardeningSkill ?: GardeningSkill.BEGINNER,
              favoritePlant = state.favoritePlant.trim(),
              country = state.country.trim(),
              hasSignedIn = true,
              avatar = state.avatar)

      try {
        pseudoRepository.updatePseudoAtomic(
            oldPseudo = state.previousPseudo.ifBlank { null },
            newPseudo = state.pseudo.trim(),
            userId = uid)

        profileRepository.saveProfile(profile)

        // Set all achievements to have a value of 0 => Lvl 1 in every achievements
        achievementsRepository.initializeAchievementsForNewUser(uid)

        onResult(true)
      } catch (_: Exception) {
        // log if needed
        onResult(false)
      }
    }
  }
}
