package com.android.mygarden.ui.profile

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.ui.navigation.TopBar
import com.android.mygarden.ui.utils.OfflineMessages
import com.android.mygarden.ui.utils.handleOfflineClick

// Layout proportions
private const val HEADER_SECTION_WEIGHT = 0.2f
private const val CONTENT_SECTION_WEIGHT = 0.3f
private const val FORM_SECTION_WEIGHT = 0.6f
private const val SPACER_SECTION_WEIGHT = 0.1f
private const val AVATAR_WIDTH_FRACTION = 0.25f

// Dimensions
private val BUTTON_HEIGHT = 56.dp
private val BUTTON_CORNER_RADIUS = 25.dp
private val DROPDOWN_HEIGHT = 150.dp
private val CARD_ELEVATION = 6.dp
private val MAX_WIDTH_AVATAR = 160.dp

// Spacing
private val HORIZONTAL_PADDING = 24.dp
private val VERTICAL_PADDING = 8.dp
private val STANDARD_PADDING = 16.dp
private val ITEM_VERTICAL_PADDING = 12.dp
private val SPACE_SAVE_BUTTON_BOTTOM_SCREEN = (-12.dp)

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
 * @param title The title to display (defaults to "New Profile")
 */
@Composable
private fun ProfileHeader(
    modifier: Modifier = Modifier,
    uiState: ProfileUIState,
    onAvatarClick: () -> Unit,
) {

  Box(modifier = modifier.fillMaxWidth()) {
    Column(
        modifier = Modifier.align(Alignment.Center).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly) {
          // Avatar placeholder with person icon
          Box(
              modifier =
                  Modifier.sizeIn(maxWidth = MAX_WIDTH_AVATAR)
                      .aspectRatio(1f)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .testTag(ProfileScreenTestTags.AVATAR)
                      .clickable { onAvatarClick() },
              contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(uiState.avatar.resId),
                    contentDescription = stringResource(R.string.avatar_picture_description),
                    modifier = Modifier.fillMaxSize())
              }
        }
  }
}

/**
 * Form section containing all profile input fields with validation.
 *
 * @param uiState Current UI state containing form field values
 * @param profileViewModel ViewModel for handling user input
 * @param modifier Modifier for customizing the layout
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ProfileForm(
    uiState: ProfileUIState,
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
  var isExperienceExpanded by remember { mutableStateOf(false) }
  var isCountryExpanded by remember { mutableStateOf(false) }
  val countryFocusRequester = remember { FocusRequester() }

  Column(modifier = modifier, verticalArrangement = Arrangement.SpaceEvenly) {
    // Required fields with validation
    OutlinedTextField(
        value = uiState.firstName,
        onValueChange = { profileViewModel.setFirstName(it) },
        label = { Text(stringResource(R.string.mandatory_first_name_label)) },
        placeholder = { Text(stringResource(R.string.mandatory_first_name_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.FIRST_NAME_FIELD),
        isError = profileViewModel.firstNameIsError(),
        singleLine = true)

    OutlinedTextField(
        value = uiState.lastName,
        onValueChange = { profileViewModel.setLastName(it) },
        label = { Text(stringResource(R.string.mandatory_last_name_label)) },
        placeholder = { Text(stringResource(R.string.mandatory_last_name_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.LAST_NAME_FIELD),
        isError = profileViewModel.lastNameIsError(),
        singleLine = true)

    OutlinedTextField(
        value = uiState.pseudo,
        onValueChange = { profileViewModel.setPseudo(it, false) },
        label = { Text(stringResource(R.string.mandatory_pseudo_label)) },
        placeholder = { Text(stringResource(R.string.mandatory_pseudo_placeholder)) },
        modifier =
            Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.PSEUDO_FIELD).onFocusChanged {
                state ->
              if (!state.isFocused) {
                profileViewModel.checkAvailabilityNow()
              }
            },
        isError = profileViewModel.pseudoIsError(),
        supportingText = {
          if (profileViewModel.pseudoIsError()) {
            Text(
                text = stringResource(R.string.error_pseudo_taken),
                color = MaterialTheme.colorScheme.error)
          }
        },
        singleLine = true)

    ExperienceDropdown(
        uiState = uiState,
        profileViewModel = profileViewModel,
        isExpanded = isExperienceExpanded,
        onExpandedChange = { isExperienceExpanded = it })

    // Optional field
    OutlinedTextField(
        value = uiState.favoritePlant,
        onValueChange = { profileViewModel.setFavoritePlant(it) },
        label = { Text(stringResource(R.string.favorite_plant_label)) },
        placeholder = { Text(stringResource(R.string.favorite_plant_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag(ProfileScreenTestTags.FAVORITE_PLANT_FIELD),
        singleLine = true)

    CountryDropdown(
        uiState = uiState,
        profileViewModel = profileViewModel,
        isExpanded = isCountryExpanded,
        onExpandedChange = { isCountryExpanded = it },
        countryFocusRequester = countryFocusRequester)
  }
}

/**
 * Dropdown for selecting gardening experience level from predefined options.
 *
 * @param uiState Current UI state
 * @param profileViewModel ViewModel for handling selection
 * @param isExpanded Whether the dropdown is currently expanded
 * @param onExpandedChange Callback for dropdown expansion state changes
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExperienceDropdown(
    uiState: ProfileUIState,
    profileViewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {

  ExposedDropdownMenuBox(
      expanded = isExpanded,
      onExpandedChange = onExpandedChange,
      modifier = Modifier.testTag(ProfileScreenTestTags.EXPERIENCE_DROPDOWN)) {
        OutlinedTextField(
            value = uiState.gardeningSkill?.name ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.experience_label)) },
            placeholder = { Text(stringResource(R.string.experience_placeholder)) },
            modifier =
                Modifier.fillMaxWidth()
                    .menuAnchor()
                    .testTag(ProfileScreenTestTags.EXPERIENCE_FIELD),
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) })

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.testTag(ProfileScreenTestTags.EXPERIENCE_DROPDOWN_MENU)) {
              GardeningSkill.values().forEach { skill ->
                DropdownMenuItem(
                    text = { Text(skill.name) },
                    onClick = {
                      profileViewModel.setGardeningSkill(skill)
                      onExpandedChange(false)
                    },
                    modifier = Modifier.testTag(ProfileScreenTestTags.getExperienceItemTag(skill)))
              }
            }
      }
}

/**
 * Searchable dropdown for country selection with real-time filtering.
 *
 * @param uiState Current UI state
 * @param profileViewModel ViewModel for handling input and selection
 * @param isExpanded Whether the dropdown is currently expanded
 * @param onExpandedChange Callback for dropdown expansion state changes
 * @param countryFocusRequester Focus requester for accessibility
 */
