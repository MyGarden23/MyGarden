package com.android.mygarden.theme

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mygarden.ui.theme.MyGardenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MyGardenThemeStatusBarInstrumentedTest {

  @get:Rule val compose = createComposeRule()

  // 9) Not in edit mode (normal runtime) + dark theme: status bar color should be set to primary
  @Test
  fun SideEffect_AppliesStatusBar_Dark() {
    val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    var expected = 0

    activity.setContent {
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
    val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
    var expected = 0

    activity.setContent {
      MyGardenTheme(darkTheme = false, dynamicColor = false) {
        expected = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    compose.waitForIdle()
    assertEquals(expected, activity.window.statusBarColor)
  }
}
