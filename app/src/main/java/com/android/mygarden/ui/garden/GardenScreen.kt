package com.android.mygarden.ui.garden

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.ui.navigation.NavigationTestTags

/** Test tags to test the screen displays */
object GardenScreenTestTags {
  const val TITLE = "MyGardenTitle"
  const val USER_PROFILE_PICTURE = "UserProfilePicture"
  const val USERNAME = "Username"
  const val EDIT_PROFILE_BUTTON = "EditProfileButton"
  const val GARDEN_LIST = "GardenList"
  const val EMPTY_GARDEN_MSG = "EmptyGardenMsg"
  const val ADD_PLANT_FAB = "AddPlantFAB"

  fun getTestTagForOwnedPlant(plant: OwnedPlant): String = "OwnedPlantNumber${plant.id}"
}

/**
 * Represents the screen of the garden with some user profile infos and the list of plants owned by
 * the user
 *
 * @param gardenViewModel the viewModel that manages the user interactions
 * @param onEditProfile the function to launch when a user clicks on the edit profile button
 * @param onAddPlant the function to launch when a user clicks on the FAB (add a plant button)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GardenScreen(
    gardenViewModel: GardenViewModel = viewModel(),
    onEditProfile: () -> Unit,
    onAddPlant: () -> Unit
) {

  val context = LocalContext.current
  val uiState by gardenViewModel.uiState.collectAsState()
  val plants = uiState.plants

  // Fetch correct owned plants list at each recomposition of the screen
  LaunchedEffect(Unit) { gardenViewModel.refreshUIState() }

  // Display the error message if the list fetch fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      gardenViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.GARDEN_SCREEN),
      // The top bar is only used to display the title of the screen
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  fontWeight = FontWeight.ExtraBold,
                  style = MaterialTheme.typography.titleLarge,
                  text = "My Garden")
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background),
            modifier = Modifier.testTag(GardenScreenTestTags.TITLE))
      },
      // The button to add a new plant to the collection
      floatingActionButton = { AddPlantFloatingButton(onAddPlant) },
      containerColor = MaterialTheme.colorScheme.background,
      content = { pd ->
        Column(modifier = Modifier.fillMaxWidth().padding(pd)) {
          // Profile row with user profile picture, username and a button to edit the profile
          ProfileRow(onEditProfile)
          Spacer(modifier = Modifier.height(16.dp))
          if (plants.isNotEmpty()) {
            // The full list of owned plant
            LazyColumn(
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(horizontal = 30.dp)
                        .testTag(GardenScreenTestTags.GARDEN_LIST)) {
                  items(plants.size) { index -> PlantCard(plants[index]) }
                }
          } else {
            // The list of plant is empty : display a simple message instead
            Box(
                modifier =
                    Modifier.fillMaxSize()
                        .padding(40.dp)
                        .testTag(GardenScreenTestTags.EMPTY_GARDEN_MSG),
                contentAlignment = Alignment.Center) {
                  Text(text = "You don't have a plant yet ! Use the button below to add a plant.")
                }
          }
        }
      })
}

/**
 * Represents the profile row with the user profile picture, its username and a button to edit the
 * profile
 *
 * @param onEditProfile the function to launch when the edit button is clicked on
 */
@Composable
fun ProfileRow(onEditProfile: () -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
      verticalAlignment = Alignment.CenterVertically) {
        // User profile picture
        Icon(
            modifier = Modifier.testTag(GardenScreenTestTags.USER_PROFILE_PICTURE),
            painter = painterResource(R.drawable.profile_unknown_photo_icon_2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.weight(1f))

        // Username
        Text(
            modifier = Modifier.testTag(GardenScreenTestTags.USERNAME),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            text = "Username")
        Spacer(modifier = Modifier.weight(1f))

        // Edit profile button
        IconButton(
            modifier = Modifier.testTag(GardenScreenTestTags.EDIT_PROFILE_BUTTON),
            onClick = onEditProfile) {
              Icon(
                  painter = painterResource(R.drawable.edit_icon),
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary)
            }
      }
}

/**
 * Represents the floating button to add a plant to the garden
 *
 * @param onAddPlant the function to launch when the button is clicked on
 */
@Composable
fun AddPlantFloatingButton(onAddPlant: () -> Unit) {
  ExtendedFloatingActionButton(
      modifier = Modifier.testTag(GardenScreenTestTags.ADD_PLANT_FAB), onClick = onAddPlant) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
              painter = painterResource(R.drawable.tree_icon),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary)
          Text("Add a plant")
        }
      }
}

/**
 * Represents an item of the list of owned plants. Changes color depending on the health status of
 * the plant
 *
 * @param ownedPlant the owned plant with characteristics to display
 */
@Composable
fun PlantCard(ownedPlant: OwnedPlant) {
  // The colored box container
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(10.dp)
              .testTag(GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlant)),
      // Color changing
      colors =
          CardDefaults.cardColors(
              containerColor =
                  when (ownedPlant.plant.healthStatus) {
                    PlantHealthStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
                    PlantHealthStatus.HEALTHY -> MaterialTheme.colorScheme.primaryContainer
                    PlantHealthStatus.SLIGHTLY_DRY -> MaterialTheme.colorScheme.primaryContainer
                    PlantHealthStatus.NEEDS_WATER,
                    PlantHealthStatus.OVERWATERED,
                    PlantHealthStatus.SEVERELY_OVERWATERED,
                    PlantHealthStatus.SEVERELY_DRY -> MaterialTheme.colorScheme.secondaryContainer
                  }),
      elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      shape = RoundedCornerShape(8.dp),
      content = {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
          // The icon or image of the plant
          // TODO: change this icon to the plant image later
          Icon(
              painter = painterResource(R.drawable.potted_plant_icon),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondary)
          Spacer(modifier = Modifier.width(16.dp))
          // All the displayed characteristics of the plant
          Column() {
            Text(text = ownedPlant.plant.name, fontWeight = FontWeight.Bold)
            Text(text = ownedPlant.plant.latinName)
            Text(text = "Status : ${ownedPlant.plant.healthStatus.name}")
          }
        }
      })
}
