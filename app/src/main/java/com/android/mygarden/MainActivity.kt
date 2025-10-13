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
  val navController = rememberNavController()
  val actions = remember(navController) { NavigationActions(navController) }

  val user = remember { FirebaseAuth.getInstance().currentUser }
  val startDestination = if (user == null) Screen.Auth.route else Screen.Camera.route

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  val currentScreen = remember(currentRoute) { currentRoute?.let { routeToScreen(it) } }
  val selectedPage = remember(currentRoute) { currentRoute?.let { routeToPage(it) } }

  Scaffold(
      bottomBar = {
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

private fun routeToScreen(route: String): Screen? =
    when (route) {
      Screen.Auth.route -> Screen.Auth
      Screen.Camera.route -> Screen.Camera
      Screen.PlantView.route -> Screen.PlantView
      Screen.Profile.route -> Screen.Profile
      else -> null
    }

private fun routeToPage(route: String): Page? =
    when (route) {
      Screen.Camera.route -> Page.Camera
      Screen.Profile.route -> Page.Profile
      else -> null
    }
