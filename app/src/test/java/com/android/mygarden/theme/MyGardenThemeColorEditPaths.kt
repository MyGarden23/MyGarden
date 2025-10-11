package com.android.mygarden.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.ui.theme.md_theme_dark_primary
import com.android.mygarden.ui.theme.md_theme_light_primary
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class MyGardenThemeColorEditPaths {

  @get:Rule val compose = createComposeRule()

  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun dynamicTrue_darkTrue_usesDynamicDark() {
    var primary = 0
    compose.setContent {
      MyGardenTheme(darkTheme = true, dynamicColor = true) {
        primary = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    // should not equal static dark or light constants
    Assert.assertNotEquals(md_theme_dark_primary.toArgb(), primary)
    Assert.assertNotEquals(md_theme_light_primary.toArgb(), primary)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun dynamicTrue_darkFalse_usesDynamicLight() {
    var primary = 0
    compose.setContent {
      MyGardenTheme(darkTheme = false, dynamicColor = true) {
        primary = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    Assert.assertNotEquals(md_theme_dark_primary.toArgb(), primary)
    Assert.assertNotEquals(md_theme_light_primary.toArgb(), primary)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun dynamicFalse_darkTrue_usesStaticDark() {
    var primary = 0
    compose.setContent {
      MyGardenTheme(darkTheme = true, dynamicColor = false) {
        primary = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    Assert.assertEquals(md_theme_dark_primary.toArgb(), primary)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.S])
  fun dynamicFalse_darkFalse_usesStaticLight() {
    var primary = 0
    compose.setContent {
      MyGardenTheme(darkTheme = false, dynamicColor = false) {
        primary = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    Assert.assertEquals(md_theme_light_primary.toArgb(), primary)
  }

  // ---------- SDK < S ----------

  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun preS_dynamicTrue_darkTrue_fallsBackToStaticDark() {
    var primary = 0
    compose.setContent {
      MyGardenTheme(darkTheme = true, dynamicColor = true) {
        primary = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    Assert.assertEquals(md_theme_dark_primary.toArgb(), primary)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun preS_dynamicTrue_darkFalse_fallsBackToStaticLight() {
    var primary = 0
    compose.setContent {
      MyGardenTheme(darkTheme = false, dynamicColor = true) {
        primary = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    Assert.assertEquals(md_theme_light_primary.toArgb(), primary)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun preS_dynamicFalse_darkTrue_usesStaticDark() {
    var primary = 0
    compose.setContent {
      MyGardenTheme(darkTheme = true, dynamicColor = false) {
        primary = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    Assert.assertEquals(md_theme_dark_primary.toArgb(), primary)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.R])
  fun preS_dynamicFalse_darkFalse_usesStaticLight() {
    var primary = 0
    compose.setContent {
      MyGardenTheme(darkTheme = false, dynamicColor = false) {
        primary = MaterialTheme.colorScheme.primary.toArgb()
      }
    }
    Assert.assertEquals(md_theme_light_primary.toArgb(), primary)
  }
}
