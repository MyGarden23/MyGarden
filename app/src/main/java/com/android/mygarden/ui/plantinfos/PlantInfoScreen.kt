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
  const val CONTENT_CONTAINER = "content_container"
  const val DESCRIPTION_TEXT = "description_text"
  const val HEALTH_STATUS_DESCRIPTION = "health_status_description"
  const val HEALTH_STATUS = "health_status"
  const val WATERING_FREQUENCY = "watering_frequency"
  const val NEXT_BUTTON = "next_button"
}

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
  LaunchedEffect(plant) { plantInfoViewModel.initializeUIState(plant) }

  Scaffold(
      modifier = Modifier.testTag(PlantInfoScreenTestTags.SCREEN),
      containerColor = MaterialTheme.colorScheme.background,
      bottomBar = {
        // Bottom bar with "Save Plant" button
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center) {
              Button(
                  onClick = {
                    val plantToSave = uiState.savePlant()
                    plantInfoViewModel.savePlant(
                        plantToSave,
                        onPlantSaved = { plantId ->
                          plantInfoViewModel.resetUIState()
                          onNextPlant(plantId)
                        })
                  },
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(56.dp)
                          .testTag(PlantInfoScreenTestTags.NEXT_BUTTON),
                  shape = RoundedCornerShape(28.dp),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        text = context.getString(R.string.next),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium)
                  }
            }
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

          // --- Name and Latin Name Section ---
          Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
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

          // --- Tab Row for Description/Health ---
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
                      }
                    }
              }
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
        for (tab in SelectedPlantInfoTab.values()) {
          val tabModifier =
              when (tab) {
                SelectedPlantInfoTab.DESCRIPTION ->
                    Modifier.testTag(PlantInfoScreenTestTags.DESCRIPTION_TAB)
                SelectedPlantInfoTab.HEALTH_STATUS ->
                    Modifier.testTag(PlantInfoScreenTestTags.HEALTH_TAB)
              }

          Tab(
              selected = uiState.selectedTab == tab,
              onClick = { plantInfoViewModel.setTab(tab) },
              modifier = tabModifier,
              text = {
                Text(
                    text = tab.text,
                    fontWeight =
                        if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp)
              })
        }
      }
}
