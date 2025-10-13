package com.android.mygarden.ui.profile

import androidx.lifecycle.ViewModel
import com.android.mygarden.model.Countries
import com.android.mygarden.model.profile.GardeningSkill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NewProfileUIState(
    val firstName: String = "",
    val LastName: String = "",
    val gardeningSkill: GardeningSkill? = null,
    val favoritePlant: String = "",
    val country: String = "",
    val registerPressed: Boolean = false,
) {
  private fun firstNameValid(): Boolean {
    return firstName.isNotBlank()
  }

  fun firstNameIsError(): Boolean {
    return registerPressed && !firstNameValid()
  }

  private fun lastNameValid(): Boolean {
    return LastName.isNotBlank()
  }

  fun lastNameIsError(): Boolean {
    return registerPressed && !lastNameValid()
  }

  private fun countryValid(): Boolean {
    return Countries.ALL.contains(country)
  }

  fun countryIsError(): Boolean {
    return registerPressed && !countryValid()
  }

  fun canRegister(): Boolean {
    return firstNameValid() && lastNameValid() && countryValid()
  }
}

class NewProfileViewModel() : ViewModel() {
  private val _uiState = MutableStateFlow(NewProfileUIState())
  // Public immutable state flow exposed to UI
  val uiState: StateFlow<NewProfileUIState> = _uiState.asStateFlow()

  fun setFirstName(firstName: String) {
    _uiState.value = _uiState.value.copy(firstName = firstName)
  }

  fun setLastName(lastName: String) {
    _uiState.value = _uiState.value.copy(LastName = lastName)
  }

  fun setGardeningSkill(gardeningSkill: GardeningSkill) {
    _uiState.value = _uiState.value.copy(gardeningSkill = gardeningSkill)
  }

  fun setFavoritePlant(favoritePlant: String) {
    _uiState.value = _uiState.value.copy(favoritePlant = favoritePlant)
  }

  fun setCountry(country: String) {
    _uiState.value = _uiState.value.copy(country = country)
  }

  fun setRegisterPressed(registerPressed: Boolean) {
    _uiState.value = _uiState.value.copy(registerPressed = registerPressed)
  }
}
