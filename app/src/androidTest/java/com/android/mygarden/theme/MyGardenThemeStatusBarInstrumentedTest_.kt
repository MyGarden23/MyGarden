package com.android.mygarden.theme

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.android.mygarden.ui.theme.MyGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyGardenThemeStatusBarInstrumentedTest_ {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  // 9) Not in edit mode (normal runtime) + dark theme: status bar color should be set to primary
  @Test
  fun SideEffect_AppliesStatusBar_Dark() {
    val activity = compose.activity
    var expected = 0

    compose.setContent {
      MyGardenTheme(darkTheme = true, dynamicColor = false) {
        // Force material to resolve so we can read the color
        expected = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    compose.waitForIdle()
    assertEquals(expected, activity.window.statusBarColor)
  }

  // 10) Not in edit mode + light theme: status bar color should be set to primary
  @Test
  fun SideEffect_AppliesStatusBar_Light() {
    val activity = compose.activity
    var expected = 0

    compose.setContent {
      MyGardenTheme(darkTheme = false, dynamicColor = false) {
        expected = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    compose.waitForIdle()
    assertEquals(expected, activity.window.statusBarColor)
  }
}
