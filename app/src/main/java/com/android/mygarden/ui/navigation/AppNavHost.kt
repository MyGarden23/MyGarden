package com.android.mygarden.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.ui.authentication.SignInScreen
import com.android.mygarden.ui.camera.CameraScreen
import com.android.mygarden.ui.editPlant.EditPlantScreen
import com.android.mygarden.ui.editPlant.EditPlantViewModel
import com.android.mygarden.ui.garden.GardenScreen
import com.android.mygarden.ui.plantinfos.PlantInfoViewModel
import com.android.mygarden.ui.plantinfos.PlantInfosScreen
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.profile.ChooseProfilePictureScreen
import com.android.mygarden.ui.profile.EditProfileScreen
import com.android.mygarden.ui.profile.NewProfileScreen
import com.android.mygarden.ui.profile.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth

private const val CHOSEN_AVATAR_KEY = "chosen_avatar"
private const val IMAGE_PATH_KEY = "imagePath"
private const val FROM_KEY = "from"
private const val OWNED_PLANT_ID_KEY = "ownedPlantId"

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
          backStackEntry.savedStateHandle.getStateFlow(CHOSEN_AVATAR_KEY, "").collectAsState()

      val chosenAvatar =
          chosenName
              .takeIf { it.isNotBlank() }
              ?.let { runCatching { Avatar.valueOf(it) }.getOrNull() }

      LaunchedEffect(chosenAvatar) {
        if (chosenAvatar != null) {
          vm.setAvatar(chosenAvatar)
          backStackEntry.savedStateHandle.set(CHOSEN_AVATAR_KEY, "")
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
          backStackEntry.savedStateHandle.getStateFlow(CHOSEN_AVATAR_KEY, "").collectAsState()

      val chosenAvatar =
          chosenName
              .takeIf { it.isNotBlank() }
              ?.let { runCatching { Avatar.valueOf(it) }.getOrNull() }

      LaunchedEffect(chosenAvatar) {
        if (chosenAvatar != null) {
          vm.setAvatar(chosenAvatar)
          backStackEntry.savedStateHandle.set(CHOSEN_AVATAR_KEY, "")
        }
      }

      EditProfileScreen(
          profileViewModel = vm,
          onSavePressed = { navigationActions.navBack() },
          onBackPressed = { navigationActions.navBack() },
          onAvatarClick = { navigationActions.navTo(destination = Screen.ChooseAvatar) })
    }

    // Camera
    composable(Screen.Camera.route) {
      CameraScreen(
          onPictureTaken = { imagePath ->
            // Clean up any previous image path before storing the new one
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(IMAGE_PATH_KEY)
            // Store image path in saved state and navigate to PlantView
            navController.currentBackStackEntry?.savedStateHandle?.set(IMAGE_PATH_KEY, imagePath)
            navigationActions.navTo(Screen.PlantInfo)
          })
    }

    // Garden
    composable(Screen.Garden.route) {
      GardenScreen(
          onEditProfile = { navigationActions.navTo(Screen.EditProfile) },
          onAddPlant = { navigationActions.navTo(Screen.Camera) },
          onPlantClick = { ownedPlant ->
            navigationActions.navTo(Screen.EditPlant(ownedPlant.id, Screen.Garden.route))
          },
          onSignOut = {
            FirebaseAuth.getInstance().signOut()
            navigationActions.navTo(Screen.Auth)
          })
    }

    // Plant Info
    composable(Screen.PlantInfo.route) { backStackEntry ->
      val plantInfoViewModel: PlantInfoViewModel = viewModel()
      val imagePath =
          navController.previousBackStackEntry?.savedStateHandle?.get<String>(IMAGE_PATH_KEY)

      // Shows plant details after a photo is taken
      // Right now it just uses a mock Plant object for demo purposes

      val plant = Plant(image = imagePath)

      PlantInfosScreen(
          plant = plant,
          plantInfoViewModel = plantInfoViewModel,
          onBackPressed = { navigationActions.navBack() },
          onNextPlant = { plantId ->
            navigationActions.navTo(Screen.EditPlant(plantId, Screen.PlantInfo.route))
          })
    }

    // Choose Avatar
    composable(Screen.ChooseAvatar.route) {
      ChooseProfilePictureScreen(
          onAvatarChosen = { avatar ->
            // Return the selection to the previous screen (Profile/NewProfile)
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set(CHOSEN_AVATAR_KEY, avatar.name)
            navigationActions.navBack()
          },
          onBack = { navigationActions.navBack() })
    }

    // EditPlant
    composable(
        route = Screen.EditPlant.route,
        arguments =
            listOf(
                navArgument(OWNED_PLANT_ID_KEY) { type = NavType.StringType },
                navArgument(FROM_KEY) {
                  type = NavType.StringType
                  nullable = true
                })) { entry ->
          val vm: EditPlantViewModel = viewModel()
          val ownedPlantId = entry.arguments?.getString(OWNED_PLANT_ID_KEY) ?: return@composable
          EditPlantScreen(
              ownedPlantId = ownedPlantId,
              editPlantViewModel = vm,
              onSaved = { navigationActions.navTo(Screen.Garden) },
              onDeleted = { navigationActions.navTo(Screen.Garden) },
              goBack = {
                if (entry.arguments?.getString(FROM_KEY) == Screen.PlantInfo.route) {
                  // Need to delete manually due to our implementation of Screen.PlantInfo.route (we
                  // add by default the plant to our garden but delete it if the user don't want to
                  // add the plant to the garden)
                  vm.deletePlant(ownedPlantId)
                  navigationActions.navBack()
                } else {
                  navigationActions.navBack()
                }
              })
        }
  }
}
