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

  object NewProfile : Screen(route = "new_profile", name = "New Profile")

  data class EditPlant(val ownedPlantId: String) :
      Screen(route = "edit_plant/${ownedPlantId}", name = "Edit Plant") {
    companion object {
      const val route = "edit_plant/{ownedPlantId}"
    }
  }

  object Garden : Screen(route = "garden", name = "Garden", isTopLevel = true)

  object ChooseAvatar : Screen(route = "choose_avatar", name = "Choose Avatar")
}
