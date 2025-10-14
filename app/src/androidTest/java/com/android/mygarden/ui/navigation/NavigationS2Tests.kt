package com.android.mygarden.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationS2Tests {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  // We’ll stash these per-test (initialized inside setContent)
  private lateinit var navController: NavHostController
  private lateinit var currentRoute: MutableState<String?>

  /** Helper to compose AppNavHost once per test with a given start route. */
  private fun setApp(startRoute: String) {
    compose.setContent {
      navController = rememberNavController()

      // Observe current route via the back stack
      val backEntry by navController.currentBackStackEntryAsState()
      currentRoute = remember { mutableStateOf<String?>(null) }
      LaunchedEffect(backEntry) { currentRoute.value = backEntry?.destination?.route }

      AppNavHost(navController = navController, startDestination = startRoute)
    }
    compose.waitForIdle()
  }

  @Test
  fun starts_at_Auth_when_requested() {
    setApp(Screen.Auth.route)
    compose.runOnIdle { assertEquals(Screen.Auth.route, currentRoute.value) }
  }

  @Test
  fun starts_at_Camera_when_requested() {
    setApp(Screen.Camera.route)
    compose.runOnIdle { assertEquals(Screen.Camera.route, currentRoute.value) }
  }

  @Test
  fun camera_navigates_to_plantView() {
    setApp(Screen.Camera.route)
    compose.runOnIdle { navController.navigate(Screen.PlantView.route) }
    compose.runOnIdle { assertEquals(Screen.PlantView.route, currentRoute.value) }
  }

  @Test
  fun back_from_plantView_returns_to_camera() {
    setApp(Screen.Camera.route)

    // Camera -> Plant View
    compose.runOnIdle { navController.navigate(Screen.PlantView.route) }
    waitForRoute(Screen.PlantView.route)

    // popBackStack -> Camera
    compose.runOnIdle { navController.popBackStack() }
    waitForRoute(Screen.Camera.route)
    compose.runOnIdle { assertEquals(Screen.Camera.route, currentRoute.value) }
  }

  @Test
  fun can_reach_profile_destination() {
    setApp(Screen.Camera.route)
    compose.runOnIdle { navController.navigate(Screen.Profile.route) }
    compose.runOnIdle { assertEquals(Screen.Profile.route, currentRoute.value) }
  }

  private fun waitForRoute(expected: String, timeoutMillis: Long = 5_000) {
    compose.waitUntil(timeoutMillis) { currentRoute.value == expected }
  }

    @Test
    fun onSignedIn_from_auth_navigates_to_camera() {
        // AppNavHost wires SignInScreen.onSignedIn -> navigate(Camera)
        setApp(Screen.Auth.route)

        // Emulate the onSignedIn callback effect
        compose.runOnIdle {
            navController.navigate(Screen.Camera.route)
        }

        // Wait for recomposition / back stack update, then assert
        waitForRoute(Screen.Camera.route)
        compose.runOnIdle {
            assertEquals(Screen.Camera.route, currentRoute.value)
        }
    }

    @Test
    fun onBackPressed_from_plantView_returns_to_camera() {
        // AppNavHost wires PlantInfosScreen.onBackPressed -> navBack()
        setApp(Screen.Camera.route)

        // Camera -> PlantView (same as tapping from CameraScreen)
        compose.runOnIdle {
            navController.navigate(Screen.PlantView.route)
        }
        waitForRoute(Screen.PlantView.route)

        // Emulate the onBackPressed callback effect
        compose.runOnIdle {
            navController.popBackStack()
        }
        waitForRoute(Screen.Camera.route)

        compose.runOnIdle {
            assertEquals(Screen.Camera.route, currentRoute.value)
        }
    }
}
