package com.android.sample.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.sample.ui.theme.MyGardenTheme
import com.android.sample.ui.theme.md_theme_dark_background
import com.android.sample.ui.theme.md_theme_dark_error
import com.android.sample.ui.theme.md_theme_dark_errorContainer
import com.android.sample.ui.theme.md_theme_dark_inverseOnSurface
import com.android.sample.ui.theme.md_theme_dark_inversePrimary
import com.android.sample.ui.theme.md_theme_dark_inverseSurface
import com.android.sample.ui.theme.md_theme_dark_onBackground
import com.android.sample.ui.theme.md_theme_dark_onError
import com.android.sample.ui.theme.md_theme_dark_onErrorContainer
import com.android.sample.ui.theme.md_theme_dark_onPrimary
import com.android.sample.ui.theme.md_theme_dark_onPrimaryContainer
import com.android.sample.ui.theme.md_theme_dark_onSecondary
import com.android.sample.ui.theme.md_theme_dark_onSecondaryContainer
import com.android.sample.ui.theme.md_theme_dark_onSurface
import com.android.sample.ui.theme.md_theme_dark_onSurfaceVariant
import com.android.sample.ui.theme.md_theme_dark_onTertiary
import com.android.sample.ui.theme.md_theme_dark_onTertiaryContainer
import com.android.sample.ui.theme.md_theme_dark_outline
import com.android.sample.ui.theme.md_theme_dark_outlineVariant
import com.android.sample.ui.theme.md_theme_dark_primary
import com.android.sample.ui.theme.md_theme_dark_primaryContainer
import com.android.sample.ui.theme.md_theme_dark_scrim
import com.android.sample.ui.theme.md_theme_dark_secondary
import com.android.sample.ui.theme.md_theme_dark_secondaryContainer
import com.android.sample.ui.theme.md_theme_dark_surface
import com.android.sample.ui.theme.md_theme_dark_surfaceTint
import com.android.sample.ui.theme.md_theme_dark_surfaceVariant
import com.android.sample.ui.theme.md_theme_dark_tertiary
import com.android.sample.ui.theme.md_theme_dark_tertiaryContainer
import com.android.sample.ui.theme.md_theme_light_background
import com.android.sample.ui.theme.md_theme_light_error
import com.android.sample.ui.theme.md_theme_light_errorContainer
import com.android.sample.ui.theme.md_theme_light_inverseOnSurface
import com.android.sample.ui.theme.md_theme_light_inversePrimary
import com.android.sample.ui.theme.md_theme_light_inverseSurface
import com.android.sample.ui.theme.md_theme_light_onBackground
import com.android.sample.ui.theme.md_theme_light_onError
import com.android.sample.ui.theme.md_theme_light_onErrorContainer
import com.android.sample.ui.theme.md_theme_light_onPrimary
import com.android.sample.ui.theme.md_theme_light_onPrimaryContainer
import com.android.sample.ui.theme.md_theme_light_onSecondary
import com.android.sample.ui.theme.md_theme_light_onSecondaryContainer
import com.android.sample.ui.theme.md_theme_light_onSurface
import com.android.sample.ui.theme.md_theme_light_onSurfaceVariant
import com.android.sample.ui.theme.md_theme_light_onTertiary
import com.android.sample.ui.theme.md_theme_light_onTertiaryContainer
import com.android.sample.ui.theme.md_theme_light_outline
import com.android.sample.ui.theme.md_theme_light_outlineVariant
import com.android.sample.ui.theme.md_theme_light_primary
import com.android.sample.ui.theme.md_theme_light_primaryContainer
import com.android.sample.ui.theme.md_theme_light_scrim
import com.android.sample.ui.theme.md_theme_light_secondary
import com.android.sample.ui.theme.md_theme_light_secondaryContainer
import com.android.sample.ui.theme.md_theme_light_surface
import com.android.sample.ui.theme.md_theme_light_surfaceTint
import com.android.sample.ui.theme.md_theme_light_surfaceVariant
import com.android.sample.ui.theme.md_theme_light_tertiary
import com.android.sample.ui.theme.md_theme_light_tertiaryContainer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyGardenThemeTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun myGardenTheme_light_uses_expected_colors() {
    lateinit var scheme: ColorScheme

    composeRule.setContent {
      MyGardenTheme(darkTheme = false, dynamicColor = false) { scheme = MaterialTheme.colorScheme }
    }
    composeRule.waitForIdle()

    with(scheme) {
      // Primary
      assertEquals(md_theme_light_primary, primary)
      assertEquals(md_theme_light_onPrimary, onPrimary)
      assertEquals(md_theme_light_primaryContainer, primaryContainer)
      assertEquals(md_theme_light_onPrimaryContainer, onPrimaryContainer)

      // Secondary
      assertEquals(md_theme_light_secondary, secondary)
      assertEquals(md_theme_light_onSecondary, onSecondary)
      assertEquals(md_theme_light_secondaryContainer, secondaryContainer)
      assertEquals(md_theme_light_onSecondaryContainer, onSecondaryContainer)

      // Tertiary
      assertEquals(md_theme_light_tertiary, tertiary)
      assertEquals(md_theme_light_onTertiary, onTertiary)
      assertEquals(md_theme_light_tertiaryContainer, tertiaryContainer)
      assertEquals(md_theme_light_onTertiaryContainer, onTertiaryContainer)

      // Error
      assertEquals(md_theme_light_error, error)
      assertEquals(md_theme_light_onError, onError)
      assertEquals(md_theme_light_errorContainer, errorContainer)
      assertEquals(md_theme_light_onErrorContainer, onErrorContainer)

      // Neutrals / Surfaces / Background
      assertEquals(md_theme_light_outline, outline)
      assertEquals(md_theme_light_background, background)
      assertEquals(md_theme_light_onBackground, onBackground)
      assertEquals(md_theme_light_surface, surface)
      assertEquals(md_theme_light_onSurface, onSurface)
      assertEquals(md_theme_light_surfaceVariant, surfaceVariant)
      assertEquals(md_theme_light_onSurfaceVariant, onSurfaceVariant)

      // Inverse & misc
      assertEquals(md_theme_light_inverseSurface, inverseSurface)
      assertEquals(md_theme_light_inverseOnSurface, inverseOnSurface)
      assertEquals(md_theme_light_inversePrimary, inversePrimary)
      assertEquals(md_theme_light_surfaceTint, surfaceTint)
      assertEquals(md_theme_light_outlineVariant, outlineVariant)
      assertEquals(md_theme_light_scrim, scrim)
    }
  }

  @Test
  fun myGardenTheme_dark_uses_expected_colors() {
    lateinit var scheme: ColorScheme

    composeRule.setContent {
      MyGardenTheme(darkTheme = true, dynamicColor = false) { scheme = MaterialTheme.colorScheme }
    }
    composeRule.waitForIdle()

    with(scheme) {
      // Primary
      assertEquals(md_theme_dark_primary, primary)
      assertEquals(md_theme_dark_onPrimary, onPrimary)
      assertEquals(md_theme_dark_primaryContainer, primaryContainer)
      assertEquals(md_theme_dark_onPrimaryContainer, onPrimaryContainer)

      // Secondary
      assertEquals(md_theme_dark_secondary, secondary)
      assertEquals(md_theme_dark_onSecondary, onSecondary)
      assertEquals(md_theme_dark_secondaryContainer, secondaryContainer)
      assertEquals(md_theme_dark_onSecondaryContainer, onSecondaryContainer)

      // Tertiary
      assertEquals(md_theme_dark_tertiary, tertiary)
      assertEquals(md_theme_dark_onTertiary, onTertiary)
      assertEquals(md_theme_dark_tertiaryContainer, tertiaryContainer)
      assertEquals(md_theme_dark_onTertiaryContainer, onTertiaryContainer)

      // Error
      assertEquals(md_theme_dark_error, error)
      assertEquals(md_theme_dark_onError, onError)
      assertEquals(md_theme_dark_errorContainer, errorContainer)
      assertEquals(md_theme_dark_onErrorContainer, onErrorContainer)

      // Neutrals / Surfaces / Background
      assertEquals(md_theme_dark_outline, outline)
      assertEquals(md_theme_dark_background, background)
      assertEquals(md_theme_dark_onBackground, onBackground)
      assertEquals(md_theme_dark_surface, surface)
      assertEquals(md_theme_dark_onSurface, onSurface)
      assertEquals(md_theme_dark_surfaceVariant, surfaceVariant)
      assertEquals(md_theme_dark_onSurfaceVariant, onSurfaceVariant)

      // Inverse & misc
      assertEquals(md_theme_dark_inverseSurface, inverseSurface)
      assertEquals(md_theme_dark_inverseOnSurface, inverseOnSurface)
      assertEquals(md_theme_dark_inversePrimary, inversePrimary)
      assertEquals(md_theme_dark_surfaceTint, surfaceTint)
      assertEquals(md_theme_dark_outlineVariant, outlineVariant)
      assertEquals(md_theme_dark_scrim, scrim)
    }
  }
}
