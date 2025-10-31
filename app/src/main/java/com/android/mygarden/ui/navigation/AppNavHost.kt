package com.android.mygarden.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.ui.authentication.SignInScreen
import com.android.mygarden.ui.camera.CameraScreen
import com.android.mygarden.ui.garden.GardenScreen
import com.android.mygarden.ui.plantinfos.PlantInfoViewModel
import com.android.mygarden.ui.plantinfos.PlantInfosScreen
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.profile.ChooseProfilePictureScreen
import com.android.mygarden.ui.profile.NewProfileScreen
import com.android.mygarden.ui.profile.NewProfileViewModel

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
    // handy wrapper so we don't call navController.navigate() directly everywhere
    val navigationActions = NavigationActions(navController)

    // Auth
    composable(Screen.Auth.route) {
      SignInScreen(
          credentialManager = credentialManagerProvider(),
          onSignedIn = { navigationActions.navTo(Screen.NewProfile) })
    }

    // New Profile
    composable(Screen.NewProfile.route) { backStackEntry ->
      val vm: NewProfileViewModel = viewModel()
      val chosenName by
          backStackEntry.savedStateHandle.getStateFlow("chosen_avatar", "").collectAsState()

      val chosenAvatar =
          chosenName
              .takeIf { it.isNotBlank() }
              ?.let { runCatching { Avatar.valueOf(it) }.getOrNull() }

      LaunchedEffect(chosenAvatar) {
        if (chosenAvatar != null) {
          vm.setAvatar(chosenAvatar)
          backStackEntry.savedStateHandle.set("chosen_avatar", "")
        }
      }

      NewProfileScreen(
          newProfileViewModel = vm,
          onRegisterPressed = { navigationActions.navTo(destination = Screen.Camera) },
          onAvatarClick = { navigationActions.navTo(destination = Screen.ChooseAvatar) })
    }

    // Profile
    composable(Screen.Profile.route) {
      // TODO: ProfileScreen(...)
    }

    // Camera
    composable(Screen.Camera.route) {
      CameraScreen(
          onPictureTaken = { imagePath ->
            // Store image path in saved state and navigate to PlantView
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
      val plantInfoViewModel: PlantInfoViewModel = viewModel()
      val imagePath =
          navController.previousBackStackEntry?.savedStateHandle?.get<String>("imagePath")
      // Shows plant details after a photo is taken
      // Right now it just uses a mock Plant object for demo purposes

      val plant = Plant(image = imagePath)

      PlantInfosScreen(
          plant = plant,
          onBackPressed = { navigationActions.navBack() },
          onSavePlant = {
            // Use navToTopLevel to navigate to Garden (top-level screen)
            // This will naturally handle the navigation stack properly
            navigationActions.navToTopLevel(Screen.Garden)
          })
    }

    // Choose Avatar
    composable(Screen.ChooseAvatar.route) {
      ChooseProfilePictureScreen(
          onAvatarChosen = { avatar ->
            // Return the selection to the previous screen (Profile/NewProfile)
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set("chosen_avatar", avatar.name)
            navigationActions.navBack()
          },
          onBack = { navigationActions.navBack() })
    }

    // EditPlant
    // Not yet in the sprint 2 version
    composable(Screen.EditPlant.route) {
      // TODO: for sprint 3
    }
  }
}
