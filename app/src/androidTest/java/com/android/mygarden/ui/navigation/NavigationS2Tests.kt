package com.android.mygarden.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val DEFAULT_WAIT_MS = 5_000L

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
    compose.runOnIdle { navController.navigate(Screen.PlantInfo.route) }
    compose.runOnIdle { assertEquals(Screen.PlantInfo.route, currentRoute.value) }
  }

  @Test
  fun back_from_plantView_returns_to_camera() {
    setApp(Screen.Camera.route)

    // Camera -> Plant View
    compose.runOnIdle { navController.navigate(Screen.PlantInfo.route) }
    waitForRoute(Screen.PlantInfo.route)

    // popBackStack -> Camera
    compose.runOnIdle { navController.popBackStack() }
    waitForRoute(Screen.Camera.route)
    compose.runOnIdle { assertEquals(Screen.Camera.route, currentRoute.value) }
  }

  @Test
  fun can_reach_garden_destination() {
    setApp(Screen.Camera.route)
    compose.runOnIdle { navController.navigate(Screen.Garden.route) }
    compose.runOnIdle { assertEquals(Screen.Garden.route, currentRoute.value) }
  }

  @Test
  fun onSignedIn_from_auth_navigates_to_camera() {
    // AppNavHost wires SignInScreen.onSignedIn -> navigate(Camera)
    setApp(Screen.Auth.route)

    // Emulate the onSignedIn callback effect
    compose.runOnIdle { navController.navigate(Screen.Camera.route) }

    // Wait for recomposition / back stack update, then assert
    waitForRoute(Screen.Camera.route)
    compose.runOnIdle { assertEquals(Screen.Camera.route, currentRoute.value) }
  }

  @Test
  fun onBackPressed_from_plantView_returns_to_camera() {
    // AppNavHost wires PlantInfosScreen.onBackPressed -> navBack()
    setApp(Screen.Camera.route)

    // Camera -> PlantView (same as tapping from CameraScreen)
    compose.runOnIdle { navController.navigate(Screen.PlantInfo.route) }
    waitForRoute(Screen.PlantInfo.route)

    // Emulate the onBackPressed callback effect
    compose.runOnIdle { navController.popBackStack() }
    waitForRoute(Screen.Camera.route)

    compose.runOnIdle { assertEquals(Screen.Camera.route, currentRoute.value) }
  }

  private fun waitForRoute(expected: String, timeoutMillis: Long = DEFAULT_WAIT_MS) {
    try {
      compose.waitUntil(timeoutMillis) { currentRoute.value == expected }
    } catch (e: ComposeTimeoutException) {
      val actual = currentRoute.value
      // Optionally include the graph’s known routes (not the back stack) for hints:
      val knownRoutes =
          try {
            buildList<String?> {
              fun walk(dest: androidx.navigation.NavDestination) {
                if (dest is androidx.navigation.NavGraph) {
                  dest.iterator().forEachRemaining { walk(it) }
                } else add(dest.route)
              }
              walk(navController.graph)
            }
          } catch (_: Throwable) {
            emptyList()
          }

      throw AssertionError(
          "Timed out after $timeoutMillis ms waiting for route='$expected'. " +
              "Actual='${actual ?: "null"}'. KnownRoutes=$knownRoutes",
          e)
    }
  }
}
