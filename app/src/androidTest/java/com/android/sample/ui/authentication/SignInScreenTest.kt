package com.android.sample.ui.authentication

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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

    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON).isDisplayed()
  }

  @Test
  fun appLogo_isDisplay() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO).isDisplayed()
  }

  @Test
  fun background_isDisplay() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_BACKGROUND).isDisplayed()
  }

  @Test
  fun googleSignInButton_clicking_works() {
    var wasClicked = false

    composeTestRule.setContent { SignInScreen(onSignInClick = { wasClicked = true }) }
    composeTestRule.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON).performClick()

    assert(wasClicked)
  }
}
