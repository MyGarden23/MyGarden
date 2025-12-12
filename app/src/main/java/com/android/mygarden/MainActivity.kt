package com.android.mygarden

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.mygarden.model.notifications.AppLifecycleObserver
import com.android.mygarden.model.notifications.NotificationsPermissionHandler.hasAlreadyDeniedNotificationsPermission
import com.android.mygarden.model.notifications.NotificationsPermissionHandler.hasNotificationsPermission
import com.android.mygarden.model.notifications.NotificationsPermissionHandler.setHasAlreadyDeniedNotificationsPermission
import com.android.mygarden.model.notifications.PushNotificationsService
import com.android.mygarden.model.offline.OfflineStateManager
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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import kotlinx.coroutines.launch

private const val JAVA_CLASSPATH_PROPERTY = "java.class.path"
private const val ANDROID_TEST_PATH = "androidTest"
private const val ANDROID_JUNIT_RUNNER = "androidx.test.ext.junit.runners.AndroidJUnit4"

/** Log tags */
private const val LOG_TAG_MAIN_ACTIVITY = "MainActivity"
private const val LOG_TAG_MYGARDEN = "MyGarden"

/** Log messages */
private const val LOG_MSG_FIRESTORE_PERSISTENCE_FAILED = "Failed to enable Firestore persistence"
private const val LOG_MSG_FIREBASE_AUTH_UNAVAILABLE = "FirebaseAuth unavailable; defaulting to Auth"

/** Dimensions */
private val OFFLINE_INDICATOR_PADDING = 8.dp

/** Offline indicator timing (in milliseconds) */
private const val OFFLINE_INDICATOR_VISIBLE_DURATION = 2000L
private const val OFFLINE_INDICATOR_HIDDEN_DURATION = 3000L

class MainActivity : ComponentActivity() {

  companion object {
    /** System property keys for test environment detection */
    const val E2E_TEST_PROPERTY = "mygarden.e2e"
    const val E2E_TEST_VALUE = "true"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize Firestore offline persistence
    // This enables automatic local caching of Firestore data
    try {
      val firestore = FirebaseFirestore.getInstance()
      val settings =
          FirebaseFirestoreSettings.Builder()
              .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
              .build()
      firestore.firestoreSettings = settings
    } catch (e: Exception) {
      // Persistence may already be enabled or unavailable
      android.util.Log.w(LOG_TAG_MAIN_ACTIVITY, LOG_MSG_FIRESTORE_PERSISTENCE_FAILED, e)
    }

    // Initialize offline state manager
    OfflineStateManager.initialize(this)

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

  // Monitor connectivity state
  val coroutineScope = rememberCoroutineScope()
  val isOnline by OfflineStateManager.isOnline.collectAsState()

  // Start monitoring connectivity changes
  LaunchedEffect(Unit) {
    if (!isInTestEnvironment) {
      coroutineScope.launch {
        OfflineStateManager.getConnectivityObserver().observe().collect { connected ->
          OfflineStateManager.setOnlineState(connected)
        }
      }
    }
  }

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
      snackbarHost = {
        // Show offline indicator when disconnected
        if (!isOnline && !isInTestEnvironment) {
          OfflineIndicator()
        }
      },
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

/** Composable that displays an offline mode indicator at the top of the screen. */
@Composable
fun OfflineIndicator() {
  var isVisible by remember { mutableStateOf(true) }

  LaunchedEffect(Unit) {
    while (true) {
      isVisible = true
      kotlinx.coroutines.delay(OFFLINE_INDICATOR_VISIBLE_DURATION)
      isVisible = false
      kotlinx.coroutines.delay(OFFLINE_INDICATOR_HIDDEN_DURATION)
    }
  }

  if (isVisible) {
    Snackbar(
        modifier = Modifier.padding(OFFLINE_INDICATOR_PADDING),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer) {
          Text(stringResource(R.string.offline_indicator_message))
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
  System.getProperty(MainActivity.E2E_TEST_PROPERTY) == MainActivity.E2E_TEST_VALUE ||
      System.getProperty(JAVA_CLASSPATH_PROPERTY)?.contains(ANDROID_TEST_PATH) == true ||
      try {
        Class.forName(ANDROID_JUNIT_RUNNER)
        true
      } catch (_: ClassNotFoundException) {
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
          .onFailure { android.util.Log.w(LOG_TAG_MYGARDEN, LOG_MSG_FIREBASE_AUTH_UNAVAILABLE, it) }
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
 *     - For FRIEND_REQUEST → navigate to the Friend Requests screen.
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

    when (notificationType) {
      PushNotificationsService.NOTIFICATIONS_TYPE_WATER_PLANT -> {
        actions.navTo(Screen.Garden)
      }
      PushNotificationsService.NOTIFICATIONS_TYPE_FRIEND_REQUEST -> {
        actions.navTo(Screen.FriendsRequests)
      }
    }

    intent.removeExtra(PushNotificationsService.NOTIFICATIONS_TYPE_IDENTIFIER)
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
 * Observes the list of new friend requests and displays a popup to notify the user.
 *
 * @param actions Navigation helper used to redirect the user when confirming or deleting the popup.
 * @param friendRequestPopupVM ViewModel that emits new friend requests.
 */
@Composable
private fun NewFriendRequestPopup(
    actions: NavigationActions,
    friendRequestPopupVM: FriendsRequestsPopupViewModel = viewModel(),
) {
  var currentFriendRequest by remember { mutableStateOf<FriendRequestUiModel?>(null) }

  val lifecycle = ProcessLifecycleOwner.get().lifecycle

  DisposableEffect(Unit) {
    val observer =
        object : DefaultLifecycleObserver {

          override fun onStart(owner: LifecycleOwner) {
            // App is returning to foreground → clear all queued popup data
            currentFriendRequest = null
          }
        }

    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
  }

  LaunchedEffect(Unit) {
    friendRequestPopupVM.newRequests.collect { friendRequestUIModel ->
      currentFriendRequest = friendRequestUIModel
    }
  }

  currentFriendRequest?.let {
    FriendsRequestsPopup(
        senderPseudo = it.senderPseudo,
        onDismiss = { currentFriendRequest = null },
        onConfirm = {
          actions.navTo(Screen.FriendsRequests)
          currentFriendRequest = null
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
