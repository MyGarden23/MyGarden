package com.android.mygarden.ui.profile

import com.android.mygarden.model.profile.Countries
import com.android.mygarden.model.profile.GardeningSkill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NewProfileViewModel.
 *
 * These tests verify the business logic of the ViewModel without UI dependencies.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewProfileViewModelTest {

  private lateinit var viewModel: NewProfileViewModel
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    viewModel = NewProfileViewModel()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_initialStateIsDefault() = runTest {
    val initialState = viewModel.uiState.value

    assertEquals("", initialState.firstName)
    assertEquals("", initialState.lastName)
    assertEquals(null, initialState.gardeningSkill)
    assertEquals("", initialState.favoritePlant)
    assertEquals("", initialState.country)
    assertEquals(false, initialState.registerPressed)
  }

  @Test
  fun setFirstName_updatesFirstNameCorrectly() = runTest {
    viewModel.setFirstName("John")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("John", uiState.firstName)
  }

  @Test
  fun setFirstName_handlesEmptyString() = runTest {
    viewModel.setFirstName("John")
    viewModel.setFirstName("")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("", uiState.firstName)
  }

  @Test
  fun setLastName_updatesLastNameCorrectly() = runTest {
    viewModel.setLastName("Doe")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("Doe", uiState.lastName)
  }

  @Test
  fun setLastName_handlesEmptyString() = runTest {
    viewModel.setLastName("Doe")
    viewModel.setLastName("")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("", uiState.lastName)
  }

  @Test
  fun setGardeningSkill_updatesGardeningSkillCorrectly() = runTest {
    viewModel.setGardeningSkill(GardeningSkill.INTERMEDIATE)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(GardeningSkill.INTERMEDIATE, uiState.gardeningSkill)
  }

  @Test
  fun setGardeningSkill_handlesAllSkillLevels() = runTest {
    // Test all gardening skills dynamically - automatically includes any new skills added
    GardeningSkill.values().forEach { skill ->
      viewModel.setGardeningSkill(skill)
      advanceUntilIdle()

      val uiState = viewModel.uiState.value
      assertEquals(skill, uiState.gardeningSkill)
    }
  }

  @Test
  fun setFavoritePlant_updatesFavoritePlantCorrectly() = runTest {
    viewModel.setFavoritePlant("Rose")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("Rose", uiState.favoritePlant)
  }

  @Test
  fun setFavoritePlant_handlesEmptyString() = runTest {
    viewModel.setFavoritePlant("Rose")
    viewModel.setFavoritePlant("")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("", uiState.favoritePlant)
  }

  @Test
  fun setCountry_updatesCountryCorrectly() = runTest {
    viewModel.setCountry("France")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("France", uiState.country)
  }

  @Test
  fun setCountry_capitalizesFirstLetter() = runTest {
    viewModel.setCountry("france")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("France", uiState.country)
  }

  @Test
  fun setCountry_handlesAlreadyCapitalizedCountry() = runTest {
    viewModel.setCountry("United States")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("United States", uiState.country)
  }

  @Test
  fun setCountry_handlesEmptyString() = runTest {
    viewModel.setCountry("France")
    viewModel.setCountry("")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("", uiState.country)
  }

  @Test
  fun setRegisterPressed_updatesRegisterPressedCorrectly() = runTest {
    viewModel.setRegisterPressed(true)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(true, uiState.registerPressed)
  }

  @Test
  fun setRegisterPressed_canToggleState() = runTest {
    viewModel.setRegisterPressed(true)
    advanceUntilIdle()
    assertEquals(true, viewModel.uiState.value.registerPressed)

    viewModel.setRegisterPressed(false)
    advanceUntilIdle()
    assertEquals(false, viewModel.uiState.value.registerPressed)
  }

  // Validation tests
  @Test
  fun firstNameIsError_returnsFalseWhenRegisterNotPressed() = runTest {
    viewModel.setFirstName("")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.firstNameIsError())
  }

  @Test
  fun firstNameIsError_returnsTrueWhenRegisterPressedAndFirstNameEmpty() = runTest {
    viewModel.setFirstName("")
    viewModel.setRegisterPressed(true)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertTrue(uiState.firstNameIsError())
  }

  @Test
  fun firstNameIsError_returnsFalseWhenRegisterPressedAndFirstNameValid() = runTest {
    viewModel.setFirstName("John")
    viewModel.setRegisterPressed(true)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.firstNameIsError())
  }

  @Test
  fun lastNameIsError_returnsFalseWhenRegisterNotPressed() = runTest {
    viewModel.setLastName("")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.lastNameIsError())
  }

  @Test
  fun lastNameIsError_returnsTrueWhenRegisterPressedAndLastNameEmpty() = runTest {
    viewModel.setLastName("")
    viewModel.setRegisterPressed(true)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertTrue(uiState.lastNameIsError())
  }

  @Test
  fun lastNameIsError_returnsFalseWhenRegisterPressedAndLastNameValid() = runTest {
    viewModel.setLastName("Doe")
    viewModel.setRegisterPressed(true)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.lastNameIsError())
  }

  @Test
  fun countryIsError_returnsFalseWhenRegisterNotPressed() = runTest {
    viewModel.setCountry("InvalidCountry")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.countryIsError())
  }

  @Test
  fun countryIsError_returnsTrueWhenRegisterPressedAndCountryInvalid() = runTest {
    viewModel.setCountry("InvalidCountry")
    viewModel.setRegisterPressed(true)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertTrue(uiState.countryIsError())
  }

  @Test
  fun countryIsError_returnsFalseWhenRegisterPressedAndCountryValid() = runTest {
    viewModel.setCountry("France")
    viewModel.setRegisterPressed(true)
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.countryIsError())
  }

  @Test
  fun canRegister_returnsFalseWhenFieldsAreEmpty() = runTest {
    viewModel.setFirstName("")
    viewModel.setLastName("")
    viewModel.setCountry("")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.canRegister())
  }

  @Test
  fun canRegister_returnsFalseWhenFirstNameEmpty() = runTest {
    viewModel.setFirstName("")
    viewModel.setLastName("Doe")
    viewModel.setCountry("France")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.canRegister())
  }

  @Test
  fun canRegister_returnsFalseWhenLastNameEmpty() = runTest {
    viewModel.setFirstName("John")
    viewModel.setLastName("")
    viewModel.setCountry("France")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.canRegister())
  }

  @Test
  fun canRegister_returnsFalseWhenCountryInvalid() = runTest {
    viewModel.setFirstName("John")
    viewModel.setLastName("Doe")
    viewModel.setCountry("InvalidCountry")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertFalse(uiState.canRegister())
  }

  @Test
  fun canRegister_returnsTrueWhenAllRequiredFieldsValid() = runTest {
    viewModel.setFirstName("John")
    viewModel.setLastName("Doe")
    viewModel.setCountry("France")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertTrue(uiState.canRegister())
  }

  @Test
  fun canRegister_returnsTrueEvenWhenOptionalFieldsEmpty() = runTest {
    viewModel.setFirstName("John")
    viewModel.setLastName("Doe")
    viewModel.setCountry("France")
    // gardeningSkill is optional and starts as null by default
    viewModel.setFavoritePlant("")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertTrue(uiState.canRegister())
  }

  @Test
  fun multipleFieldUpdates_preservesOtherFields() = runTest {
    viewModel.setFirstName("John")
    viewModel.setLastName("Doe")
    viewModel.setGardeningSkill(GardeningSkill.BEGINNER)
    viewModel.setFavoritePlant("Rose")
    viewModel.setCountry("France")
    advanceUntilIdle()

    // Update one field and verify others remain unchanged
    viewModel.setFirstName("Jane")
    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals("Jane", uiState.firstName)
    assertEquals("Doe", uiState.lastName)
    assertEquals(GardeningSkill.BEGINNER, uiState.gardeningSkill)
    assertEquals("Rose", uiState.favoritePlant)
    assertEquals("France", uiState.country)
  }

  @Test
  fun validCountriesFromCountriesList_canRegister() = runTest {
    // Test a few random countries from the actual Countries.ALL list
    val testCountries = Countries.ALL.take(5) // Take first 5 countries from the real list

    testCountries.forEach { country ->
      viewModel.setFirstName("John")
      viewModel.setLastName("Doe")
      viewModel.setCountry(country)
      advanceUntilIdle()

      val uiState = viewModel.uiState.value
      assertTrue("Should be able to register with country: $country", uiState.canRegister())
      assertFalse("Country should not be an error: $country", uiState.countryIsError())
    }
  }

  /** Tests the setAvatar function in the NewProfileViewModel. */
  @Test
  fun setAvatar_handlesAvatarChange() = runTest {
    val firstAvatar = Avatar.A2
    val secondAvatar = Avatar.A3

    viewModel.setAvatar(firstAvatar)
    advanceUntilIdle()
    assertEquals(firstAvatar, viewModel.uiState.value.avatar)

    viewModel.setAvatar(secondAvatar)
    advanceUntilIdle()
    assertEquals(secondAvatar, viewModel.uiState.value.avatar)
  }
}
