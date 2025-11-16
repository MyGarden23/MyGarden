package com.android.mygarden.ui.plantinfos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.mygarden.R
import com.android.mygarden.model.plant.Plant

/** Test tags for PlantInfoScreen components */
object PlantInfoScreenTestTags {
  const val SCREEN = "plant_info_screen"
  const val PLANT_IMAGE = "plant_image"
  const val BACK_BUTTON = "back_button"
  const val PLANT_NAME = "plant_name"
  const val PLANT_LATIN_NAME = "plant_latin_name"
  const val TAB_ROW = "tab_row"
  const val DESCRIPTION_TAB = "description_tab"
  const val HEALTH_TAB = "health_tab"
  const val LOCATION_TAB = "location_tab"
  const val CONTENT_CONTAINER = "content_container"
  const val DESCRIPTION_TEXT = "description_text"
  const val HEALTH_STATUS_DESCRIPTION = "health_status_description"
  const val HEALTH_STATUS = "health_status"
  const val LOCATION_TEXT = "location_text"
  const val LIGHT_EXPOSURE_TEXT = "light_exposure_text"
  const val WATERING_FREQUENCY = "watering_frequency"
  const val NEXT_BUTTON = "next_button"
  const val NEXT_BUTTON_LOADING = "next_button_loading"

  const val TIPS_BUTTON = "tips_button"
  const val TIPS_DIALOG = "tips_dialog"
  const val TIPS_TEXT = "tips_text"
  const val TIPS_CLOSE_BUTTON = "tips_close_button"
}

// Padding constants for PlantInfosScreen
private val PLANT_NAME_SECTION_HORIZONTAL_PADDING = 20.dp
private val PLANT_NAME_SECTION_VERTICAL_PADDING = 16.dp

