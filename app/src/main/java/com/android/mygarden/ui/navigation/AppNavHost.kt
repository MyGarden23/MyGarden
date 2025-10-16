package com.android.mygarden.ui.navigation

import androidx.compose.runtime.Composable
import androidx.credentials.CredentialManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.ui.authentication.SignInScreen
import com.android.mygarden.ui.camera.CameraScreen
import com.android.mygarden.ui.editPlant.EditPlantScreen
import com.android.mygarden.ui.garden.GardenScreen
import com.android.mygarden.ui.plantinfos.PlantInfosScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    credentialManagerProvider: () -> CredentialManager = {
      CredentialManager.create(navController.context)
    }
) {
  // This NavHost is basically the "map" of all screens in the app.
  // Each composable() below defines one destination (screen) and what to do when navigating there.
  NavHost(navController = navController, startDestination = startDestination) {
    // handy wrapper so we don’t call navController.navigate() directly everywhere
    val navigationActions = NavigationActions(navController)
    // Auth
    composable(Screen.Auth.route) {
      SignInScreen(
          credentialManager = credentialManagerProvider(),
          onSignedIn = { navigationActions.navTo(Screen.Camera) })
    }

    // Camera
    composable(Screen.Camera.route) {
      CameraScreen(
          onPictureTaken = { imagePath ->
            navController.currentBackStackEntry?.savedStateHandle?.set("imagePath", imagePath)
            navigationActions.navTo(Screen.PlantView)
          })
    }

    // Garden
    composable(Screen.Garden.route) {
      GardenScreen(
          onEditProfile = { /* TODO: Navigate to Profile edit */},
          onAddPlant = { navigationActions.navTo(Screen.Camera) })
    }

    // Plant View
    composable(Screen.PlantView.route) { backStackEntry ->
      val imagePath = backStackEntry.savedStateHandle.get<String>("imagePath")
      // Shows plant details after a photo is taken
      // Right now it just uses a mock Plant object for demo purposes
      PlantInfosScreen(
          plant =
              Plant(
                  name = "Rose",
                  image = imagePath,
                  latinName = "Rosum",
                  description = "Roses are red",
                  healthStatus = PlantHealthStatus.HEALTHY,
                  healthStatusDescription = PlantHealthStatus.HEALTHY.description,
                  wateringFrequency = 2),
          onBackPressed = { navigationActions.navBack() },
          onSavePlant = {
            // First save the plant to repository, then navigate to EditPlant
            // TODO: Pass the actual plant ID from the save operation
            navigationActions.navTo(Screen.EditPlant)
          })
    }

    // EditPlant
    composable(Screen.EditPlant.route) {
      // For now, we use a mock plant ID. Later this should come from navigation arguments
      EditPlantScreen(
          ownedPlantId = "mock-plant-id", // TODO: Get from navigation arguments
          onSaved = { navigationActions.navTo(Screen.Garden) },
          onDeleted = { navigationActions.navTo(Screen.Garden) },
          goBack = { navigationActions.navBack() })
    }
  }
}
