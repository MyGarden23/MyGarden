package com.android.mygarden

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.mygarden.model.notifications.AppLifecycleObserver
import com.android.mygarden.model.notifications.NotificationsPermissionHandler.hasAlreadyDeniedNotificationsPermission
import com.android.mygarden.model.notifications.NotificationsPermissionHandler.hasNotificationsPermission
import com.android.mygarden.model.notifications.NotificationsPermissionHandler.setHasAlreadyDeniedNotificationsPermission
import com.android.mygarden.model.notifications.PushNotificationsService
import com.android.mygarden.model.plant.OwnedPlant
import com.android.mygarden.ui.friendsRequests.FriendRequestUiModel
import com.android.mygarden.ui.friendsRequests.FriendsRequestsPopup
import com.android.mygarden.ui.friendsRequests.FriendsRequestsPopupViewModel
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.BottomBar
import com.android.mygarden.ui.navigation.NavigationActions
import com.android.mygarden.ui.navigation.Page
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.navigation.routeToPage
import com.android.mygarden.ui.navigation.routeToScreen
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
  val isInTestEnvironment = rememberIsInTestEnvironment()

  // Ask for notification permission and skip it if currently in test environments to avoid
  // interference
  AskForNotificationsPermissionIfNeeded(isInTestEnvironment)

  // If the app is launched by a notification, go to the garden
  // Only create the LaunchedEffect if there's an intent and not in test mode
  HandleNotificationNavigation(intent, isInTestEnvironment, actions)

  // Determine where to start: if the user is logged in, skip Sign-In
  // For end-to-end tests, we can force starting on camera
  val startDestination = rememberStartDestination()

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
        // Show bottom bar for main screens: Camera, Feed and Garden
        if (currentScreen == Screen.Camera ||
            currentScreen == Screen.Garden ||
            currentScreen == Screen.Feed) {
          // Determine selected page more carefully - don't default to Camera
          // if we're coming from a non-top-level screen
          val pageToSelect =
              selectedPage
                  ?: when (currentScreen) {
                    Screen.Garden -> Page.Garden
                    Screen.Camera -> Page.Camera
                    Screen.Feed -> Page.Feed
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
          ThirstyPlantsPopup(actions = actions)
          NewFriendRequestPopup(actions = actions)
        }
      }
}

/**
 * Determines whether the app is running in a test environment.
 *
 * This checks several indicators:
 * - A custom system property `"mygarden.e2e"` used for end-to-end tests
 * - The Java classpath containing `"androidTest"` (instrumented tests)
 * - The presence of the AndroidJUnit4 test runner
 *
 * The result is remembered so it is computed only once per composition.
 *
 * @return `true` if running under a test environment, otherwise `false`.
 */
@Composable
private fun rememberIsInTestEnvironment(): Boolean = remember {
  System.getProperty("mygarden.e2e") == "true" ||
      System.getProperty("java.class.path")?.contains("androidTest") == true ||
      try {
        Class.forName("androidx.test.ext.junit.runners.AndroidJUnit4")
        true
      } catch (e: ClassNotFoundException) {
        false
      }
}

/**
 * Computes the initial navigation destination of the app based on authentication state.
 *
 * Logic:
 * - If a Firebase user is already logged in → start at the Camera screen.
 * - Otherwise → start at the Auth screen.
 *
 * FirebaseAuth access is wrapped in `runCatching` to avoid crashes on devices or tests where
 * Firebase may not be available.
 *
 * The result is remembered so it runs only once per composition.
 *
 * @return The route string of the start destination.
 */
@Composable
private fun rememberStartDestination(): String = remember {
  val user =
      runCatching { FirebaseAuth.getInstance().currentUser }
          .onFailure {
            android.util.Log.w("MyGarden", "FirebaseAuth unavailable; defaulting to Auth", it)
          }
          .getOrNull()
  if (user == null) Screen.Auth.route else Screen.Camera.route
}

/**
 * Handles app navigation triggered by a received push notification.
 *
 * Behavior:
 * - If running in a test environment, this is disabled to avoid flaky navigation.
 * - If the intent contains a `"NOTIFICATIONS_TYPE_IDENTIFIER"`:
 *     - For WATER_PLANT → navigate to the Garden screen.
 *     - The intent key is removed afterward to avoid repeated triggers.
 *
 * Navigation occurs inside a `LaunchedEffect` tied to the intent.
 *
 * @param intent The activity intent that may contain notification data.
 * @param isInTestEnvironment Whether the app is currently running under tests.
 * @param actions Helper object for navigation operations.
 */
