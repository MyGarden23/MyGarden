package com.android.mygarden.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.model.profile.Countries_
import com.android.mygarden.model.profile.GardeningSkill

// Layout proportions
private const val HEADER_SECTION_WEIGHT = 0.2f
private const val CONTENT_SECTION_WEIGHT = 0.3f
private const val FORM_SECTION_WEIGHT = 0.6f
private const val SPACER_SECTION_WEIGHT = 0.1f
private const val AVATAR_WIDTH_FRACTION = 0.25f
private const val AVATAR_ICON_SIZE_FRACTION = 0.5f

// Dimensions
private val BUTTON_HEIGHT = 56.dp
private val BUTTON_CORNER_RADIUS = 25.dp
private val DROPDOWN_HEIGHT = 250.dp
private val DROPDOWN_OFFSET = (-266).dp
private val CARD_ELEVATION = 6.dp

// Spacing
private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 8.dp
private val STANDARD_PADDING = 16.dp
private val ITEM_VERTICAL_PADDING = 12.dp

// Typography
private val TITLE_FONT_SIZE = 20.sp
private val BUTTON_FONT_SIZE = 16.sp
private val CAPTION_FONT_SIZE = 12.sp
private val BODY_FONT_SIZE = 14.sp

// Countries logic
private const val MAX_COUNTRIES_DISPLAYED = 15

/**
 * Header section displaying the profile creation title and user avatar placeholder.
 *
 * @param modifier Modifier for customizing the layout
 */
@Composable
private fun ProfileHeader(modifier: Modifier = Modifier) {
  Row(modifier = modifier) {
    Column(
        modifier = Modifier.weight(CONTENT_SECTION_WEIGHT).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly) {
          Text(
              text = "New Profile",
              fontSize = TITLE_FONT_SIZE,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.testTag(NewProfileScreenTestTags_.TITLE))

          // Avatar placeholder with person icon
          Box(
              modifier =
                  Modifier.fillMaxWidth(AVATAR_WIDTH_FRACTION)
                      .aspectRatio(1f)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .testTag(NewProfileScreenTestTags_.AVATAR),
              contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Profile Avatar",
                    modifier = Modifier.fillMaxSize(AVATAR_ICON_SIZE_FRACTION),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
              }
        }
  }
}

