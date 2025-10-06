@file:Suppress("TestFunctionName")

package com.android.sample.theme

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.sample.ui.theme.MyGardenTheme
import com.android.sample.ui.theme.md_theme_dark_primary
import com.android.sample.ui.theme.md_theme_light_primary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric.buildActivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class MyGardenThemeColorSchemeTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  // --- Helpers ---

  private class EditModeView(private val activity: Activity, private val editMode: Boolean) :
      View(activity) {
    override fun isInEditMode(): Boolean = editMode
  }

  @Composable
  private fun withEditModeView(editMode: Boolean, content: @Composable () -> Unit) {
    val activity = buildActivity(ComponentActivity::class.java).setup().get()
    CompositionLocalProvider(LocalView provides EditModeView(activity, editMode)) { content() }
  }

  // -------- 6 color-scheme paths --------

  // 1) API >= S, dynamicColor = true, darkTheme = true -> dynamicDarkColorScheme
  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun Dynamic_API31_Dark_UsesDynamicDark() {
    var primary = 0
    compose.setContent {
      withEditModeView(false) {
        MyGardenTheme(darkTheme = true, dynamicColor = true) {
          primary = MaterialTheme.colorScheme.primary.toArgb()
        }
      }
    }
    compose.waitForIdle()
    // Dynamic primary should differ from the static MD3 palette in most themes, but we check that
    // it is *not* the known static light/dark primaries to ensure the dynamic branch was taken.
    // (Keeps the test robust across devices/themes.)
    assert(primary != md_theme_dark_primary.toArgb())
    assert(primary != md_theme_light_primary.toArgb())
  }

  // 2) API >= S, dynamicColor = true, darkTheme = false -> dynamicLightColorScheme
  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun Dynamic_API31_Light_UsesDynamicLight() {
    var primary = 0
    compose.setContent {
      withEditModeView(false) {
        MyGardenTheme(darkTheme = false, dynamicColor = true) {
          primary = MaterialTheme.colorScheme.primary.toArgb()
        }
      }
    }
    compose.waitForIdle()
    assert(primary != md_theme_dark_primary.toArgb())
    assert(primary != md_theme_light_primary.toArgb())
  }

  // 3) dynamicColor = false, darkTheme = true -> DarkColorScheme
  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun Static_Dark_UsesDarkPalette() {
    var primary = 0
    compose.setContent {
      withEditModeView(false) {
        MyGardenTheme(darkTheme = true, dynamicColor = false) {
          primary = MaterialTheme.colorScheme.primary.toArgb()
        }
      }
    }
    compose.waitForIdle()
    assertEquals(md_theme_dark_primary.toArgb(), primary)
  }

  // 4) dynamicColor = false, darkTheme = false -> LightColorScheme
  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun Static_Light_UsesLightPalette() {
    var primary = 0
    compose.setContent {
      withEditModeView(false) {
        MyGardenTheme(darkTheme = false, dynamicColor = false) {
          primary = MaterialTheme.colorScheme.primary.toArgb()
        }
      }
    }
    compose.waitForIdle()
    assertEquals(md_theme_light_primary.toArgb(), primary)
  }

  // 5) API < S, dynamicColor = true, darkTheme = true -> falls back to DarkColorScheme
  @Test
  @Config(sdk = [Build.VERSION_CODES.R]) // 30
  fun PreS_DynamicRequested_Dark_FallsBackToDark() {
    var primary = 0
    compose.setContent {
      withEditModeView(false) {
        MyGardenTheme(darkTheme = true, dynamicColor = true) {
          primary = MaterialTheme.colorScheme.primary.toArgb()
        }
      }
    }
    compose.waitForIdle()
    assertEquals(md_theme_dark_primary.toArgb(), primary)
  }

  // 6) API < S, dynamicColor = true, darkTheme = false -> falls back to LightColorScheme
  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun PreS_DynamicRequested_Light_FallsBackToLight() {
    var primary = 0
    compose.setContent {
      withEditModeView(false) {
        MyGardenTheme(darkTheme = false, dynamicColor = true) {
          primary = MaterialTheme.colorScheme.primary.toArgb()
        }
      }
    }
    compose.waitForIdle()
    assertEquals(md_theme_light_primary.toArgb(), primary)
  }

  // -------- 2 paths: edit mode = true should SKIP SideEffect --------

  // 7) Edit mode true + dark theme: status bar should NOT be set
  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun EditModeTrue_Dark_SideEffectSkipped() {
    val activity = buildActivity(ComponentActivity::class.java).setup().get()
    val original = activity.window.statusBarColor

    compose.setContent {
      // LocalView returns a view reporting edit mode = true
      CompositionLocalProvider(LocalView provides EditModeView(activity, true)) {
        MyGardenTheme(darkTheme = true, dynamicColor = false) { /* no-op */}
      }
    }
    compose.waitForIdle()
    // unchanged means SideEffect block was skipped
    assertEquals(original, activity.window.statusBarColor)
  }

  // 8) Edit mode true + light theme: still SKIP SideEffect
  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun EditModeTrue_Light_SideEffectSkipped() {
    val activity = buildActivity(ComponentActivity::class.java).setup().get()
    val original = activity.window.statusBarColor

    compose.setContent {
      CompositionLocalProvider(LocalView provides EditModeView(activity, true)) {
        MyGardenTheme(darkTheme = false, dynamicColor = false) { /* no-op */}
      }
    }
    compose.waitForIdle()
    assertEquals(original, activity.window.statusBarColor)
  }
}
