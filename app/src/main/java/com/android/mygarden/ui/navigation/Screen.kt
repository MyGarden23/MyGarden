package com.android.mygarden.ui.navigation

import com.android.mygarden.R

/**
 * Representation of a specific screen of the app
 *
 * @param route the route of the screen
 * @param nameResId the name of the screen
 * @param isTopLevel whether the screen is a core screen that can be accessible from the bottom bar
 *   or not
 */
sealed class Screen(val route: String, val nameResId: Int, val isTopLevel: Boolean = false) {
  object Auth : Screen(route = "auth", nameResId = R.string.authentication_screen_title)

  object Camera :
      Screen(route = "camera", nameResId = R.string.camera_screen_title, isTopLevel = true)

  object PlantInfo : Screen(route = "plant_info", nameResId = R.string.plant_info_screen_title)

  object NewProfile : Screen(route = "new_profile", nameResId = R.string.new_profile_screen_title)

  data class EditPlant(val ownedPlantId: String, val from: String? = null) :
      Screen(route = buildRoute(ownedPlantId, from), nameResId = R.string.edit_plant_screen_title) {

    /**
     * Companion object defining constants and utilities for the EditPlant screen navigation.
     *
     * Contains the base route, argument keys, and a helper function to build the complete
     * navigation route dynamically.
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

  object EditProfile :
      Screen(route = "edit_profile", nameResId = R.string.edit_profile_screen_title)

  object Garden :
      Screen(route = "garden", nameResId = R.string.garden_screen_title, isTopLevel = true)

  object ChooseAvatar :
      Screen(route = "choose_avatar", nameResId = R.string.choose_avatar_screen_title)
}