@Composable
private fun CountryDropdown(
    uiState: ProfileUIState,
    profileViewModel: ProfileViewModel,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    countryFocusRequester: FocusRequester
) {
  val context = LocalContext.current
  val filteredCountries =
      if (uiState.country.isBlank()) {
        context.resources.getStringArray(R.array.countries).toList()
      } else {
        context.resources.getStringArray(R.array.countries).toList().filter {
          it.contains(uiState.country, ignoreCase = true)
        }
      }

  Box(modifier = Modifier.fillMaxWidth()) {
    OutlinedTextField(
        value = uiState.country,
        onValueChange = { newText ->
          profileViewModel.setCountry(newText)
          onExpandedChange(newText.isNotEmpty())
        },
        label = { Text(stringResource(R.string.mandatory_country_label)) },
        placeholder = { Text(stringResource(R.string.mandatory_country_placeholder)) },
        modifier =
            Modifier.fillMaxWidth()
                .focusRequester(countryFocusRequester)
                .testTag(ProfileScreenTestTags.COUNTRY_FIELD),
        isError = profileViewModel.countryIsError(),
        singleLine = true,
        trailingIcon = {
          Icon(
              Icons.Default.ArrowDropDown,
              contentDescription = stringResource(R.string.mandatory_country_dropdown_description),
              modifier =
                  Modifier.clickable { onExpandedChange(!isExpanded) }
                      .testTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_ICON))
        })

    if (isExpanded) {
      CountryDropdownMenu(
          filteredCountries = filteredCountries,
          onCountrySelected = { country ->
            profileViewModel.setCountry(country)
            onExpandedChange(false)
          },
          onClose = { onExpandedChange(false) })
    }
  }
}

/**
 * Dropdown menu displaying filtered country list with search results information.
 *
 * @param filteredCountries List of countries matching the search criteria
 * @param onCountrySelected Callback when a country is selected
 * @param onClose Callback when the dropdown is closed
 */
