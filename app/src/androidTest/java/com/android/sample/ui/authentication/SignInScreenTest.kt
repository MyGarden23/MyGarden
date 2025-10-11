package com.android.sample.ui.authentication

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignInScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun signInButton_isDisplay() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun appLogo_isDisplay() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO).assertIsDisplayed()
  }

  @Test
  fun background_isDisplay() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_BACKGROUND)
        .assertIsDisplayed()
  }

  @Test
  fun shows_dark_logo_in_dark_mode() {
    // Set the dark mode
    composeTestRule.setContent { SignInScreen(isDarkTheme = true) }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO)
        .assert(SemanticsMatcher.expectValue(LogoResNameKey, "app_logo_dark"))
  }

  @Test
  fun shows_light_logo_in_light_mode() {
    // Set the light mode
    composeTestRule.setContent { SignInScreen(isDarkTheme = false) }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO)
        .assert(SemanticsMatcher.expectValue(LogoResNameKey, "app_logo_light"))
  }
}
