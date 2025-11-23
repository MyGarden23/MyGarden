package com.android.mygarden.ui.navigation

/**
 * Maps a route string to its corresponding [Screen] instance.
 *
 * This centralizes the logic for converting navigation routes into strongly typed [Screen] objects
 * so that UI layers like `MainActivity` do not need to know about the mapping details.
 */
fun routeToScreen(route: String): Screen? =
    when (route) {
      Screen.Auth.route -> Screen.Auth
      Screen.Camera.route -> Screen.Camera
      Screen.PlantInfo.route -> Screen.PlantInfo
      Screen.PlantInfoFromGarden.route -> Screen.PlantInfoFromGarden
      Screen.PlantInfoFromCamera.route -> Screen.PlantInfoFromCamera
      Screen.NewProfile.route -> Screen.NewProfile
      Screen.Garden.route -> Screen.Garden
      Screen.Feed.route -> Screen.Feed
      Screen.ChooseAvatar.route -> Screen.ChooseAvatar
      else -> {
        if (route.startsWith(Screen.EditPlant.BASE)) {
          val ownedPlantId = route.removePrefix("${Screen.EditPlant.BASE}/")
          Screen.EditPlant(ownedPlantId)
        } else {
          null
        }
      }
    }

/** Maps a route string to its corresponding [Page] for bottom bar selection. */
fun routeToPage(route: String): Page? =
    when (route) {
      Screen.Camera.route -> Page.Camera
      Screen.Garden.route -> Page.Garden
      Screen.Feed.route -> Page.Feed
      else -> null
    }