@Composable
private fun CountryDropdownMenu(
    filteredCountries: List<String>,
    onCountrySelected: (String) -> Unit,
    onClose: () -> Unit
) {

  Card(
      elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION),
      modifier =
          Modifier.fillMaxWidth()
              .sizeIn(maxHeight = DROPDOWN_HEIGHT)
              .offset(y = (-DROPDOWN_HEIGHT))
              .testTag(ProfileScreenTestTags.COUNTRY_DROPDOWN),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        LazyColumn(modifier = Modifier.testTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU)) {
          // Search results count
          item {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = STANDARD_PADDING, vertical = VERTICAL_PADDING),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text =
                          stringResource(R.string.number_countries_found, filteredCountries.size),
                      modifier = Modifier.testTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT),
                      fontSize = CAPTION_FONT_SIZE,
                      fontStyle = FontStyle.Italic,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)

                  Icon(
                      painter = painterResource(R.drawable.x_circle),
                      contentDescription = stringResource(R.string.close_country_dropdown),
                      modifier =
                          Modifier.clickable { onClose() }
                              .padding(start = VERTICAL_PADDING)
                              .testTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_CLOSE),
                      tint = MaterialTheme.colorScheme.error)
                }
          }

          // Country list (limited for performance)
          items(filteredCountries.take(MAX_COUNTRIES_DISPLAYED)) { country ->
            Text(
                text = country,
                modifier =
                    Modifier.fillMaxWidth()
                        .clickable { onCountrySelected(country) }
                        .padding(horizontal = STANDARD_PADDING, vertical = ITEM_VERTICAL_PADDING)
                        .testTag(ProfileScreenTestTags.getCountryItemTag(country)),
                color = MaterialTheme.colorScheme.onSurface)
          }

          // Additional results indicator
          if (filteredCountries.size > MAX_COUNTRIES_DISPLAYED) {
            item {
              Text(
                  text =
                      stringResource(
                          R.string.more_countries_found,
                          filteredCountries.size - MAX_COUNTRIES_DISPLAYED),
                  modifier =
                      Modifier.padding(STANDARD_PADDING)
                          .testTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS),
                  fontSize = CAPTION_FONT_SIZE,
                  fontStyle = FontStyle.Italic,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }

          // No results message
          if (filteredCountries.isEmpty()) {
            item {
              Text(
                  text = stringResource(R.string.no_country_found),
                  modifier =
                      Modifier.padding(STANDARD_PADDING)
                          .testTag(ProfileScreenTestTags.COUNTRY_NO_RESULTS),
                  fontSize = BODY_FONT_SIZE,
                  color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
}

/**
 * Save button that validates form and triggers profile saving.
 *
 * @param uiState Current UI state for validation
 * @param profileViewModel ViewModel for triggering registration
 * @param onRegisterPressed Callback when registration is successful
 */
@Composable
private fun SaveButton(
    profileViewModel: ProfileViewModel,
    onRegisterPressed: () -> Unit,
    isOnline: Boolean = true
) {
  val context = LocalContext.current

  Button(
      onClick = {
        handleOfflineClick(
            isOnline = isOnline,
            context = context,
            offlineMessageResId = OfflineMessages.CANNOT_SAVE_PROFILE) {
              profileViewModel.setRegisterPressed(true)
              if (profileViewModel.canRegister()) {
                profileViewModel.submit(
                    { success ->
                      if (success) {
                        onRegisterPressed()
                      }
                    },
                    context)
              }
            }
      },
      modifier =
          Modifier.fillMaxWidth()
              .height(BUTTON_HEIGHT)
              .padding(horizontal = HORIZONTAL_PADDING, vertical = VERTICAL_PADDING)
              .testTag(ProfileScreenTestTags.SAVE_BUTTON),
      colors =
          if (isOnline) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          } else {
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
          },
      shape = RoundedCornerShape(BUTTON_CORNER_RADIUS)) {
        Text(
            text = stringResource(R.string.save_profile_button_text),
            color =
                if (isOnline) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
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
 * @param profileViewModel ViewModel managing form state and validation
 * @param onSavePressed Callback invoked on successful profile registration
 * @param onAvatarClick Callback when avatar is clicked
 * @param onNavBackIconClick Optional callback for navigation icon (e.g., back button). If null, no
 *   navigation icon is shown.
 * @param title The title to display in the header (defaults to "New Profile")
 */
@Composable
@SuppressLint("RememberReturnType")
@OptIn(ExperimentalMaterial3Api::class)
fun ProfileScreenBase(
    profileViewModel: ProfileViewModel = viewModel(),
    onSavePressed: () -> Unit,
    onAvatarClick: () -> Unit = {}, // put {} by default so that previous test can still run
    onNavBackIconClick: (() -> Unit)? = null,
    title: String = LocalContext.current.getString(R.string.new_profile_screen_title),
) {
  val context = LocalContext.current
  val countries = remember(context) { context.resources.getStringArray(R.array.countries).toList() }

  LaunchedEffect(Unit) { profileViewModel.setCountries(countries) }
  val uiState by profileViewModel.uiState.collectAsState()

  val isOnline by OfflineStateManager.isOnline.collectAsState(initial = true)
  Scaffold(
      modifier = Modifier.testTag(ProfileScreenTestTags.SCREEN),
      topBar = {
        // Back button displayed only when Edit Profile, not New Profile
        TopBar(
            title = title,
            hasGoBackButton = onNavBackIconClick != null,
            onGoBack =
                if (onNavBackIconClick != null) {
                  onNavBackIconClick
                } else {
                  {}
                })
      },
      bottomBar = {
        Box(Modifier.offset(y = SPACE_SAVE_BUTTON_BOTTOM_SCREEN)) {
          SaveButton(
              profileViewModel = profileViewModel,
              onRegisterPressed = onSavePressed,
              isOnline = isOnline)
        }
      }) { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = HORIZONTAL_PADDING),
            verticalArrangement = Arrangement.SpaceBetween) {
              ProfileHeader(
                  uiState = uiState,
                  onAvatarClick = onAvatarClick,
                  modifier = Modifier.weight(HEADER_SECTION_WEIGHT),
              )

              ProfileForm(
                  uiState = uiState,
                  profileViewModel = profileViewModel,
                  modifier = Modifier.weight(FORM_SECTION_WEIGHT))

              Spacer(modifier = Modifier.weight(SPACER_SECTION_WEIGHT))
            }
      }
}
