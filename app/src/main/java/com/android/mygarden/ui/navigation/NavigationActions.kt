package com.android.mygarden.ui.navigation

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
    if (destination.isTopLevel) {
      navToTopLevel(destination)
      return
    }

    val route =
        when (destination) {
          is Screen.EditPlant ->
              Screen.EditPlant.buildRoute(destination.ownedPlantId, destination.from)
          is Screen.FriendGarden -> Screen.FriendGarden.buildRoute(destination.friendId)
          else -> destination.route
        }

    controller.navigate(route) {
      launchSingleTop = true
      // Non-top-level screens always load fresh data, so we don't restore their state
      restoreState = false
    }
  }

  /**
   * Navigate to a top-level destination (e.g., bottom bar tab). Ensures:
   * - Only one instance (launchSingleTop)
   * - State is saved/restored when switching between tabs
   * - We pop up to the graph's start destination instead of the destination itself
   */
  private fun navToTopLevel(destination: Screen) {
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

  /**
   * Navigate to the PlantInfoFromGarden screen with saved state parameters.
   *
   * @param ownedPlantId the ID of the owned plant to display
   * @param isViewMode whether the plant info should be in view-only mode (default: false)
   * @param friendId the ID of the friend whose plant is being viewed (optional)
   */
  fun navigateToPlantInfoFromGarden(
      ownedPlantId: String,
      isViewMode: Boolean = false,
      friendId: String? = null
  ) {
    controller.currentBackStackEntry
        ?.savedStateHandle
        ?.set("ownedPlantId_to_plantInfo", ownedPlantId)
    if (isViewMode) {
      controller.currentBackStackEntry?.savedStateHandle?.set("isViewMode", true)
    }
    if (friendId != null) {
      controller.currentBackStackEntry?.savedStateHandle?.set("friendId", friendId)
    }
    navTo(Screen.PlantInfoFromGarden)
  }
}
