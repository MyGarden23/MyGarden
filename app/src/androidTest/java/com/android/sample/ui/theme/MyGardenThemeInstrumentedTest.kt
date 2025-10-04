package com.android.sample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MyGardenThemeInstrumentedTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun myGardenTheme_light_uses_expected_colors() {
    var primary: Color? = null
    var onPrimary: Color? = null
    var background: Color? = null

    composeRule.setContent {
      MyGardenTheme(darkTheme = false, dynamicColor = false) {
        primary = MaterialTheme.colorScheme.primary
        onPrimary = MaterialTheme.colorScheme.onPrimary
        background = MaterialTheme.colorScheme.background
      }
    }
    composeRule.waitForIdle()

    assertEquals(md_theme_light_primary, primary)
    assertEquals(md_theme_light_onPrimary, onPrimary)
    assertEquals(md_theme_light_background, background)
  }

  @Test
  fun myGardenTheme_dark_uses_expected_colors() {
    var primary: Color? = null
    var container: Color? = null

    composeRule.setContent {
      MyGardenTheme(darkTheme = true, dynamicColor = false) {
        primary = MaterialTheme.colorScheme.primary
        container = MaterialTheme.colorScheme.primaryContainer
      }
    }
    composeRule.waitForIdle()

    assertEquals(md_theme_dark_primary, primary)
    assertEquals(md_theme_dark_primaryContainer, container)
  }
}
