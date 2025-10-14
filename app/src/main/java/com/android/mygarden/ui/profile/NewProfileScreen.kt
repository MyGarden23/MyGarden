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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.model.profile.Countries
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.ui.theme.MyGardenTheme

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
 * Screen for creating a new user profile in the MyGarden app.
 *
 * This screen allows users to input their personal information including:
 * - First and last name (required fields)
 * - Gardening experience level
 * - Favorite plant (optional)
 * - Country selection with search functionality
 *
 * The screen uses Material 3 design principles with proper theming and accessibility support. Form
 * validation is handled through the ViewModel with visual error indicators.
 *
 * @param newProfileViewModel ViewModel that manages the profile creation state and validation
 * @param onRegisterPressed Callback function invoked when the registration is successful
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NewProfileScreen(
    newProfileViewModel: NewProfileViewModel = viewModel(),
    onRegisterPressed: () -> Unit
) {
  // Collect UI state from ViewModel
  val uiState by newProfileViewModel.uiState.collectAsState()

  // Focus requester for country text field (for accessibility)
  val countryFocusRequester = remember { FocusRequester() }

  // Local state for dropdown menus
  var isExperienceExpanded by remember { mutableStateOf(false) }
  var isCountryExpanded by remember { mutableStateOf(false) }

  // Filter countries based on user input for search functionality
  val filteredCountries =
      if (uiState.country.isBlank()) {
        Countries.ALL
      } else {
        Countries.ALL.filter { it.contains(uiState.country, ignoreCase = true) }
      }

  Scaffold(
      modifier = Modifier.testTag(NewProfileScreenTestTags.SCREEN),
      bottomBar = {
        // Fixed bottom button for profile registration
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
                    .testTag(NewProfileScreenTestTags.REGISTER_BUTTON),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(BUTTON_CORNER_RADIUS)) {
              Text(
                  text = "Register Profile",
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontSize = BUTTON_FONT_SIZE,
                  fontWeight = FontWeight.Medium)
            }
      }) { paddingValues ->
        // Main content layout with three sections using weight distribution
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.SpaceBetween) {
              // Header section (20% of screen) - Title and Avatar
              Row(modifier = Modifier.weight(HEADER_SECTION_WEIGHT)) {
                Column(
                    modifier = Modifier.weight(CONTENT_SECTION_WEIGHT).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly) {

                      // Screen title
                      Text(
                          text = "New Profile",
                          fontSize = TITLE_FONT_SIZE,
                          fontWeight = FontWeight.Medium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.testTag(NewProfileScreenTestTags.TITLE))

                      // User avatar placeholder with person icon
                      Box(
                          modifier =
                              Modifier.fillMaxWidth(AVATAR_WIDTH_FRACTION) // 25% of screen width
                                  .aspectRatio(1f) // Maintain square aspect ratio
                                  .clip(CircleShape)
                                  .background(MaterialTheme.colorScheme.surfaceVariant)
                                  .testTag(NewProfileScreenTestTags.AVATAR),
                          contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile Avatar",
                                modifier =
                                    Modifier.fillMaxSize(
                                        AVATAR_ICON_SIZE_FRACTION), // 50% of circle size
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                          }
                    }
              }

              // Form section (60% of screen) - Input fields
              Column(
                  modifier = Modifier.weight(FORM_SECTION_WEIGHT),
                  verticalArrangement = Arrangement.SpaceEvenly) {

                    // First Name field - Required field with validation
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = { newProfileViewModel.setFirstName(it) },
                        label = { Text("First Name *") },
                        placeholder = { Text("Enter your first name") },
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag(NewProfileScreenTestTags.FIRST_NAME_FIELD),
                        isError = uiState.firstNameIsError(),
                        singleLine = true)

                    // Last Name field - Required field with validation
                    OutlinedTextField(
                        value = uiState.lastName,
                        onValueChange = { newProfileViewModel.setLastName(it) },
                        label = { Text("Last Name *") },
                        placeholder = { Text("Enter your last name") },
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag(NewProfileScreenTestTags.LAST_NAME_FIELD),
                        isError = uiState.lastNameIsError(),
                        singleLine = true)

                    // Gardening Experience dropdown - Read-only field with dropdown menu
                    ExposedDropdownMenuBox(
                        expanded = isExperienceExpanded,
                        onExpandedChange = { isExperienceExpanded = it },
                        modifier = Modifier.testTag(NewProfileScreenTestTags.EXPERIENCE_DROPDOWN)) {
                          OutlinedTextField(
                              value = uiState.gardeningSkill?.name ?: "",
                              onValueChange = {},
                              readOnly = true,
                              label = { Text("Experience with Plants") },
                              placeholder = { Text("Select your experience level") },
                              modifier =
                                  Modifier.fillMaxWidth()
                                      .menuAnchor()
                                      .testTag(NewProfileScreenTestTags.EXPERIENCE_FIELD),
                              trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                              })

                          DropdownMenu(
                              expanded = isExperienceExpanded,
                              onDismissRequest = { isExperienceExpanded = false },
                              modifier =
                                  Modifier.testTag(
                                      NewProfileScreenTestTags.EXPERIENCE_DROPDOWN_MENU)) {
                                GardeningSkill.values().forEach { skill ->
                                  DropdownMenuItem(
                                      text = { Text(skill.name) },
                                      onClick = {
                                        newProfileViewModel.setGardeningSkill(skill)
                                        isExperienceExpanded = false
                                      },
                                      modifier =
                                          Modifier.testTag(
                                              NewProfileScreenTestTags.getExperienceItemTag(skill)))
                                }
                              }
                        }

                    // Favorite Plant field - Optional field
                    OutlinedTextField(
                        value = uiState.favoritePlant,
                        onValueChange = { newProfileViewModel.setFavoritePlant(it) },
                        label = { Text("Favorite Plant") },
                        placeholder = { Text("Enter your favorite plant") },
                        modifier =
                            Modifier.fillMaxWidth()
                                .testTag(NewProfileScreenTestTags.FAVORITE_PLANT_FIELD),
                        singleLine = true)

                    // Country selection with search functionality - Required field
                    Box(modifier = Modifier.fillMaxWidth()) {
                      OutlinedTextField(
                          value = uiState.country,
                          onValueChange = { newText ->
                            newProfileViewModel.setCountry(newText)
                            // Open dropdown when user starts typing
                            isCountryExpanded = newText.isNotEmpty()
                          },
                          label = { Text("Country *") },
                          placeholder = { Text("Search for your country") },
                          modifier =
                              Modifier.fillMaxWidth()
                                  .focusRequester(countryFocusRequester)
                                  .testTag(NewProfileScreenTestTags.COUNTRY_FIELD),
                          isError = uiState.countryIsError(),
                          singleLine = true,
                          trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Country dropdown",
                                modifier =
                                    Modifier.clickable { isCountryExpanded = !isCountryExpanded }
                                        .testTag(NewProfileScreenTestTags.COUNTRY_DROPDOWN_ICON))
                          })

                      // Searchable dropdown for country selection
                      if (isCountryExpanded) {
                        Card(
                            elevation =
                                CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(DROPDOWN_HEIGHT)
                                    .offset(y = DROPDOWN_OFFSET)
                                    .testTag(NewProfileScreenTestTags.COUNTRY_DROPDOWN),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface)) {
                              LazyColumn(
                                  modifier =
                                      Modifier.testTag(
                                          NewProfileScreenTestTags.COUNTRY_DROPDOWN_MENU)) {
                                    // Display number of countries found
                                    item {
                                      Text(
                                          text = "${filteredCountries.size} countries found",
                                          modifier =
                                              Modifier.padding(STANDARD_PADDING)
                                                  .testTag(
                                                      NewProfileScreenTestTags
                                                          .COUNTRY_RESULTS_COUNT),
                                          fontSize = CAPTION_FONT_SIZE,
                                          fontStyle = FontStyle.Italic,
                                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Display filtered countries (limited to 15 for performance)
                                    items(filteredCountries.take(MAX_COUNTRIES_DISPLAYED)) { country
                                      ->
                                      Text(
                                          text = country,
                                          modifier =
                                              Modifier.fillMaxWidth()
                                                  .clickable {
                                                    newProfileViewModel.setCountry(country)
                                                    isCountryExpanded = false
                                                  }
                                                  .padding(
                                                      horizontal = STANDARD_PADDING,
                                                      vertical = ITEM_VERTICAL_PADDING)
                                                  .testTag(
                                                      NewProfileScreenTestTags.getCountryItemTag(
                                                          country)),
                                          color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    // Show indicator for additional countries if more than 15
                                    // results
                                    if (filteredCountries.size > MAX_COUNTRIES_DISPLAYED) {
                                      item {
                                        Text(
                                            text =
                                                "... and ${filteredCountries.size - MAX_COUNTRIES_DISPLAYED} more countries",
                                            modifier =
                                                Modifier.padding(STANDARD_PADDING)
                                                    .testTag(
                                                        NewProfileScreenTestTags
                                                            .COUNTRY_MORE_RESULTS),
                                            fontSize = CAPTION_FONT_SIZE,
                                            fontStyle = FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                      }
                                    }

                                    // Display message when no countries found
                                    if (filteredCountries.isEmpty()) {
                                      item {
                                        Text(
                                            text = "No countries found",
                                            modifier =
                                                Modifier.padding(STANDARD_PADDING)
                                                    .testTag(
                                                        NewProfileScreenTestTags
                                                            .COUNTRY_NO_RESULTS),
                                            fontSize = BODY_FONT_SIZE,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                      }
                                    }
                                  }
                            }
                      }
                    }
                  }

              // Bottom spacer (10% of screen) - Pushes content up
              Spacer(modifier = Modifier.weight(SPACER_SECTION_WEIGHT))
            }
      }
}

/**
 * Preview composable for the NewProfileScreen. Used for development and design testing in Android
 * Studio.
 */
@Preview
@Composable
fun NewProfileScreenPreview() {
  MyGardenTheme { NewProfileScreen(onRegisterPressed = {}) }
}
