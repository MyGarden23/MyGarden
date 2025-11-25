package com.android.mygarden.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.addFriend.AddFriendScreen
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
  NavHost(navController = navController, startDestination = startDestination) {
    val navigationActions = NavigationActions(navController)

    // Auth
    composable(Screen.Auth.route) {
      SignInScreen(
          credentialManager = credentialManagerProvider(),
          onSignedIn = {
            // New user: go to NewProfile and remove Auth from back stack so that
            // pressing system back does not return to the sign-in screen.
            navController.navigate(Screen.NewProfile.route) {
              popUpTo(Screen.Auth.route) { inclusive = true }
              launchSingleTop = true
            }
          },
          onLogIn = {
            // Existing user: go directly to Camera and also remove Auth from back stack.
            // This prevents returning to the sign-in screen from Camera via the
            // system back button.
            navController.navigate(Screen.Camera.route) {
              popUpTo(Screen.Auth.route) { inclusive = true }
              launchSingleTop = true
            }
          })
    }

    // New Profile
    composable(Screen.NewProfile.route) { backStackEntry ->
      val vm: ProfileViewModel = viewModel()
      HandleAvatarSelection(backStackEntry, vm)

      NewProfileScreen(
          profileViewModel = vm,
          onSavePressed = {
            // When the user finishes creating a profile, navigate to Camera and
            // remove NewProfile from the back stack so the user cannot return to
            // the profile creation screen via the system back button.
            navController.navigate(Screen.Camera.route) {
              popUpTo(Screen.NewProfile.route) { inclusive = true }
              launchSingleTop = true
            }
          },
          onAvatarClick = { navigationActions.navTo(Screen.ChooseAvatar) })
    }

    // Edit Profile
    composable(Screen.EditProfile.route) { backStackEntry ->
      val vm: ProfileViewModel = viewModel()
      HandleAvatarSelection(backStackEntry, vm)

      EditProfileScreen(
          profileViewModel = vm,
          onSavePressed = { navigationActions.navBack() },
          onBackPressed = { navigationActions.navBack() },
          onAvatarClick = { navigationActions.navTo(Screen.ChooseAvatar) })
    }

    // Camera
    composable(Screen.Camera.route) {
      CameraScreen(
          onPictureTaken = { imagePath ->
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>(IMAGE_PATH_KEY)

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
            PlantsRepositoryProvider.repository.cleanup()
            ProfileRepositoryProvider.repository.cleanup()
            ActivityRepositoryProvider.repository.cleanup()
            FirebaseAuth.getInstance().signOut()
            // Clear the entire back stack so saved states can't be
            // restored after logout while keeping `saveState` enabled for normal
            // tab switching.
            while (navController.popBackStack()) {
              // pop until empty
            }
            navController.navigate(Screen.Auth.route) { launchSingleTop = true }
          })
    }

    // Feed
    composable(Screen.Feed.route) {
      FeedScreen(onAddFriend = { navigationActions.navTo(Screen.AddFriend) })
    }

    // Add Friends
    composable(Screen.AddFriend.route) {
      AddFriendScreen(onBackPressed = { navigationActions.navBack() })
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
    composable(Screen.PlantInfo.route) {
      val plantInfoViewModel: PlantInfoViewModel = viewModel()
      val imagePath =
          navController.previousBackStackEntry?.savedStateHandle?.get<String>(IMAGE_PATH_KEY)

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
            navController.navigate(Screen.EditPlant.buildRoute(plantId, Screen.PlantInfo.route)) {
              popUpTo(Screen.Camera.route) { inclusive = false }
            }
          })
    }

    // Choose Avatar
    composable(Screen.ChooseAvatar.route) {
      ChooseProfilePictureScreen(
          onAvatarChosen = { avatar ->
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
                  } else null,
              goBack = {
                if (entry.arguments?.getString(FROM_KEY) == Screen.PlantInfoFromCamera.route) {
                  vm.deletePlant(ownedPlantId)
                }
                navigationActions.navBack()
              })
        }
  }
}

/**
 * Small helper to avoid duplication: Handles reading AVATAR from savedStateHandle + applying it in
 * the ViewModel Used by NewProfile and EditProfile (which had duplicated code).
 */
@Composable
fun HandleAvatarSelection(backStackEntry: NavBackStackEntry, vm: ProfileViewModel) {
  val chosenName by
      backStackEntry.savedStateHandle.getStateFlow(CHOSEN_AVATAR_KEY, "").collectAsState()

  val chosenAvatar =
      chosenName.takeIf { it.isNotBlank() }?.let { runCatching { Avatar.valueOf(it) }.getOrNull() }

  LaunchedEffect(chosenAvatar) {
    if (chosenAvatar != null) {
      vm.setAvatar(chosenAvatar)
      backStackEntry.savedStateHandle[CHOSEN_AVATAR_KEY] = ""
    }
  }
}
