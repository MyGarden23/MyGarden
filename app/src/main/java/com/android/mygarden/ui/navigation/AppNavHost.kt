package com.android.mygarden

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.credentials.CredentialManager
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantHealthStatus
import com.android.mygarden.ui.authentication.SignInScreen
import com.android.mygarden.ui.camera.CameraScreen
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.plantinfos.PlantInfosScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    onSignedIn: () -> Unit,
    onOpenPlant: (Bitmap) -> Unit,
    onBack: () -> Unit,
    credentialManagerProvider: () -> CredentialManager = {
      CredentialManager.create(navController.context)
    }
) {
  NavHost(navController = navController, startDestination = startDestination) {
    // Auth
    composable(Screen.Auth.route) {
      SignInScreen(credentialManager = credentialManagerProvider(), onSignedIn = onSignedIn)
    }

    // Top-level Camera
    composable(Screen.Camera.route) { CameraScreen(onPictureTaken = onOpenPlant) }

    // Optional top-level Profile
    composable(Screen.Profile.route) {
      // TODO: ProfileScreen(...)
    }

    // Non top-level Plant View (shows back)
    composable(Screen.PlantView.route) {
      PlantInfosScreen(
          plant =
              Plant(
                  name = "Rose",
                  image = null,
                  latinName = "Rosum",
                  description = "Roses are red",
                  healthStatus = PlantHealthStatus.HEALTHY,
                  healthStatusDescription = PlantHealthStatus.HEALTHY.description,
                  wateringFrequency = 2),
          onBackPressed = onBack)
    }
  }
}
