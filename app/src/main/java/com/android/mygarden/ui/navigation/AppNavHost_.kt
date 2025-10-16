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
    val navigationActions = NavigationActions_(navController)
    // Auth
    composable(Screen_.Auth.route) {
      SignInScreen(
          credentialManager = credentialManagerProvider(),
          onSignedIn = { navigationActions.navTo(Screen_.Camera) })
    }

    // Camera
    composable(Screen_.Camera.route) {
      CameraScreen(
          onPictureTaken = { imagePath ->
            navController.currentBackStackEntry?.savedStateHandle?.set("imagePath", imagePath)
            navigationActions.navTo(Screen_.PlantView)
          })
    }

    // Profile
    composable(Screen_.Profile.route) {
      // TODO: ProfileScreen(...)
    }

    // Plant View
    composable(Screen_.PlantView.route) { backStackEntry ->
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
          onBackPressed = { navigationActions.navBack() })
    }
  }
}
