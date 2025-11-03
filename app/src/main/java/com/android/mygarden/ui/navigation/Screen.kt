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

  object PlantInfo : Screen(route = "plant_info", name = "Plant Info")

  object NewProfile : Screen(route = "new_profile", name = "New Profile")

  data class EditPlant(val ownedPlantId: String, val from: String? = null) :
      Screen(route = buildRoute(ownedPlantId, from), name = "Edit Plant") {

    /**
     * Companion object defining constants and utilities for the EditPlant screen navigation.
     *
     * Contains the base route, argument keys, and a helper function to build
     * the complete navigation route dynamically.
     *
     * @property BASE The base route used for the EditPlant screen.
     * @property ARG_ID The key for the owned plant ID argument.
     * @property ARG_FROM The key for the source screen argument.
     * @property route The full navigation route pattern with arguments.
     */
    companion object {
      const val BASE = "edit_plant"
      const val ARG_ID = "ownedPlantId"
      const val ARG_FROM = "from"
      const val route = "$BASE/{$ARG_ID}?$ARG_FROM={$ARG_FROM}"

      /**
       * Builds the route for the EditPlant screen.
       *
       * @param ownedPlantId The ID of the owned plant to be edited.
       * @param from The screen from which the EditPlant screen was opened.
       */
      fun buildRoute(ownedPlantId: String, from: String? = null): String {
        return if (from != null && from.isNotBlank()) {
          "$BASE/$ownedPlantId?$ARG_FROM=$from"
        } else {
          "$BASE/$ownedPlantId"
        }
      }
    }
  }

  object EditProfile : Screen(route = "edit_profile", name = "New Profile")

  object Garden : Screen(route = "garden", name = "Garden", isTopLevel = true)

  object ChooseAvatar : Screen(route = "choose_avatar", name = "Choose Avatar")
}
