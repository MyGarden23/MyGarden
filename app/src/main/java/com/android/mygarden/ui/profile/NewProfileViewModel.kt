package com.android.mygarden.ui.profile

import androidx.lifecycle.ViewModel
import com.android.mygarden.model.profile.GardeningSkill
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NewProfileUIState(
    val firstName: String = "",
    val LastName: String = "",
    val gardeningSkill: GardeningSkill = GardeningSkill.NOVICE,
    val favoritePlant: String = "",
    val country: String = "",
)

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
}