/**
 * Screen displaying detailed information about a plant.
 *
 * Features:
 * - Plant image placeholder
 * - Plant name and latin name
 * - Tabbed interface for Description and Health information
 * - Save button to add the plant to user's garden
 *
 * @param plant The plant to display information for
 * @param plantInfoViewModel ViewModel managing the UI state
 * @param onBackPressed Callback when the back button is pressed
 * @param onNextPlant Callback when the Save Plant button is clicked, receives the plant ID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantInfosScreen(
    plant: Plant,
    plantInfoViewModel: PlantInfoViewModel = viewModel(),
    onBackPressed: () -> Unit,
    onNextPlant: (String) -> Unit = {}
) {
  val context = LocalContext.current
  // Observe UI state from ViewModel
  val uiState by plantInfoViewModel.uiState.collectAsState()

  // Remember scroll states for each tab separately
  // This ensures scroll position is maintained when switching between tabs
  val descriptionScrollState = rememberScrollState()
  val healthScrollState = rememberScrollState()

  // Initialize UI state when plant changes
  LaunchedEffect(plant) {
    val loadingText = context.getString(R.string.loading_plant_infos)
    plantInfoViewModel.initializeUIState(plant, loadingText)
  }

  Scaffold(
      modifier = Modifier.testTag(PlantInfoScreenTestTags.SCREEN),
      containerColor = MaterialTheme.colorScheme.background,
      bottomBar = {
        SavePlantBottomBar(
            uiState = uiState,
            onSavePlant = {
              val plantToSave = uiState.savePlant()
              plantInfoViewModel.savePlant(
                  plantToSave,
                  onPlantSaved = { plantId ->
                    plantInfoViewModel.resetUIState()
                    onNextPlant(plantId)
                  })
            })
      }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // --- Plant Image Header ---
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(280.dp)
                      .background(MaterialTheme.colorScheme.primary)
                      .testTag(PlantInfoScreenTestTags.PLANT_IMAGE)) {
                // Placeholder for plant image
                Box(modifier = Modifier.fillMaxSize()) {
                  AsyncImage(
                      model = ImageRequest.Builder(context).data(plant.image ?: "").build(),
                      contentDescription = context.getString(R.string.image_plant_description),
                      modifier =
                          Modifier.fillMaxWidth()
                              .background(MaterialTheme.colorScheme.surfaceVariant),
                      contentScale = ContentScale.Crop)
                }

                // Back button overlaid on top-left corner of image
                IconButton(
                    onClick = {
                      plantInfoViewModel.resetUIState()
                      onBackPressed()
                    },
                    enabled = !uiState.isSaving,
                    modifier =
                        Modifier.align(Alignment.TopStart)
                            .padding(8.dp)
                            .testTag(PlantInfoScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                          contentDescription = context.getString(R.string.back_description),
                          tint = MaterialTheme.colorScheme.onPrimary)
                    }
              }

          // --- Name, Latin Name and Tips Section ---
          Row(
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(
                          horizontal = PLANT_NAME_SECTION_HORIZONTAL_PADDING,
                          vertical = PLANT_NAME_SECTION_VERTICAL_PADDING),
              verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                  // Common name (e.g., "Rose")
                  Text(
                      text = uiState.name,
                      modifier = Modifier.testTag(PlantInfoScreenTestTags.PLANT_NAME),
                      fontSize = 28.sp,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onBackground)
                  // Scientific name (e.g., "Rosa rubiginosa")
                  Text(
                      text = uiState.latinName,
                      fontSize = 16.sp,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.testTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME))
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Tips button
                Button(
                    onClick = {
                      plantInfoViewModel.showCareTips(uiState.latinName, uiState.healthStatus)
                    },
                    modifier = Modifier.testTag(PlantInfoScreenTestTags.TIPS_BUTTON)) {
                      Text(text = stringResource(id = R.string.tips_button_label))
                    }
              }

          // --- Tab Row for Description/Health/Location ---
          ModulableTabRow(
              uiState = uiState,
              plantInfoViewModel = plantInfoViewModel,
              modifier = Modifier.testTag(PlantInfoScreenTestTags.TAB_ROW))

          // --- Scrollable Content for Active Tab ---
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .weight(1f) // Takes remaining space between tabs and bottom button
                      .background(MaterialTheme.colorScheme.background)
                      .testTag(PlantInfoScreenTestTags.CONTENT_CONTAINER)) {
                Column(
                    modifier =
                        Modifier.fillMaxSize()
                            // Use different scroll state for each tab to preserve scroll position
                            .verticalScroll(
                                if (uiState.selectedTab == SelectedPlantInfoTab.DESCRIPTION)
                                    descriptionScrollState
                                else healthScrollState)
                            .padding(20.dp)) {
                      when (uiState.selectedTab) {
                        // --- Description Tab Content ---
                        SelectedPlantInfoTab.DESCRIPTION -> {
                          Text(
                              text = uiState.description,
                              fontSize = 14.sp,
                              color = MaterialTheme.colorScheme.onBackground,
                              lineHeight = 20.sp,
                              modifier = Modifier.testTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT))
                        }

                        // --- Health Tab Content ---
                        SelectedPlantInfoTab.HEALTH_STATUS -> {
                          // Health status description
                          Text(
                              text = uiState.healthStatusDescription,
                              fontSize = 14.sp,
                              color = MaterialTheme.colorScheme.onBackground,
                              lineHeight = 20.sp,
                              modifier =
                                  Modifier.testTag(
                                      PlantInfoScreenTestTags.HEALTH_STATUS_DESCRIPTION))
                          Spacer(modifier = Modifier.height(16.dp))

                          // Current health status with emoji
                          Text(
                              // text = "Status: ${uiState.healthStatus.description}"
                              text =
                                  stringResource(
                                      R.string.status_label,
                                      stringResource(id = uiState.healthStatus.descriptionRes)),
                              fontSize = 16.sp,
                              fontWeight = FontWeight.Medium,
                              color = MaterialTheme.colorScheme.onBackground,
                              modifier = Modifier.testTag(PlantInfoScreenTestTags.HEALTH_STATUS))
                          Spacer(modifier = Modifier.height(8.dp))

                          // Watering frequency information
                          Text(
                              text =
                                  context.getString(
                                      R.string.watering_frequency, uiState.wateringFrequency),
                              fontSize = 14.sp,
                              color = MaterialTheme.colorScheme.onBackground,
                              modifier =
                                  Modifier.testTag(PlantInfoScreenTestTags.WATERING_FREQUENCY))
                        }
                        // --- Location Tab Content ---
                        SelectedPlantInfoTab.LOCATION -> {
                          // Location text, INDOOR or OUTDOOR
                          Text(
                              text = uiState.location.name,
                              fontSize = 16.sp,
                              fontWeight = FontWeight.Medium,
                              color = MaterialTheme.colorScheme.onBackground,
                              lineHeight = 20.sp,
                              modifier = Modifier.testTag(PlantInfoScreenTestTags.LOCATION_TEXT))

                          Spacer(modifier = Modifier.height(8.dp))
                          // Light exposure description
                          Text(
                              text = uiState.lightExposure,
                              fontSize = 14.sp,
                              color = MaterialTheme.colorScheme.onBackground,
                              lineHeight = 20.sp,
                              modifier =
                                  Modifier.testTag(PlantInfoScreenTestTags.LIGHT_EXPOSURE_TEXT))
                        }
                      }
                    }
              }
        }
        // PopUp Tips dialog
        if (uiState.showCareTipsDialog) {
          CareTipsDialog(uiState = uiState, onDismiss = { plantInfoViewModel.dismissCareTips() })
        }
      }
}

@Composable
fun ModulableTabRow(
    uiState: PlantInfoUIState,
    plantInfoViewModel: PlantInfoViewModel,
    modifier: Modifier = Modifier
) {
  TabRow(
      selectedTabIndex = uiState.selectedTab.ordinal,
      modifier = modifier,
      containerColor = MaterialTheme.colorScheme.background,
      contentColor = MaterialTheme.colorScheme.onBackground) {
        for (tab in SelectedPlantInfoTab.entries) {
          val tabModifier =
              when (tab) {
                SelectedPlantInfoTab.DESCRIPTION ->
                    Modifier.testTag(PlantInfoScreenTestTags.DESCRIPTION_TAB)
                SelectedPlantInfoTab.HEALTH_STATUS ->
                    Modifier.testTag(PlantInfoScreenTestTags.HEALTH_TAB)
                SelectedPlantInfoTab.LOCATION ->
                    Modifier.testTag(PlantInfoScreenTestTags.LOCATION_TAB)
              }

          Tab(
              selected = uiState.selectedTab == tab,
              onClick = { plantInfoViewModel.setTab(tab) },
              modifier = tabModifier,
              text = {
                Text(
                    text = stringResource(id = tab.textRes),
                    fontWeight =
                        if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp)
              })
        }
      }
}

@Composable
private fun CareTipsDialog(uiState: PlantInfoUIState, onDismiss: () -> Unit) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(text = stringResource(R.string.tips_title, uiState.latinName)) },
      text = {
        val textToShow =
            when {
              uiState.careTips == PlantInfoViewModel.LOADING_TIPS_PLACEHOLDER ->
                  stringResource(R.string.tips_loading_message)
              uiState.careTips == PlantInfoViewModel.UNKNOWN_PLANT_TIPS_PLACEHOLDER ->
                  stringResource(R.string.unknown_plant_tips_message)
              else -> uiState.careTips
            }
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
          Text(text = textToShow, modifier = Modifier.testTag(PlantInfoScreenTestTags.TIPS_TEXT))
        }
      },
      confirmButton = {
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag(PlantInfoScreenTestTags.TIPS_CLOSE_BUTTON)) {
              Text(text = stringResource(id = R.string.tips_close_button))
            }
      },
      modifier = Modifier.testTag(PlantInfoScreenTestTags.TIPS_DIALOG))
}

@Composable
private fun SavePlantBottomBar(uiState: PlantInfoUIState, onSavePlant: () -> Unit) {
  val context = LocalContext.current
  Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
    Button(
        onClick = onSavePlant,
        modifier =
            Modifier.fillMaxWidth().height(56.dp).testTag(PlantInfoScreenTestTags.NEXT_BUTTON),
        shape = RoundedCornerShape(28.dp),
        enabled = !uiState.isSaving,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant)) {
          if (uiState.isSaving) {
            Row(
                modifier = Modifier.testTag(PlantInfoScreenTestTags.NEXT_BUTTON_LOADING),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                  CircularProgressIndicator(
                      modifier = Modifier.size(24.dp),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      strokeWidth = 2.dp)
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                      text = stringResource(id = R.string.uploading),
                      fontSize = 18.sp,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
          } else {
            Text(
                text = context.getString(R.string.next),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium)
          }
        }
  }
}
