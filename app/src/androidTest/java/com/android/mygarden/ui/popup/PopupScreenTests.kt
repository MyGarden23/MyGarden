package com.android.mygarden.ui.popup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PopupScreenTests {

  @get:Rule val testRule = createComposeRule()

  /** Create the Pop-up Screen with default [plantName] and lambdas */
  @Before
  fun setContent() {
    testRule.setContent { Popup(plantName = "Bertrand", onDismiss = {}, onConfirm = {}) }
  }

  /** Tests that all components of the pop-up are displayed */
  @Test
  fun allDisplayed() {
    testRule.onNodeWithTag(PopupScreenTestTags.CARD).assertIsDisplayed()
    testRule.onNodeWithTag(PopupScreenTestTags.TITLE).assertIsDisplayed()
    testRule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsDisplayed()
    testRule.onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsDisplayed()
  }

  /** Tests that all buttons present are clickable */
  @Test
  fun buttonsAreClickable() {
    testRule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsEnabled()
    testRule.onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsEnabled()
  }
}
