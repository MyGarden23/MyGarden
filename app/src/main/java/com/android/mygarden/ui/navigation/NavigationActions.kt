package com.android.mygarden.ui.navigation

import android.util.Log
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Class that represents the different possible navigations through the screens.
 *
 * @param controller the NavHostController used to navigate
 */
class NavigationActions(private val controller: NavHostController) {

  /**
   * Navigate to the given screen.
   * - If it's a top-level screen, delegates to [navToTopLevel] for proper tab behavior.
   * - Otherwise, performs a simple navigate with singleTop and optional restoreState.
   */
  fun navTo(destination: Screen) {
    if (destination == null) {
      Log.w("NavigationActions", "navTo() called with null destination — ignored")
      return
    }
    if (destination.isTopLevel) {
      navToTopLevel(destination)
      return
    }
    controller.navigate(destination.route) {
      launchSingleTop = true
      // Typically we want to restore state when navigating within the main graph,
      // but avoid restoring when going to the auth flow.
      if (destination != Screen.Auth) {
        restoreState = true
      }
    }
  }

  /**
   * Navigate to a top-level destination (e.g., bottom bar tab). Ensures:
   * - Only one instance (launchSingleTop)
   * - State is saved/restored when switching between tabs
   * - We pop up to the graph's start destination instead of the destination itself
   */
  fun navToTopLevel(destination: Screen) {
    val current = currentRoute()
    if (current == destination.route) return

    controller.navigate(destination.route) {
      launchSingleTop = true
      // Don't restore state to avoid navigation issues with temporary screens
      // restoreState = true  // Commented out to prevent PlantView restoration
      popUpTo(controller.graph.findStartDestination().id) { saveState = true }
    }
  }

  /** Navigate back to previous screen. */
  fun navBack() {
    controller.popBackStack()
  }

  /** Helper to fetch the current route safely. */
  private fun currentRoute(): String? = controller.currentDestination?.route
}
