package com.android.mygarden

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.mygarden.model.notifications.AppLifecycleObserver
import com.android.mygarden.model.notifications.PushNotificationsService
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

    ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver)

    setContent {
      MyGardenTheme() {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MyGardenApp(intent = intent)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGardenApp(intent: Intent? = null) {
  // The main NavController drives all navigation between screens
  val navController = rememberNavController()
  // Small wrapper to simplify navigation calls
  val actions = remember(navController) { NavigationActions(navController) }

  // Check if we're in any test environment
  val isInTestEnvironment = remember {
    System.getProperty("mygarden.e2e") == "true" ||
        System.getProperty("java.class.path")?.contains("androidTest") == true ||
        try {
          Class.forName("androidx.test.ext.junit.runners.AndroidJUnit4")
          true
        } catch (e: ClassNotFoundException) {
          false
        }
  }

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

  // Ask for notification permission
  /* For Sprint 5, assume that the user will accept to receive notifications.
  Permission handling will be done in a separate task in Sprint 6 */
  // Skip notification permission in test environments to avoid interference
  if (!isInTestEnvironment) {
    AskForNotificationsPermission()
  }

  // If the app is launched by a notification, go to the garden
  // Only create the LaunchedEffect if there's an intent and not in test mode
  if (intent != null && !isInTestEnvironment) {
    LaunchedEffect(intent) {
      val notificationType =
          intent.getStringExtra(PushNotificationsService.NOTIFICATIONS_TYPE_IDENTIFIER)
      if (notificationType == PushNotificationsService.NOTIFICATIONS_TYPE_WATER_PLANT) {
        actions.navTo(Screen.Garden)
        intent.removeExtra(PushNotificationsService.NOTIFICATIONS_TYPE_IDENTIFIER)
      }
    }
  }

  // Determine where to start: if the user is logged in, skip Sign-In
  // For end-to-end tests, we can force starting on camera
  val startDestination = remember {
    val user =
        runCatching { FirebaseAuth.getInstance().currentUser }
            .onFailure {
              android.util.Log.w("MyGarden", "FirebaseAuth unavailable; defaulting to Auth", it)
            }
            .getOrNull()
    if (user == null) Screen.Auth.route else Screen.Camera.route
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
        // Show bottom bar for main screens: Camera and Garden
        if (currentScreen == Screen.Camera || currentScreen == Screen.Garden) {
          // Determine selected page more carefully - don't default to Camera
          // if we're coming from a non-top-level screen
          val pageToSelect =
              selectedPage
                  ?: when (currentScreen) {
                    Screen.Garden -> Page.Garden
                    Screen.Camera -> Page.Camera
                    else -> Page.Camera
                  }
          BottomBar(selectedPage = pageToSelect, onSelect = { actions.navTo(it.destination) })
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
      Screen.PlantInfo.route -> Screen.PlantInfo
      Screen.PlantInfoFromGarden.route -> Screen.PlantInfoFromGarden
      Screen.PlantInfoFromCamera.route -> Screen.PlantInfoFromCamera
      Screen.NewProfile.route -> Screen.NewProfile
      Screen.Garden.route -> Screen.Garden
      Screen.ChooseAvatar.route -> Screen.ChooseAvatar
      else -> {
        if (route.startsWith(EDIT_PLANT_BASE)) {
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
      Screen.Garden.route -> Page.Garden
      else -> null
    }

/**
 * Ask the user for notification permission if the API is greater of equal to 33.
 *
 * Note: this function is really basic and assumes that the user allows the app to send
 * notifications. The complete permission workflow will be implemented in the future.
 */
@Composable
private fun AskForNotificationsPermission() {
  // Notification permission request if API is >= 33
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
  val context = LocalContext.current
  val notificationPermission = remember { mutableStateOf(hasNotificationsPermission(context)) }

  val notificationLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { notificationPermission.value = it })

  // Side-effect: only trigger once, not on every recomposition
  LaunchedEffect(Unit) {
    if (!hasNotificationsPermission(context)) {
      notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}

/**
 * Returns true or false depending on whether the user has granted notifications permission
 *
 * @param context the context used to access permission state
 * @return true if the user has granted the app notifications permission, false otherwise
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun hasNotificationsPermission(context: Context): Boolean {
  return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
      PackageManager.PERMISSION_GRANTED
}
