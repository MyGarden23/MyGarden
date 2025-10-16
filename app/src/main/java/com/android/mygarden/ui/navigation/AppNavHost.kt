package com.android.mygarden.ui.navigation

import androidx.compose.runtime.Composable
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.ui.authentication.SignInScreen
import com.android.mygarden.ui.camera.CameraScreen
import com.android.mygarden.ui.garden.GardenScreen
import com.android.mygarden.ui.plantinfos.PlantInfoViewModel
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
    // Profile
    composable(Screen.Profile.route) {
      // TODO: ProfileScreen(...)
    }

    // Camera
    composable(Screen.Camera.route) {
      CameraScreen(
          onPictureTaken = {
            // Navigate to PlantView and remove Camera from back stack to prevent navigation issues
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
      val plantInfoViewModel: PlantInfoViewModel = viewModel()
      // Shows plant details after a photo is taken
      // Right now it just uses a mock Plant object for demo purposes
      val plant =
          Plant(
              name = "Rose",
              image = null,
              latinName = "Rosum",
              description = "Roses are red",
              healthStatus = PlantHealthStatus.HEALTHY,
              healthStatusDescription = PlantHealthStatus.HEALTHY.description,
              wateringFrequency = 2)
      PlantInfosScreen(
          plant = plant,
          onBackPressed = { navigationActions.navBack() },
          onSavePlant = {
            plantInfoViewModel.savePlant(plant)
            // Use navToTopLevel to navigate to Garden (top-level screen)
            // This will naturally handle the navigation stack properly
            navigationActions.navToTopLevel(Screen.Garden)
          })
    }
    // EditPlant
    // Not yet in the sprint 2 version
    composable(Screen.EditPlant.route) {
      // TODO: for sprint 3
          plant =
              Plant(
                  name = "Rose",
                  image = imagePath,
                  latinName = "Rosum",
                  description = "Roses are red",
                  healthStatus = PlantHealthStatus.HEALTHY,
                  healthStatusDescription = PlantHealthStatus.HEALTHY.description,
                  wateringFrequency = 2),
          onBackPressed = { navigationActions.navBack() })
    }
  }
}
