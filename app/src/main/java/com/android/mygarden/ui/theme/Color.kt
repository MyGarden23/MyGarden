package com.android.mygarden.ui.theme

import androidx.compose.ui.graphics.Color

// Light theme
val md_theme_light_primary = Color(0xFF367444)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFF66BB6A)
val md_theme_light_onPrimaryContainer = Color(0xFF0A200A)
val md_theme_light_secondary = Color(0xFFFFB74D)
val md_theme_light_onSecondary = Color(0xFF000000)
val md_theme_light_secondaryContainer = Color(0x80FFCC80)
val md_theme_light_onSecondaryContainer = Color(0xFF000000)
val md_theme_light_tertiary = Color(0xFF8D6E63)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFD7CCC8)
val md_theme_light_onTertiaryContainer = Color(0xFF2C1B12)
val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFF9DEDC)
val md_theme_light_onErrorContainer = Color(0xFF410E0B)
val md_theme_light_outline = Color(0xFF757575)
val md_theme_light_background = Color(0xFFFCFAF1)
val md_theme_light_onBackground = Color(0xFF1C1C1C)
val md_theme_light_surface = Color(0xFFFCFAF1)
val md_theme_light_onSurface = Color(0xFF1C1C1C)
val md_theme_light_surfaceVariant = Color(0xFFE0E0E0)
val md_theme_light_onSurfaceVariant = Color(0xFF424242)
val md_theme_light_inverseSurface = Color(0xFF303030)
val md_theme_light_inverseOnSurface = Color(0xFFF5F5F5)
val md_theme_light_inversePrimary = Color(0xFF47AC74)
val md_theme_light_surfaceTint = Color(0xFF367444)
val md_theme_light_outlineVariant = Color(0xFFBDBDBD)
val md_theme_light_scrim = Color(0xFF000000)

// Dark theme
val md_theme_dark_primary = Color(0xFF47AC74)
val md_theme_dark_onPrimary = Color(0xFF003300)
val md_theme_dark_primaryContainer = Color(0xFF388E3C)
val md_theme_dark_onPrimaryContainer = Color(0xFFE8F5E9)
val md_theme_dark_secondary = Color(0xFFFFCC80)
val md_theme_dark_onSecondary = Color(0xFF402100)
val md_theme_dark_secondaryContainer = Color(0xFF504338)
val md_theme_dark_onSecondaryContainer = Color(0xFFE8DDD0)
val md_theme_dark_tertiary = Color(0xFFBCAAA4)
val md_theme_dark_onTertiary = Color(0xFF2C1B12)
val md_theme_dark_tertiaryContainer = Color(0xFF8D6E63)
val md_theme_dark_onTertiaryContainer = Color(0xFFFFF8F6)
val md_theme_dark_error = Color(0xFFF2B8B5)
val md_theme_dark_onError = Color(0xFF601410)
val md_theme_dark_errorContainer = Color(0xFF8C1D18)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_outline = Color(0xFF9E9E9E)
val md_theme_dark_background = Color(0xFF121212)
val md_theme_dark_onBackground = Color(0xFFE0E0E0)
val md_theme_dark_surface = Color(0xFF121212)
val md_theme_dark_onSurface = Color(0xFFE0E0E0)
val md_theme_dark_surfaceVariant = Color(0xFF424242)
val md_theme_dark_onSurfaceVariant = Color(0xFFBDBDBD)
val md_theme_dark_inverseSurface = Color(0xFFE0E0E0)
val md_theme_dark_inverseOnSurface = Color(0xFF303030)
val md_theme_dark_inversePrimary = Color(0xFF367444)
val md_theme_dark_surfaceTint = Color(0xFF47AC74)
val md_theme_dark_outlineVariant = Color(0xFF616161)
val md_theme_dark_scrim = Color(0xFF000000)

/**
 * Data class containing all the custom colors added to the Material theme.
 *
 * @param wateringBlue the blue color used on the watering button and bar in multiple states
 * @param wateringOrange the orange color used on the watering button and bar in multiple state
 * @param redPlantCardBackground the red color used on the background on plant cards with critical
 *   state
 * @param iconsAndButtonWhiteColor the white color that is used to put some icons and buttons in
 *   white for better UI (fixed to be the same in the light and dark mode)
 * @param acceptButtonColor the color used for the accept button in [FriendsRequestsScreen]
 * @param refuseButtonColor the color used for the refuse button in [FriendsRequestsScreen]
 * @param waterActivityBlue the blue color used for the background of the waterActivity card in
 *   [FeedScreen]
 * @param onWaterActivityBlue the blue color used for the icon of the waterActivity card in
 *   [FeedScreen]
 * @param friendActivityRed the red color used for the background of the friendActivity card in
 *   [FeedScreen]
 * @param onFriendActivityRed the red color used for the icon of the friendActivity card in
 *   [FeedScreen]
 * @param achievementGrey the grey color used for the background of the achievementActivity card in
 *   [FeedScreen]
 * @param onAchievementGrey the grey color used for the icon of the achievementActivity card in
 *   [FeedScreen]
 */
data class CustomColors(
    val wateringBlue: Color,
    val wateringOrange: Color,
    val redPlantCardBackground: Color,
    val iconsAndButtonWhiteColor: Color,
    val acceptButtonColor: Color,
    val refuseButtonColor: Color,
    val waterActivityBlue: Color,
    val onWaterActivityBlue: Color,
    val friendActivityRed: Color,
    val onFriendActivityRed: Color,
    val achievementGrey: Color,
    val onAchievementGrey: Color,
    val notificationRed: Color,
)
