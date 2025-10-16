package com.android.mygarden.ui.navigation

/**
 * Representation of a specific screen of the app
 *
 * @param route the route of the screen
 * @param name the name of the screen
 * @param isTopLevel whether the screen is a core screen that can be accessible from the bottom bar
 *   or not
 */
sealed class Screen(val route: String, val name: String, val isTopLevel: Boolean = false) {
  object Auth : Screen(route = "auth", name = "Authentication")

  object Camera : Screen(route = "camera", name = "Camera", isTopLevel = true)

  object PlantView : Screen(route = "plant_view", name = "Plant View")

  object Profile : Screen(route = "profile", name = "Profile", isTopLevel = true)

  object EditPlant : Screen(route = "edit_plant", name = "Edit Plant")

  object Garden : Screen(route = "garden", name = "Garden", isTopLevel = true)
}
