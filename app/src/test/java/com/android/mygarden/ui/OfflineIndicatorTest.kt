package com.android.mygarden.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.mygarden.OfflineIndicator
import com.android.mygarden.ui.theme.MyGardenTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the OfflineIndicator composable using Robolectric. This ensures the offline
 * indicator displays correctly with the expected message.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineIndicatorTest {

  @get:Rule val testRule = createComposeRule()

  @Before
  fun setContent() {
    testRule.setContent { MyGardenTheme { OfflineIndicator() } }
  }

  @Test
  fun offlineIndicator_displaysCorrectMessage() {
    // Verify the offline message is displayed
    testRule.onNodeWithText("⚠️ You are offline. Some features are disabled.").assertIsDisplayed()
  }
}