/**
 * Form section containing all profile input fields with validation.
 *
 * @param uiState Current UI state containing form field values
 * @param newProfileViewModel ViewModel for handling user input
 * @param modifier Modifier for customizing the layout
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileForm(
    uiState: NewProfileUIState,
    newProfileViewModel: NewProfileViewModel,
    modifier: Modifier = Modifier
) {
  var isExperienceExpanded by remember { mutableStateOf(false) }
  var isCountryExpanded by remember { mutableStateOf(false) }
  val countryFocusRequester = remember { FocusRequester() }

  Column(modifier = modifier, verticalArrangement = Arrangement.SpaceEvenly) {
    // Required fields with validation
    OutlinedTextField(
        value = uiState.firstName,
        onValueChange = { newProfileViewModel.setFirstName(it) },
        label = { Text("First Name *") },
        placeholder = { Text("Enter your first name") },
        modifier = Modifier.fillMaxWidth().testTag(NewProfileScreenTestTags_.FIRST_NAME_FIELD),
        isError = uiState.firstNameIsError(),
        singleLine = true)

    OutlinedTextField(
        value = uiState.lastName,
        onValueChange = { newProfileViewModel.setLastName(it) },
        label = { Text("Last Name *") },
        placeholder = { Text("Enter your last name") },
        modifier = Modifier.fillMaxWidth().testTag(NewProfileScreenTestTags_.LAST_NAME_FIELD),
        isError = uiState.lastNameIsError(),
        singleLine = true)

    ExperienceDropdown(
        uiState = uiState,
        newProfileViewModel = newProfileViewModel,
        isExpanded = isExperienceExpanded,
        onExpandedChange = { isExperienceExpanded = it })

    // Optional field
    OutlinedTextField(
        value = uiState.favoritePlant,
        onValueChange = { newProfileViewModel.setFavoritePlant(it) },
        label = { Text("Favorite Plant") },
        placeholder = { Text("Enter your favorite plant") },
        modifier = Modifier.fillMaxWidth().testTag(NewProfileScreenTestTags_.FAVORITE_PLANT_FIELD),
        singleLine = true)

    CountryDropdown(
        uiState = uiState,
        newProfileViewModel = newProfileViewModel,
        isExpanded = isCountryExpanded,
        onExpandedChange = { isCountryExpanded = it },
        countryFocusRequester = countryFocusRequester)
  }
}

/**
 * Dropdown for selecting gardening experience level from predefined options.
 *
 * @param uiState Current UI state
 * @param newProfileViewModel ViewModel for handling selection
 * @param isExpanded Whether the dropdown is currently expanded
 * @param onExpandedChange Callback for dropdown expansion state changes
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExperienceDropdown(
    uiState: NewProfileUIState,
    newProfileViewModel: NewProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
  ExposedDropdownMenuBox(
      expanded = isExpanded,
      onExpandedChange = onExpandedChange,
      modifier = Modifier.testTag(NewProfileScreenTestTags_.EXPERIENCE_DROPDOWN)) {
        OutlinedTextField(
            value = uiState.gardeningSkill?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Experience with Plants") },
            placeholder = { Text("Select your experience level") },
            modifier =
                Modifier.fillMaxWidth()
                    .menuAnchor()
                    .testTag(NewProfileScreenTestTags_.EXPERIENCE_FIELD),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) })

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.testTag(NewProfileScreenTestTags_.EXPERIENCE_DROPDOWN_MENU)) {
              GardeningSkill.values().forEach { skill ->
                DropdownMenuItem(
                    text = { Text(skill.name) },
                    onClick = {
                      newProfileViewModel.setGardeningSkill(skill)
                      onExpandedChange(false)
                    },
                    modifier =
                        Modifier.testTag(NewProfileScreenTestTags_.getExperienceItemTag(skill)))
              }
            }
      }
}

/**
 * Searchable dropdown for country selection with real-time filtering.
 *
 * @param uiState Current UI state
 * @param newProfileViewModel ViewModel for handling input and selection
 * @param isExpanded Whether the dropdown is currently expanded
 * @param onExpandedChange Callback for dropdown expansion state changes
 * @param countryFocusRequester Focus requester for accessibility
 */
@Composable
private fun CountryDropdown(
    uiState: NewProfileUIState,
    newProfileViewModel: NewProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    countryFocusRequester: FocusRequester
) {
  val filteredCountries =
      if (uiState.country.isBlank()) {
        Countries_.ALL
      } else {
        Countries_.ALL.filter { it.contains(uiState.country, ignoreCase = true) }
      }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = uiState.country,
        onValueChange = { newText ->
          newProfileViewModel.setCountry(newText)
          onExpandedChange(newText.isNotEmpty())
        },
        label = { Text("Country *") },
        placeholder = { Text("Search for your country") },
        modifier =
            Modifier.fillMaxWidth()
                .focusRequester(countryFocusRequester)
                .testTag(NewProfileScreenTestTags_.COUNTRY_FIELD),
        isError = uiState.countryIsError(),
        singleLine = true,
        trailingIcon = {
          Icon(
              Icons.Default.ArrowDropDown,
              contentDescription = "Country dropdown",
              modifier =
                  Modifier.clickable { onExpandedChange(!isExpanded) }
                      .testTag(NewProfileScreenTestTags_.COUNTRY_DROPDOWN_ICON))
        })

    if (isExpanded) {
      CountryDropdownMenu(
          filteredCountries = filteredCountries,
          onCountrySelected = { country ->
            newProfileViewModel.setCountry(country)
            onExpandedChange(false)
          })
    }
  }
}

/**
 * Dropdown menu displaying filtered country list with search results information.
 *
 * @param filteredCountries List of countries matching the search criteria
 * @param onCountrySelected Callback when a country is selected
 */
