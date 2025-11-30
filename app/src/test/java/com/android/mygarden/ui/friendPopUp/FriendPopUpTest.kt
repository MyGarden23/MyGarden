package com.android.mygarden.ui.friendsRequests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.mygarden.ui.popup.PopupScreenTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FriendsRequestsPopupTests {

  @get:Rule val testRule = createComposeRule()

  /** Create the Pop-up Screen with default [senderName] and lambdas */
  @Before
  fun setContent() {
    testRule.setContent {
      FriendsRequestsPopup(senderName = "Pseudo", onDismiss = {}, onConfirm = {})
    }
  }

  /** Tests that all components of the pop-up are displayed */
  @Test
  fun allDisplayed() {
    testRule.onNodeWithTag(PopupScreenTestTags.CARD).assertIsDisplayed()
    testRule.onNodeWithTag(PopupScreenTestTags.TITLE).assertIsDisplayed()
    testRule
        .onNodeWithTag(PopupScreenTestTags.TITLE)
        .assertTextEquals("New Friend's Request from Pseudo")
    testRule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsDisplayed()
    testRule.onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsDisplayed()
  }

  /** Tests that all buttons present are clickable */
  @Test
  fun buttonsAreEnabled() {
    testRule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsEnabled()
    testRule.onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsEnabled()
  }
}
