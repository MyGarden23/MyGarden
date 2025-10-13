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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.model.Countries
import com.android.mygarden.model.profile.GardeningSkill

@Composable
fun NewProfileScreen(
    newProfileViewModel: NewProfileViewModel = viewModel(),
    onRegisterPressed: () -> Unit
) {
  val uiState by newProfileViewModel.uiState.collectAsState()

  val countryFocusRequester = remember { FocusRequester() }

  var isExperienceExpanded by remember { mutableStateOf(false) }
  var isCountryExpanded by remember { mutableStateOf(false) }
  var countryTextFieldValue by rememberSaveable { mutableStateOf("") }

  // Logique de filtrage
  val filteredCountries =
      if (uiState.country.isBlank()) {
        Countries.ALL
      } else {
        Countries.ALL.filter { it.contains(uiState.country, ignoreCase = true) }
      }

  Scaffold(
      bottomBar = {
        // Register Button en tant que bottom bar
        Button(
            onClick = {
              newProfileViewModel.setRegisterPressed(true)
              if (uiState.canRegister()) {
                onRegisterPressed
              }
            },
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
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween // Distribution équitable
            ) {
              // Section supérieure - Titre + Avatar
              Row(modifier = Modifier.weight(0.2f)) {
                Column(
                    modifier = Modifier.weight(0.3f).fillMaxHeight(), // 30% de l'écran
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly) {
                      // Titre
                      Text(
                          text = "New Profile",
                          fontSize = 20.sp,
                          fontWeight = FontWeight.Medium,
                          color = Color.Gray)

                      // Avatar
                      Box(
                          modifier =
                              Modifier.fillMaxWidth(0.25f) // 25% de la largeur de l'écran
                                  .aspectRatio(1f) // Garde un ratio carré
                                  .clip(CircleShape)
                                  .background(Color.Gray),
                          contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.fillMaxSize(0.5f), // 50% de la taille du cercle
                                tint = Color.White)
                          }
                    }
              }

              // Section du milieu - Formulaire
              Column(
                  modifier = Modifier.weight(0.6f), // 60% de l'écran
                  verticalArrangement = Arrangement.SpaceEvenly) {
                    // First Name
                    OutlinedTextField(
                        value = uiState.firstName,
                        onValueChange = { newProfileViewModel.setFirstName(it) },
                        label = { Text("First Name *") },
                        placeholder = { Text("Enter your first name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.firstNameIsError(),
                        singleLine = true)

                    // Last Name
                    OutlinedTextField(
                        value = uiState.LastName,
                        onValueChange = { newProfileViewModel.setLastName(it) },
                        label = { Text("Last Name *") },
                        placeholder = { Text("Enter your last name") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.lastNameIsError(),
                        singleLine = true)

                    // Experience (dropdown)
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
                        label = { Text("Favorite Plant") },
                        placeholder = { Text("Enter your favorite plant") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true)

                    // Country (searchable dropdown)
                    Box(modifier = Modifier.fillMaxWidth()) {
                      OutlinedTextField(
                          value = uiState.country,
                          onValueChange = { newText ->
                            newProfileViewModel.setCountry(newText)
                            // Ouvrir le dropdown quand on tape du texte
                            if (newText.isNotEmpty()) {
                              isCountryExpanded = true
                            } else {
                              isCountryExpanded = false
                            }
                          },
                          label = { Text("Country *") },
                          placeholder = { Text("Search for your country") },
                          modifier = Modifier.fillMaxWidth().focusRequester(countryFocusRequester),
                          isError = uiState.countryIsError(),
                          singleLine = true,
                          trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown des pays",
                                modifier =
                                    Modifier.clickable { isCountryExpanded = !isCountryExpanded })
                          })

                      // Utiliser un if au lieu d'une condition dans expanded pour éviter les
                      // problèmes de focus
                      if (isCountryExpanded) {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier =
                                Modifier.fillMaxWidth().height(250.dp).offset(y = (-266).dp)) {
                              LazyColumn {
                                // Afficher le nombre de pays trouvés
                                item {
                                  Text(
                                      text = "${filteredCountries.size} pays trouvés",
                                      modifier = Modifier.padding(16.dp),
                                      fontSize = 12.sp,
                                      fontStyle = FontStyle.Italic,
                                      color = Color.Gray)
                                }

                                // Afficher les pays (jusqu'à 15)
                                items(filteredCountries.take(15)) { country ->
                                  Text(
                                      text = country,
                                      modifier =
                                          Modifier.fillMaxWidth()
                                              .clickable {
                                                newProfileViewModel.setCountry(country)
                                                isCountryExpanded = false
                                              }
                                              .padding(horizontal = 16.dp, vertical = 12.dp))
                                }

                                // Si il y a plus de 15 résultats, afficher un indicateur
                                if (filteredCountries.size > 15) {
                                  item {
                                    Text(
                                        text = "... et ${filteredCountries.size - 15} autres pays",
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic,
                                        color = Color.Gray)
                                  }
                                }

                                // Message si aucun pays trouvé
                                if (filteredCountries.isEmpty()) {
                                  item {
                                    Text(
                                        text = "Aucun pays trouvé",
                                        modifier = Modifier.padding(16.dp),
                                        fontSize = 14.sp,
                                        color = Color.Gray)
                                  }
                                }
                              }
                            }
                      }
                    }
                  }

              // Section inférieure - Spacer pour pousser le contenu vers le haut
              Spacer(modifier = Modifier.weight(0.1f)) // 10% restant
        }
      }
}

@Preview
@Composable
fun NewProfileScreenPreview() {
  NewProfileScreen(onRegisterPressed = {})
}