@Composable
private fun HandleNotificationNavigation(
    intent: Intent?,
    isInTestEnvironment: Boolean,
    actions: NavigationActions,
) {
  if (intent == null || isInTestEnvironment) return

  LaunchedEffect(intent) {
    val notificationType =
        intent.getStringExtra(PushNotificationsService.NOTIFICATIONS_TYPE_IDENTIFIER)
    if (notificationType == PushNotificationsService.NOTIFICATIONS_TYPE_WATER_PLANT) {
      actions.navTo(Screen.Garden)
      intent.removeExtra(PushNotificationsService.NOTIFICATIONS_TYPE_IDENTIFIER)
    }
  }
}

/**
 * Requests notification permissions when appropriate.
 *
 * Behavior:
 * - In normal app execution → asks for notification permission using
 *   `AskForNotificationsPermission()`.
 * - In test environments → permission dialog is skipped to avoid blocking tests.
 *
 * @param isInTestEnvironment Indicates whether the current execution is part of a test suite.
 */
@Composable
private fun AskForNotificationsPermissionIfNeeded(isInTestEnvironment: Boolean) {
  if (!isInTestEnvironment) {
    AskForNotificationsPermission()
  }
}

/**
 * Observes the list of plants that have just transitioned to a "needs water" state and displays a
 * popup to notify the user.
 *
 * Behavior:
 * - Subscribes to `PopupViewModel.thirstyPlants`, which emits an `OwnedPlant` whenever its status
 *   becomes `NEEDS_WATER`.
 * - When a new plant is received, it is stored in a local state (`currentThirstyPlant`) and a
 *   `WaterPlantPopup` is shown.
 * - Dismissing or confirming the popup resets `currentThirstyPlant` to `null`, allowing future
 *   popups to be shown.
 * - Confirming the popup also navigates the user to the Garden screen.
 *
 * @param actions Navigation helper used to redirect the user when confirming the popup.
 * @param popupVM ViewModel that emits plants needing water. Defaults to a local ViewModel instance.
 */
@Composable
private fun ThirstyPlantsPopup(
    actions: NavigationActions,
    popupVM: PopupViewModel = viewModel(),
) {
  // This view model will be used to collect the plants whose status transitioned to NEEDS_WATER in
  // order to display the pop-up
  // This var is used to know when to display the pop-up and for which plant ; it is reset to [null]
  // everytime a pop-up is not displayed anymore
  val viewModel = popupVM
  var currentThirstyPlant by remember { mutableStateOf<OwnedPlant?>(null) }

  LaunchedEffect(Unit) {
    // Collect all the plants that became thirsty from the view model to display the popup
    viewModel.thirstyPlants.collect { ownedPlant -> currentThirstyPlant = ownedPlant }
  }

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

/**
 * Ask the user for notification permission if the API is greater of equal to 33.
 *
 * Note: this function is really basic and assumes that the user allows the app to send
 * notifications. The complete permission workflow will be implemented in the future.
 */
@Composable
internal fun AskForNotificationsPermission() {
  // Notification permission request if API is >= 33
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
  val context = LocalContext.current
  val notificationPermission = remember { mutableStateOf(hasNotificationsPermission(context)) }

  val notificationLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { notificationPermission.value = it })

  LaunchedEffect(Unit) {
    // Refresh the permission on new app launch
    notificationPermission.value = hasNotificationsPermission(context)
    // Notification permission request only trigger when the user has not already denied it
    if (!notificationPermission.value && !hasAlreadyDeniedNotificationsPermission(context)) {
      notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      setHasAlreadyDeniedNotificationsPermission(context, true)
    }
  }
}

@Composable
private fun NewFriendRequestPopup(
    actions: NavigationActions,
    friendRequestPopupVM: FriendsRequestsPopupViewModel = viewModel(),
) {
  var currentFriendRequest by remember { mutableStateOf<FriendRequestUiModel?>(null) }

  LaunchedEffect(Unit) {
    friendRequestPopupVM.newRequests.collect { friendRequestUIModel ->
      currentFriendRequest = friendRequestUIModel
    }
  }
  Log.d("FriendRequestsMain", currentFriendRequest.toString())
  currentFriendRequest?.let {
    FriendsRequestsPopup(
        senderPseudo = it.senderPseudo,
        onDismiss = { currentFriendRequest = null },
        onConfirm = {
          actions.navTo(Screen.Camera) // TODO(Change this to FriendRequestScreen)
          currentFriendRequest = null
        })
  }
}
