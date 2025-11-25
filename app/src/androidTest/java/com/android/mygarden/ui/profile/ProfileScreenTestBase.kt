package com.android.mygarden.ui.profile

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.mygarden.R
import org.junit.Rule

/**
 * Abstract base class for testing profile screens (NewProfileScreen and EditProfileScreen).
 *
 * Since both screens use the same ProfileScreenBase composable, they share the same UI and
 * behavior. This base class contains all common test cases that apply to both screens.
 *
 * Subclasses must implement:
 * - [setContent] to set up the specific screen being tested
 * - [expectedTitle] to provide the expected title text for that screen
 * - [expectedButtonText] to provide the expected button text for that screen
 */
abstract class ProfileScreenTestBase {

  @get:Rule abstract val composeTestRule: ComposeTestRule

  /** Flag to track whether the onSavePressed or onBackCalled callbacks have been invoked. */
  protected var onSavePressedCalled: Boolean = false
  protected var onBackPressedCalled: Boolean = false

  abstract val context: Context

  /**
   * Subclasses must implement this to set up the content for testing. Should reset
   * onSavePressedCalled flag and call composeTestRule.setContent.
   */
  abstract fun setContent()

  /** Subclasses must override this to provide the expected title for the screen. */
  abstract val expectedTitle: String

  /** Subclasses must override this to provide the expected button text for the screen. */
  abstract val expectedButtonText: String

  /** Helper function to perform text input and verify that the text is displayed correctly. */
  protected fun performTextInputAndAssert(testTag: String, inputText: String) {
    composeTestRule.onNodeWithTag(testTag).performTextInput(inputText)
    composeTestRule.onNodeWithTag(testTag).assertTextContains(inputText)
  }

  @org.junit.Test
  fun countrySearchShowsNoResultsMessage() {
    setContent()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .performTextInput("NonExistentCountry")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_NO_RESULTS)
        .assertIsDisplayed()
        .assertTextEquals(context.getString(R.string.no_country_found))
  }

  @org.junit.Test
  fun selectingCountryFromDropdownUpdatesField() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("Fr")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.getCountryItemTag("France")).performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).assertTextContains("France")
  }
  /**
   * Subclasses should implement this to set content with a fake repository for testing successful
   * form submissions.
   */
  abstract fun setContentWithFakeRepo()
}
