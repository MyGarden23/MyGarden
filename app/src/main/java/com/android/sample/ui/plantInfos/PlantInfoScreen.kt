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

  // Initialize UI state when plant changes
  LaunchedEffect(plant) { plantInfoViewModel.initializeUIState(plant) }

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
