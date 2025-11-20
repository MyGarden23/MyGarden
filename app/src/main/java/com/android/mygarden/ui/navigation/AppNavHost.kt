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
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.authentication.SignInScreen
import com.android.mygarden.ui.camera.CameraScreen
import com.android.mygarden.ui.editPlant.EditPlantScreen
import com.android.mygarden.ui.editPlant.EditPlantViewModel
import com.android.mygarden.ui.feed.FeedScreen
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
private const val OWNED_PLANT_ID_TO_PLANT_INFO_KEY = "ownedPlantId_to_plantInfo"

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
            navigationActions.navTo(Screen.PlantInfoFromCamera)
          })
    }

    // Plant Info From Camera
    composable(Screen.PlantInfoFromCamera.route) {
      val imagePath =
          navController.previousBackStackEntry?.savedStateHandle?.get<String>(IMAGE_PATH_KEY)

      val plant = Plant(image = imagePath)

      PlantInfosScreen(
          plant = plant,
          ownedPlantId = null,
          onNextPlant = { plantId ->
            // Navigate to EditPlant and remove PlantInfo from backstack
            // This prevents going back to PlantInfo after saving (local image is deleted)
            navController.navigate(
                Screen.EditPlant.buildRoute(plantId, Screen.PlantInfoFromCamera.route)) {
                  popUpTo(Screen.Camera.route) { inclusive = false }
                }
          },
          onBackPressed = { navigationActions.navBack() })
    }

    // Garden
    composable(Screen.Garden.route) {
      GardenScreen(
          onEditProfile = { navigationActions.navTo(Screen.EditProfile) },
          onAddPlant = { navigationActions.navTo(Screen.Camera) },
          onPlantClick = { ownedPlant ->
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set(OWNED_PLANT_ID_TO_PLANT_INFO_KEY, ownedPlant.id)
            navigationActions.navTo(Screen.PlantInfoFromGarden)
          },
          onSignOut = {
            // Clean up repositories before signing out to prevent PERMISSION_DENIED errors
            PlantsRepositoryProvider.repository.cleanup()
            ProfileRepositoryProvider.repository.cleanup()
            FirebaseAuth.getInstance().signOut()
            navigationActions.navTo(Screen.Auth)
          })
    }

    // Feed
    composable(Screen.Feed.route) {
      FeedScreen() // Add the OnAddFriend callback when implemented!
    }

    // Plant Info From Garden
    composable(Screen.PlantInfoFromGarden.route) { _ ->
      val plantInfoViewModel: PlantInfoViewModel = viewModel()
      val ownedPlantId: String? =
          navController.previousBackStackEntry
              ?.savedStateHandle
              ?.get<String>(OWNED_PLANT_ID_TO_PLANT_INFO_KEY)

      PlantInfosScreen(
          plant = Plant(),
          ownedPlantId = ownedPlantId,
          plantInfoViewModel = plantInfoViewModel,
          onBackPressed = { navigationActions.navBack() },
          onNextPlant = { plantId ->
            navigationActions.navTo(Screen.EditPlant(plantId, Screen.Garden.route))
          },
      )
    }

    // Plant Info
    composable(Screen.PlantInfo.route) { backStackEntry ->
      val plantInfoViewModel: PlantInfoViewModel = viewModel()
      val imagePath =
          navController.previousBackStackEntry?.savedStateHandle?.get<String>(IMAGE_PATH_KEY)

      // Shows plant details after a photo is taken
      // Right now it just uses a mock Plant object for demo purposes

      val plant = Plant(image = imagePath)
      val ownedPlantId: String? =
          navController.previousBackStackEntry
              ?.savedStateHandle
              ?.get<String>(OWNED_PLANT_ID_TO_PLANT_INFO_KEY)
      PlantInfosScreen(
          plant = plant,
          ownedPlantId = ownedPlantId,
          plantInfoViewModel = plantInfoViewModel,
          onBackPressed = { navigationActions.navBack() },
          onNextPlant = { plantId ->
            // Navigate to EditPlant and remove PlantInfo from backstack
            // This prevents going back to PlantInfo after saving (local image is deleted)
            navController.navigate(Screen.EditPlant.buildRoute(plantId, Screen.PlantInfo.route)) {
              popUpTo(Screen.Camera.route) { inclusive = false }
            }
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
              onDeleted =
                  if (entry.arguments?.getString(FROM_KEY) != Screen.PlantInfoFromCamera.route) {
                    { navigationActions.navTo(Screen.Garden) }
                  } else {
                    null
                  },
              goBack = {
                if (entry.arguments?.getString(FROM_KEY) == Screen.PlantInfoFromCamera.route) {
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
