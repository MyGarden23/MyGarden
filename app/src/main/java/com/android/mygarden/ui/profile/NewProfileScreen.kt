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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.model.Countries
import com.android.mygarden.model.profile.GardeningSkill

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
      bottomBar = {
        // Fixed bottom button for profile registration
        Button(
            onClick = {
              newProfileViewModel.setRegisterPressed(true)
              if (uiState.canRegister()) {
                onRegisterPressed
              }
            },
            modifier =
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp, vertical = 8.dp),
            colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(25.dp)) {
              Text(
                  text = "Register Profile",
                  color = MaterialTheme.colorScheme.onPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium)
            }
      }) { paddingValues ->
        // Main content layout with three sections using weight distribution
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
              // Header section (20% of screen) - Title and Avatar
              Row(modifier = Modifier.weight(0.2f)) {
                Column(
                    modifier = Modifier.weight(0.3f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly) {

                      // Screen title
                      Text(
                          text = "New Profile",
                          fontSize = 20.sp,
                          fontWeight = FontWeight.Medium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)

                      // User avatar placeholder with person icon
                      Box(
                          modifier =
                              Modifier.fillMaxWidth(0.25f) // 25% of screen width
                                  .aspectRatio(1f) // Maintain square aspect ratio
                                  .clip(CircleShape)
                                  .background(MaterialTheme.colorScheme.surfaceVariant),
                          contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.fillMaxSize(0.5f), // 50% of circle size
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                          }
                    }
              }

              // Form section (60% of screen) - Input fields
              Column(
                  modifier = Modifier.weight(0.6f), verticalArrangement = Arrangement.SpaceEvenly) {

                    // First Name field - Required field with validation
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = { newProfileViewModel.setFirstName(it) },
                        label = { Text("First Name *") },
                        placeholder = { Text("Enter your first name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.firstNameIsError(),
                        singleLine = true)

                    // Last Name field - Required field with validation
                    OutlinedTextField(
                        value = uiState.lastName,
                        onValueChange = { newProfileViewModel.setLastName(it) },
                        label = { Text("Last Name *") },
                        placeholder = { Text("Enter your last name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.lastNameIsError(),
                        singleLine = true)

                    // Gardening Experience dropdown - Read-only field with dropdown menu
                    Box {
                      OutlinedTextField(
                          value = uiState.gardeningSkill?.name ?: "",
                          onValueChange = {},
                          readOnly = true,
                          label = { Text("Experience with Plants") },
                          placeholder = { Text("Select your experience level") },
                          modifier = Modifier.fillMaxWidth(),
                          trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier =
                                    Modifier.clickable {
                                      isExperienceExpanded = !isExperienceExpanded
                                    })
                          })

                      // Dropdown menu for gardening skill selection
                      DropdownMenu(
                          expanded = isExperienceExpanded,
                          onDismissRequest = { isExperienceExpanded = false }) {
                            GardeningSkill.values().forEach { skill ->
                              DropdownMenuItem(
                                  text = { Text(skill.name) },
                                  onClick = {
                                    newProfileViewModel.setGardeningSkill(skill)
                                    isExperienceExpanded = false
                                  })
                            }
                          }
                    }

                    // Favorite Plant field - Optional field
                    OutlinedTextField(
                        value = uiState.favoritePlant,
                        onValueChange = { newProfileViewModel.setFavoritePlant(it) },
                        label = { Text("Favorite Plant") },
                        placeholder = { Text("Enter your favorite plant") },
                        modifier = Modifier.fillMaxWidth(),
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
                          modifier = Modifier.fillMaxWidth().focusRequester(countryFocusRequester),
                          isError = uiState.countryIsError(),
                          singleLine = true,
                          trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Country dropdown",
                                modifier =
                                    Modifier.clickable { isCountryExpanded = !isCountryExpanded })
                          })

                      // Searchable dropdown for country selection
                      if (isCountryExpanded) {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier.fillMaxWidth().height(250.dp).offset(y = (-266).dp),
                            colors =
                                CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface)) {
                              LazyColumn {
                                // Display number of countries found
                                item {
                                  Text(
                                      text = "${filteredCountries.size} countries found",
                                      modifier = Modifier.padding(16.dp),
                                      fontSize = 12.sp,
                                      fontStyle = FontStyle.Italic,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                // Display filtered countries (limited to 15 for performance)
                                items(filteredCountries.take(15)) { country ->
                                  Text(
                                      text = country,
                                      modifier =
                                          Modifier.fillMaxWidth()
                                              .clickable {
                                                newProfileViewModel.setCountry(country)
                                                isCountryExpanded = false
                                              }
                                              .padding(horizontal = 16.dp, vertical = 12.dp),
                                      color = MaterialTheme.colorScheme.onSurface)
                                }

                                // Show indicator for additional countries if more than 15 results
                                if (filteredCountries.size > 15) {
                                  item {
                                    Text(
                                        text =
                                            "... and ${filteredCountries.size - 15} more countries",
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                  }
                                }

                                // Display message when no countries found
                                if (filteredCountries.isEmpty()) {
                                  item {
                                    Text(
                                        text = "No countries found",
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                  }
                                }
                              }
                            }
                      }
                    }
                  }

              // Bottom spacer (10% of screen) - Pushes content up
              Spacer(modifier = Modifier.weight(0.1f))
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
  NewProfileScreen(onRegisterPressed = {})
}
