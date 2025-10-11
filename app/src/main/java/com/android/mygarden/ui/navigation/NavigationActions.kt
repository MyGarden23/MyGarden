package com.android.mygarden.ui.navigation

import androidx.navigation.NavHostController

/**
 * Representation of a specific screen of the app
 *
 * @param route the route of the screen
 * @param name the name of the screen
 * @param isTopLevel whether the screen is a core screen that can be accessible from the bottom bar
 *   or not
 */
sealed class Screen(val route: String, val name: String, val isTopLevel: Boolean = false) {
  object Camera : Screen(route = "camera", name = "Camera", isTopLevel = true)
  // These 2 screens will be useful for next sprints
  // object SignIn: Screen(route = "sign_in", name = "Sign In", isTopLevel = true)
  // object PlantView: Screen(route = "plant_view", name = "Plant View")
  object Profile : Screen(route = "profile", name = "Profile", isTopLevel = true)
}

/**
 * Class that represent the different possible navigations through the screens
 *
 * @param controller the NavHostController used to navigate
 */
class NavigationActions(val controller: NavHostController) {
  /**
   * Navigate to the given screen
   *
   * @param destination the screen to navigate to
   */
  fun navTo(destination: Screen) {
    // ensures the destination is not top level and the current route is not the destination route
    // meaning that a navigation actually happens if we don't try to go in a top level destination
    // from itself
    if (!(destination.isTopLevel &&
        (controller.currentDestination?.route ?: "") == destination.route)) {
      controller.navigate(destination.route) {
        // ensures a top level destination appears only once in the stack
        if (destination.isTopLevel) {
          launchSingleTop = true
          popUpTo(destination.route) { inclusive = true }
        }
      }
    }
  }

  /* TODO: uncomment this function when a back button is actually implemented
  /** Navigate back to previous screen. */
  fun navBack() {
    controller.popBackStack()
  }
   */
}
