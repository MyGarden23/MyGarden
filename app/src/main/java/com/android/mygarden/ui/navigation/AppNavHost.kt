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
import com.android.mygarden.ui.profile.EditProfileScreen
import com.android.mygarden.ui.profile.NewProfileScreen
import com.google.firebase.auth.FirebaseAuth
import com.android.mygarden.ui.profile.ProfileViewModel

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
          onSignedIn = { navigationActions.navTo(Screen.NewProfile) },
          onLogIn = { navigationActions.navTo(Screen.Camera) })
    }

    // New Profile
    composable(Screen.NewProfile.route) { backStackEntry ->
      val vm: ProfileViewModel = viewModel()
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
          profileViewModel = vm,
          onSavePressed = { navigationActions.navTo(destination = Screen.Camera) },
          onAvatarClick = { navigationActions.navTo(destination = Screen.ChooseAvatar) })
    }

    // Edit Profile
    composable(Screen.EditProfile.route) { backStackEntry ->
      val vm: ProfileViewModel = viewModel()
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

      EditProfileScreen(
          profileViewModel = vm,
          onSavePressed = { navigationActions.navBack() },
          onBackPressed = { navigationActions.navBack() },
          onAvatarClick = { navigationActions.navTo(destination = Screen.ChooseAvatar) })
    }

    // Profile
    composable(Screen.Garden.route) {
      // TODO: ProfileScreen(...)
    }

    // Camera
    composable(Screen.Camera.route) {
      CameraScreen(
          onPictureTaken = { imagePath ->
            // Store image path in saved state and navigate to PlantView
            navController.currentBackStackEntry?.savedStateHandle?.set("imagePath", imagePath)
            navigationActions.navTo(Screen.PlantInfo)
          })
    }

    // Garden
    composable(Screen.Garden.route) {
      GardenScreen(
          onEditProfile = { navigationActions.navTo(Screen.EditProfile) },
          onAddPlant = { navigationActions.navTo(Screen.Camera) },
          onPlantClick = { ownedPlant -> navigationActions.navTo(Screen.EditPlant(ownedPlant.id)) },
          onSignOut = {
            FirebaseAuth.getInstance().signOut()
            navigationActions.navTo(Screen.Auth)
          })
    }

    // Plant Info
    composable(Screen.PlantInfo.route) { backStackEntry ->
      val plantInfoViewModel: PlantInfoViewModel = viewModel()
      val imagePath =
          navController.previousBackStackEntry?.savedStateHandle?.get<String>("imagePath")
      // Shows plant details after a photo is taken
      // Right now it just uses a mock Plant object for demo purposes

      val plant = Plant(image = imagePath)

      PlantInfosScreen(
          plant = plant,
          plantInfoViewModel = plantInfoViewModel,
          onBackPressed = { navigationActions.navBack() },
          onSavePlant = { navigationActions.navTo(Screen.Garden) })
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
    composable(
        route = Screen.EditPlant.route,
        arguments = listOf(navArgument("ownedPlantId") { type = NavType.StringType })) { entry ->
          val ownedPlantId = entry.arguments?.getString("ownedPlantId") ?: return@composable
          EditPlantScreen(
              ownedPlantId = ownedPlantId,
              onSaved = { navigationActions.navToTopLevel(Screen.Garden) },
              onDeleted = { navigationActions.navToTopLevel(Screen.Garden) },
              goBack = { navigationActions.navBack() })
        }
  }
}
