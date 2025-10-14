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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.BottomBar
import com.android.mygarden.ui.navigation.NavigationActions
import com.android.mygarden.ui.navigation.Page
import com.android.mygarden.ui.navigation.Screen
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
  // Determine where to start: if the user is logged in, skip Sign-In
  val user = remember {
    runCatching { FirebaseAuth.getInstance().currentUser }
        .onFailure {
          android.util.Log.w("MyGarden", "FirebaseAuth unavailable; defaulting to Auth", it)
        }
        .getOrNull()
  }
  val startDestination = if (user == null) Screen.Auth.route else Screen.Camera.route
  // Observe the current destination so we can update UI accordingly
  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  // Helper functions to map the route string to our sealed classes
  val currentScreen = remember(currentRoute) { currentRoute?.let { routeToScreen(it) } }
  val selectedPage = remember(currentRoute) { currentRoute?.let { routeToPage(it) } }

  Scaffold(
      bottomBar = {
        // only show the bottom for Camera and Profile for now, add screen if needed
        if (currentScreen == Screen.Camera || currentScreen == Screen.Profile) {
          BottomBar(
              selectedPage = selectedPage ?: Page.Camera,
              onSelect = { actions.navToTopLevel(it.destination) })
        }
      }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
          AppNavHost(
              navController = navController,
              startDestination = startDestination,
          )
        }
      }
}

// Maps the current route (String) to its Screen object
private fun routeToScreen(route: String): Screen? =
    when (route) {
      Screen.Auth.route -> Screen.Auth
      Screen.Camera.route -> Screen.Camera
      Screen.PlantView.route -> Screen.PlantView
      Screen.Profile.route -> Screen.Profile
      else -> null
    }
// Maps the current route (String) to its Page (used for bottom bar selection)
private fun routeToPage(route: String): Page? =
    when (route) {
      Screen.Camera.route -> Page.Camera
      Screen.Profile.route -> Page.Profile
      else -> null
    }
