package com.android.mygarden.ui.navigation

/**
 * Maps a route string to its corresponding [Screen] instance.
 *
 * This centralizes the logic for converting navigation routes into strongly typed [Screen] objects
 * so that UI layers like `MainActivity` do not need to know about the mapping details.
 *
 * @param route The navigation route string to map.
 * @return The corresponding [Screen] instance if the route matches; otherwise `null`.
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
      Screen.FriendList.route -> Screen.FriendList
      else -> {
        if (route.startsWith(Screen.EditPlant.BASE)) {
          val ownedPlantId = route.removePrefix("${Screen.EditPlant.BASE}/")
          Screen.EditPlant(ownedPlantId)
        } else {
          null
        }
      }
    }

/**
 * Maps a route string to its corresponding [Page] for bottom bar selection.
 *
 * @param route The navigation route string to map (for example: `"camera"`, `"garden"`, or
 *   `"feed"`).
 * @return The corresponding [Page] if the route matches; otherwise `null`.
 */
fun routeToPage(route: String): Page? =
    when (route) {
      Screen.Camera.route -> Page.Camera
      Screen.Garden.route -> Page.Garden
      Screen.Feed.route -> Page.Feed
      else -> null
    }