@Composable
private fun CountryDropdownMenu(
    filteredCountries: List<String>,
    onCountrySelected: (String) -> Unit
) {
  Card(
      elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
      modifier =
          Modifier.fillMaxWidth()
              .height(DROPDOWN_HEIGHT)
              .offset(y = DROPDOWN_OFFSET)
              .testTag(NewProfileScreenTestTags_.COUNTRY_DROPDOWN),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        LazyColumn(modifier = Modifier.testTag(NewProfileScreenTestTags_.COUNTRY_DROPDOWN_MENU)) {
          // Search results count
          item {
            Text(
                text = "${filteredCountries.size} countries found",
                modifier =
                    Modifier.padding(STANDARD_PADDING)
                        .testTag(NewProfileScreenTestTags_.COUNTRY_RESULTS_COUNT),
                fontSize = CAPTION_FONT_SIZE,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }

          // Country list (limited for performance)
          items(filteredCountries.take(MAX_COUNTRIES_DISPLAYED)) { country ->
            Text(
                text = country,
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable { onCountrySelected(country) }
                        .padding(horizontal = STANDARD_PADDING, vertical = ITEM_VERTICAL_PADDING)
                        .testTag(NewProfileScreenTestTags_.getCountryItemTag(country)),
                color = MaterialTheme.colorScheme.onSurface)
          }

          // Additional results indicator
          if (filteredCountries.size > MAX_COUNTRIES_DISPLAYED) {
            item {
              Text(
                  text =
                      "... and ${filteredCountries.size - MAX_COUNTRIES_DISPLAYED} more countries",
                  modifier =
                      Modifier.padding(STANDARD_PADDING)
                          .testTag(NewProfileScreenTestTags_.COUNTRY_MORE_RESULTS),
                  fontSize = CAPTION_FONT_SIZE,
                  fontStyle = FontStyle.Italic,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          // No results message
          if (filteredCountries.isEmpty()) {
            item {
              Text(
                  text = "No countries found",
                  modifier =
                      Modifier.padding(STANDARD_PADDING)
                          .testTag(NewProfileScreenTestTags_.COUNTRY_NO_RESULTS),
                  fontSize = BODY_FONT_SIZE,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
}

/**
 * Registration button that validates form and triggers profile creation.
 *
 * @param uiState Current UI state for validation
 * @param newProfileViewModel ViewModel for triggering registration
 * @param onRegisterPressed Callback when registration is successful
 */
@Composable
private fun RegisterButton(
    uiState: NewProfileUIState,
    newProfileViewModel: NewProfileViewModel,
    onRegisterPressed: () -> Unit
) {
  Button(
      onClick = {
        newProfileViewModel.setRegisterPressed(true)
        if (uiState.canRegister()) {
          onRegisterPressed()
        }
      },
      modifier =
          Modifier.fillMaxWidth()
              .height(BUTTON_HEIGHT)
              .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING)
              .testTag(NewProfileScreenTestTags_.REGISTER_BUTTON),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
      shape = RoundedCornerShape(BUTTON_CORNER_RADIUS)) {
        Text(
            text = "Register Profile",
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = BUTTON_FONT_SIZE,
            fontWeight = FontWeight.Medium)
      }
}

/**
 * Main screen for creating a new user profile.
 *
 * Features:
 * - Required fields: First name, last name, country (with validation)
 * - Optional fields: Gardening experience, favorite plant
 * - Searchable country dropdown with real-time filtering
 * - Material 3 theming and accessibility support
 *
 * @param newProfileViewModel ViewModel managing form state and validation
 * @param onRegisterPressed Callback invoked on successful profile registration
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NewProfileScreen(
    newProfileViewModel: NewProfileViewModel = viewModel(),
    onRegisterPressed: () -> Unit
) {
  val uiState by newProfileViewModel.uiState.collectAsState()

  Scaffold(
      modifier = Modifier.testTag(NewProfileScreenTestTags_.SCREEN),
      bottomBar = {
        RegisterButton(
            uiState = uiState,
            newProfileViewModel = newProfileViewModel,
            onRegisterPressed = onRegisterPressed)
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.SpaceBetween) {
              ProfileHeader(modifier = Modifier.weight(HEADER_SECTION_WEIGHT))

              ProfileForm(
                  uiState = uiState,
                  newProfileViewModel = newProfileViewModel,
                  modifier = Modifier.weight(FORM_SECTION_WEIGHT))

              Spacer(modifier = Modifier.weight(SPACER_SECTION_WEIGHT))
            }
      }
}
