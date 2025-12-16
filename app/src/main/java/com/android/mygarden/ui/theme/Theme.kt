package com.android.mygarden.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme =
    lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        onError = md_theme_light_onError,
        errorContainer = md_theme_light_errorContainer,
        onErrorContainer = md_theme_light_onErrorContainer,
        outline = md_theme_light_outline,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        inverseSurface = md_theme_light_inverseSurface,
        inverseOnSurface = md_theme_light_inverseOnSurface,
        inversePrimary = md_theme_light_inversePrimary,
        surfaceTint = md_theme_light_surfaceTint,
        outlineVariant = md_theme_light_outlineVariant,
        scrim = md_theme_light_scrim,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        errorContainer = md_theme_dark_errorContainer,
        onErrorContainer = md_theme_dark_onErrorContainer,
        outline = md_theme_dark_outline,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        inverseSurface = md_theme_dark_inverseSurface,
        inverseOnSurface = md_theme_dark_inverseOnSurface,
        inversePrimary = md_theme_dark_inversePrimary,
        surfaceTint = md_theme_dark_surfaceTint,
        outlineVariant = md_theme_dark_outlineVariant,
        scrim = md_theme_dark_scrim,
    )

/** The custom colors added to the MaterialTheme ones (light mode) */
private val customColorsLight =
    CustomColors(
        wateringBlue = Color(0xFF8FB8C8),
        wateringOrange = Color(0xFFD4B896),
        redPlantCardBackground = Color(0xFFE8C4BF),
        iconsAndButtonWhiteColor = Color.White,
        acceptButtonColor = Color(0xFF7FA884),
        refuseButtonColor = Color(0xFFD49B93),
        waterActivityBlue = Color(0xFF8FB8C8),
        onWaterActivityBlue = Color(0xFF2C4F5E),
        friendActivityRed = Color(0xFFE8D9D0),
        onFriendActivityRed = Color(0xFF5A4838),
        achievementGrey = Color(0xFFD5D3CF),
        onAchievementGrey = Color(0xFF5A5653),
        notificationRed = Color(0xFFF50202))

/** The custom colors added to the MaterialTheme ones (dark mode) */
private val customColorsDark =
    CustomColors(
        wateringBlue = Color(0xFF7AB8A8),
        wateringOrange = Color(0xFFC8A882),
        redPlantCardBackground = Color(0xFF8B6B68),
        iconsAndButtonWhiteColor = Color.White,
        acceptButtonColor = Color(0xFF6B9070),
        refuseButtonColor = Color(0xFFB88078),
        waterActivityBlue = Color(0xFF8FB8C8),
        onWaterActivityBlue = Color(0xFF2C4F5E),
        friendActivityRed = Color(0xFF5A4838),
        onFriendActivityRed = Color(0xFFE0D5C8),
        achievementGrey = Color(0xFF6B6965),
        onAchievementGrey = Color(0xFFD8D6D2),
        notificationRed = Color(0xFFF50202))

/** Local slot that holds the custom colors value in the composition tree */
val LocalCustomColors = staticCompositionLocalOf {
  CustomColors(
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified,
      Color.Unspecified)
}

@Composable
fun MyGardenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }
  val customColors = if (darkTheme) customColorsDark else customColorsLight
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.primary.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }
  CompositionLocalProvider(LocalCustomColors provides customColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}

/** Extends the Material theme with the extra custom colors added */
object ExtendedTheme {
  val colors: CustomColors
    @Composable get() = LocalCustomColors.current
}
