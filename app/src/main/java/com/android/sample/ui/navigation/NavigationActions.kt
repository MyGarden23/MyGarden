package com.android.sample.ui.navigation

import androidx.navigation.NavHostController

sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevel: Boolean = false
) {
    object Camera: Screen(route = "camera", name = "Camera", isTopLevel = true)
    // These 2 screens will be useful for next sprints
    //object SignIn: Screen(route = "sign_in", name = "Sign In", isTopLevel = true)
    //object PlantView: Screen(route = "plant_view", name = "Plant View")
    object Profile: Screen(route = "profile", name = "Profile", isTopLevel = true)
}

class NavigationActions(
    val controller: NavHostController
) {
    /**
     * Navigate to the given screen
     *
     * @param destination the screen to navigate to
     */
    fun navTo(destination: Screen) {
        if (!(destination.isTopLevel && (controller.currentDestination?.route ?: "") == destination.route)) {
            controller.navigate(destination.route) {
                if (destination.isTopLevel) {
                    launchSingleTop = true
                    popUpTo(destination.route) {inclusive = true}
                }
            }
        }
    }

    /** Navigate back to previous screen. */
    fun navBack() {
        controller.popBackStack()
    }
}