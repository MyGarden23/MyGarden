package com.android.sample.ui.plantInfos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.sample.model.plant.Plant
import com.android.sample.model.plant.PlantHealthStatus

// TODO change each "Color(0xFFF5F0E8)" with the theme color
// TODO change each "Color(0xFF7FA869)"  with the theme color

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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantInfosScreen(
    plant: Plant,
    plantInfoViewModel: PlantInfoViewModel = viewModel(),
    onBackPressed: () -> Unit
) {
  // Observe UI state from ViewModel
  val uiState by plantInfoViewModel.uiState.collectAsState()

  // Remember scroll states for each tab separately
  // This ensures scroll position is maintained when switching between tabs
  val descriptionScrollState = rememberScrollState()
  val healthScrollState = rememberScrollState()

  // Initialize UI state when plant changes
  LaunchedEffect(plant) { plantInfoViewModel.initializeUIState(plant) }

  Scaffold(
      containerColor = Color(0xFFF5F0E8), // Beige background for entire scaffold
      bottomBar = {
        // Bottom bar with "Save Plant" button
        Box(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center) {
              Button(
                  onClick = {
                    // TODO Maybe later add a field to put in the last watered time
                    plantInfoViewModel.savePlant(plant)
                  },
                  modifier = Modifier.fillMaxWidth().height(56.dp),
                  shape = RoundedCornerShape(28.dp),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = Color(0xFF7FA869) // Green color
                          )) {
                    Text(text = "Save Plant", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                  }
            }
      }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          // --- Plant Image Header ---
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .height(280.dp)
                      .background(Color(0xFF8BC34A)) // Placeholder green background
              ) {
                // Placeholder for plant image
                Text(
                    text = "Plant Image",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center))

                // Back button overlaid on top-left corner of image
                IconButton(
                    onClick = onBackPressed,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                      Icon(
                          imageVector = Icons.Filled.ArrowBack,
                          contentDescription = "Back",
                          tint = Color.Black)
                    }
              }

          // --- Name and Latin Name Section ---
          Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            // Common name (e.g., "Rose")
            Text(
                text = uiState.name,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black)
            // Scientific name (e.g., "Rosa rubiginosa")
            Text(text = uiState.latinName, fontSize = 16.sp, color = Color.Gray)
          }

          // --- Tab Row for Description/Health ---
          TabRow(
              selectedTabIndex =
                  if (uiState.selectedTab == SelectedPlantInfoTab.DESCRIPTION) 0 else 1,
              modifier = Modifier.fillMaxWidth(),
              containerColor = Color(0xFFF5F0E8), // Beige background
              contentColor = Color.Black) {
                // Description Tab
                Tab(
                    selected = uiState.selectedTab == SelectedPlantInfoTab.DESCRIPTION,
                    onClick = { plantInfoViewModel.setTab(SelectedPlantInfoTab.DESCRIPTION) },
                    text = {
                      Text(
                          text = "Description",
                          fontWeight =
                              if (uiState.selectedTab == SelectedPlantInfoTab.DESCRIPTION)
                                  FontWeight.Bold
                              else FontWeight.Normal,
                          fontSize = 16.sp)
                    })
                // Health Tab
                Tab(
                    selected = uiState.selectedTab == SelectedPlantInfoTab.HEALTH_STATUS,
                    onClick = { plantInfoViewModel.setTab(SelectedPlantInfoTab.HEALTH_STATUS) },
                    text = {
                      Text(
                          text = "Health",
                          fontWeight =
                              if (uiState.selectedTab == SelectedPlantInfoTab.HEALTH_STATUS)
                                  FontWeight.Bold
                              else FontWeight.Normal,
                          fontSize = 16.sp)
                    })
              }

          // --- Scrollable Content for Active Tab ---
          Box(
              modifier =
                  Modifier.fillMaxWidth()
                      .weight(1f) // Takes remaining space between tabs and bottom button
                      .background(Color(0xFFF5F0E8)) // Beige background
              ) {
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
                              color = Color.Black,
                              lineHeight = 20.sp)
                        }

                        // --- Health Tab Content ---
                        SelectedPlantInfoTab.HEALTH_STATUS -> {
                          // Health status description
                          Text(
                              text = uiState.healthStatusDescription,
                              fontSize = 14.sp,
                              color = Color.Black,
                              lineHeight = 20.sp)
                          Spacer(modifier = Modifier.height(16.dp))

                          // Current health status with emoji
                          Text(
                              text = "Status: ${uiState.healthStatus.description}",
                              fontSize = 16.sp,
                              fontWeight = FontWeight.Medium,
                              color = Color.Black)
                          Spacer(modifier = Modifier.height(8.dp))

                          // Watering frequency information
                          Text(
                              text = "Watering Frequency: Every ${uiState.wateringFrequency} days",
                              fontSize = 14.sp,
                              color = Color.Black)
                        }
                      }
                    }
              }
        }
      }
}

@Preview
@Composable
fun PlantDetailScreenPreview() {
  val plant =
      Plant(
          name = "test name",
          image = null, // Can use a URL or a ByteArray for storage (to be discussed)
          latinName = "latin name",
          description = "Description of the plant",
          healthStatus = PlantHealthStatus.HEALTHY,
          healthStatusDescription = "The plant is healthy",
          wateringFrequency = 0, // in days
      )
  PlantInfosScreen(plant) {}
}
