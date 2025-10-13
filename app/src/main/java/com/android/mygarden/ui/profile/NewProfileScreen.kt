package com.android.mygarden.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.ui.theme.MyGardenTheme

// Liste des pays
private val countries =
    listOf(
        "Afghanistan",
        "Albania",
        "Algeria",
        "Argentina",
        "Australia",
        "Austria",
        "Bangladesh",
        "Belgium",
        "Brazil",
        "Canada",
        "Chile",
        "China",
        "Colombia",
        "Denmark",
        "Egypt",
        "Finland",
        "France",
        "Germany",
        "Greece",
        "India",
        "Indonesia",
        "Iran",
        "Iraq",
        "Ireland",
        "Israel",
        "Italy",
        "Japan",
        "Jordan",
        "Kazakhstan",
        "Kenya",
        "Malaysia",
        "Mexico",
        "Morocco",
        "Netherlands",
        "New Zealand",
        "Norway",
        "Pakistan",
        "Peru",
        "Philippines",
        "Poland",
        "Portugal",
        "Romania",
        "Russia",
        "Saudi Arabia",
        "Singapore",
        "South Africa",
        "South Korea",
        "Spain",
        "Sweden",
        "Switzerland",
        "Thailand",
        "Turkey",
        "Ukraine",
        "United Kingdom",
        "United States",
        "Vietnam")

@Composable
fun NewProfileScreen(
    newProfileViewModel: NewProfileViewModel = viewModel(),
    onRegisterPressed: () -> Unit
) {
  val uiState by newProfileViewModel.uiState.collectAsState()
  val focusManager = LocalFocusManager.current
  val countryFocusRequester = remember { FocusRequester() }

  var isExperienceExpanded by remember { mutableStateOf(false) }
  var isCountryExpanded by remember { mutableStateOf(false) }
  var countryTextFieldValue by remember { mutableStateOf("") }

  // Logique de filtrage
  val filteredCountries =
      if (countryTextFieldValue.isEmpty()) {
        countries
      } else {
        countries.filter { it.contains(countryTextFieldValue, ignoreCase = true) }
      }

  Scaffold(
      bottomBar = {
        // Register Button en tant que bottom bar
        Button(
            onClick = onRegisterPressed,
            modifier =
                Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 24.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8BC34A)),
            shape = RoundedCornerShape(25.dp)) {
              Text(
                  text = "Register Profile",
                  color = Color.White,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium)
            }
      }) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
              // Titre
              Text(
                  text = "New Profile",
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Medium,
                  color = Color.Gray,
                  modifier = Modifier.padding(bottom = 24.dp))

              // Avatar
              Box(
                  modifier = Modifier.size(100.dp).clip(CircleShape).background(Color.Gray),
                  contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Profile Avatar",
                        modifier = Modifier.size(50.dp),
                        tint = Color.White)
                  }

              Spacer(modifier = Modifier.height(24.dp))

              // First Name
              OutlinedTextField(
                  value = uiState.firstName,
                  onValueChange = { newProfileViewModel.setFirstName(it) },
                  label = {
                    Text(
                        "FIRST NAME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray)
                  },
                  placeholder = { Text("Text field data", color = Color.LightGray) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color.Gray, unfocusedBorderColor = Color.LightGray),
                  shape = RoundedCornerShape(8.dp))

              // Last Name
              OutlinedTextField(
                  value = uiState.LastName,
                  onValueChange = { newProfileViewModel.setLastName(it) },
                  label = {
                    Text(
                        "LAST NAME",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray)
                  },
                  placeholder = { Text("Text field data", color = Color.LightGray) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color.Gray, unfocusedBorderColor = Color.LightGray),
                  shape = RoundedCornerShape(8.dp))

              // Experience (dropdown)
              Box {
                OutlinedTextField(
                    value = uiState.gardeningSkill.name,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                      Text(
                          "EXPERIENCE WITH PLANTS",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Medium,
                          color = Color.Gray)
                    },
                    placeholder = { Text("Text field data", color = Color.LightGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    trailingIcon = {
                      Icon(
                          Icons.Default.ArrowDropDown,
                          contentDescription = null,
                          modifier =
                              Modifier.clickable { isExperienceExpanded = !isExperienceExpanded })
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray),
                    shape = RoundedCornerShape(8.dp))

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

              // Favorite Plant
              OutlinedTextField(
                  value = uiState.favoritePlant,
                  onValueChange = { newProfileViewModel.setFavoritePlant(it) },
                  label = {
                    Text(
                        "FAVORITE PLANT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray)
                  },
                  placeholder = { Text("Text field data", color = Color.LightGray) },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Color.Gray, unfocusedBorderColor = Color.LightGray),
                  shape = RoundedCornerShape(8.dp))

              // Country (searchable dropdown)
              Box {
                OutlinedTextField(
                    value = countryTextFieldValue,
                    onValueChange = { newText ->
                      countryTextFieldValue = newText
                      // Ouvrir le dropdown quand on tape du texte, mais sans voler le focus
                      if (newText.isNotEmpty()) {
                        isCountryExpanded = true
                      } else {
                        isCountryExpanded = false
                      }
                    },
                    label = {
                      Text(
                          "PAYS",
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Medium,
                          color = Color.Gray)
                    },
                    placeholder = { Text("Rechercher un pays", color = Color.LightGray) },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .focusRequester(countryFocusRequester),
                    trailingIcon = {
                      Icon(
                          Icons.Default.ArrowDropDown,
                          contentDescription = null,
                          modifier = Modifier.clickable { isCountryExpanded = !isCountryExpanded })
                    },
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.LightGray),
                    shape = RoundedCornerShape(8.dp))

                // Utiliser un if au lieu d'une condition dans expanded pour éviter les problèmes de
                // focus
                if (isCountryExpanded) {
                  Card(
                      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                      modifier = Modifier.fillMaxWidth().height(300.dp)) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                          if (filteredCountries.isEmpty()) {
                            item {
                              Text(
                                  text = "Aucun pays trouvé",
                                  fontSize = 12.sp,
                                  color = Color.Gray,
                                  fontStyle = FontStyle.Italic,
                                  modifier = Modifier.padding(16.dp))
                            }
                          } else {
                            // Afficher un indicateur du nombre de résultats si on filtre
                            if (countryTextFieldValue.isNotEmpty()) {
                              item {
                                Text(
                                    text = "${filteredCountries.size} pays trouvés",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(12.dp))
                              }
                            }
                            items(filteredCountries.take(15)) { country ->
                              Text(
                                  text = country,
                                  modifier =
                                      Modifier.fillMaxWidth()
                                          .clickable {
                                            newProfileViewModel.setCountry(country)
                                            countryTextFieldValue = country
                                            isCountryExpanded = false
                                          }
                                          .padding(horizontal = 16.dp, vertical = 12.dp),
                                  fontSize = 14.sp,
                                  color = Color.Black)
                            }
                            // Si il y a plus de 15 résultats, afficher un indicateur
                            if (filteredCountries.size > 15) {
                              item {
                                Text(
                                    text = "... et ${filteredCountries.size - 15} autres pays",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(12.dp))
                              }
                            }
                          }
                        }
                      }
                }
              }

              // Utiliser le poids pour distribuer l'espace restant
              Spacer(modifier = Modifier.weight(1f))
            }
      }
}

@Preview
@Composable
fun NewProfileScreenPreview() {
  MyGardenTheme { NewProfileScreen(onRegisterPressed = {}) }
}
