package com.android.mygarden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.BottomBar
import com.android.mygarden.ui.navigation.NavigationActions
import com.android.mygarden.ui.navigation.Page
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.popup.PopupViewModel
import com.android.mygarden.ui.popup.WaterPlantPopup
import com.android.mygarden.ui.theme.MyGardenTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      MyGardenTheme() {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MyGardenApp()
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGardenApp() {
  // The main NavController drives all navigation between screens
  val navController = rememberNavController()
  // Small wrapper to simplify navigation calls
  val actions = remember(navController) { NavigationActions(navController) }

  // This view model will be used to collect the plants whose status transitioned to NEEDS_WATER in
  // order to display the pop-up
  val popupVM: PopupViewModel = viewModel()
  // This var is used to know when to display the pop-up and for which plant ; it is reset to [null]
  // everytime a pop-up is not displayed anymore
  var currentThirstyPlant by remember { mutableStateOf<OwnedPlant?>(null) }

  LaunchedEffect(Unit) {
    // Collect all the plants that became thirsty from the view model to display the popup
    popupVM.thirstyPlants.collect { ownedPlant -> currentThirstyPlant = ownedPlant }
  }

  // Determine where to start: if the user is logged in, skip Sign-In
  // For end-to-end tests, we can force starting on camera
  val startDestination = remember {
    // Simple check for end-to-end test mode via system property
    val isEndToEndTest = System.getProperty("mygarden.e2e") == "true"

    if (isEndToEndTest) {
      // In end-to-end test mode, always start on camera screen
      Screen.Camera.route
    } else {
      // In normal app mode, check authentication
      val user =
          runCatching { FirebaseAuth.getInstance().currentUser }
              .onFailure {
                android.util.Log.w("MyGarden", "FirebaseAuth unavailable; defaulting to Auth", it)
              }
              .getOrNull()
      if (user == null) Screen.Auth.route else Screen.Camera.route
    }
  }

  // Observe the current destination so we can update UI accordingly
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  // Helper functions to map the route string to our sealed classes
  val currentScreen: Screen? =
      remember(currentRoute) { if (currentRoute != null) routeToScreen(currentRoute) else null }
  val selectedPage: Page? =
      remember(currentRoute) { if (currentRoute != null) routeToPage(currentRoute) else null }

  Scaffold(
      bottomBar = {
        // Show bottom bar for main screens: Camera, Profile, and Garden
        if (currentScreen == Screen.Camera ||
            currentScreen == Screen.Profile ||
            currentScreen == Screen.Garden) {
          // Determine selected page more carefully - don't default to Camera
          // if we're coming from a non-top-level screen
          val pageToSelect =
              selectedPage
                  ?: when (currentScreen) {
                    Screen.Garden -> Page.Garden
                    Screen.Profile -> Page.Profile
                    Screen.Camera -> Page.Camera
                    else -> Page.Camera
                  }
          BottomBar(
              selectedPage = pageToSelect, onSelect = { actions.navToTopLevel(it.destination) })
        }
      }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
          AppNavHost(
              navController = navController,
              startDestination = startDestination,
          )

          // Display a pop-up whenever a newly thirsty plant is collected from the view model
          currentThirstyPlant?.let {
            WaterPlantPopup(
                plantName = it.plant.name,
                // Reset the var when quitting the pop-up to let a future one be displayed
                onDismiss = { currentThirstyPlant = null },
                // Navigate to garden when using this button and reset the var
                onConfirm = {
                  actions.navTo(Screen.Garden)
                  currentThirstyPlant = null
                })
          }
        }
      }
}

private const val EDIT_PLANT_BASE = "edit_plant"

// Maps the current route (String) to its Screen object
private fun routeToScreen(route: String): Screen? =
    when (route) {
      Screen.Auth.route -> Screen.Auth
      Screen.Camera.route -> Screen.Camera
      Screen.PlantView.route -> Screen.PlantView
      Screen.NewProfile.route -> Screen.NewProfile
      Screen.Profile.route -> Screen.Profile
      Screen.Garden.route -> Screen.Garden
      Screen.ChooseAvatar.route -> Screen.ChooseAvatar
      else -> {
        if (Screen.EditPlant.route.startsWith(EDIT_PLANT_BASE)) {
          val ownedPlantId = route.removePrefix("$EDIT_PLANT_BASE/")
          Screen.EditPlant(ownedPlantId)
        } else {
          null
        }
      }
    }
// Maps the current route (String) to its Page (used for bottom bar selection)
private fun routeToPage(route: String): Page? =
    when (route) {
      Screen.Camera.route -> Page.Camera
      Screen.Profile.route -> Page.Profile
      Screen.Garden.route -> Page.Garden
      else -> null
    }
