package com.android.mygarden.ui.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.ui.navigation.NavigationTestTags
import org.junit.Assert.assertEquals
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

  // ========== COMMON TESTS ==========

  @org.junit.Test
  fun allUIComponentsAreDisplayed() {
    setContent()

    // Verify the main screen container is displayed
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).assertIsDisplayed()

    // Verify header components
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.AVATAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.AVATAR).assertHasClickAction()

    // Verify all input fields are displayed
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FAVORITE_PLANT_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).assertIsDisplayed()

    // Verify interactive elements
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_ICON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).assertIsDisplayed()
  }

  @org.junit.Test
  fun titleDisplaysCorrectText() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertTextEquals(expectedTitle)
  }

  @org.junit.Test
  fun saveButtonDisplaysCorrectText() {
    setContent()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON)
        .assertTextContains(expectedButtonText)
  }

  @org.junit.Test
  fun firstNameFieldAcceptsInput() {
    setContent()
    performTextInputAndAssert(ProfileScreenTestTags.FIRST_NAME_FIELD, "John")
  }

  @org.junit.Test
  fun lastNameFieldAcceptsInput() {
    setContent()
    performTextInputAndAssert(ProfileScreenTestTags.LAST_NAME_FIELD, "Doe")
  }

  @org.junit.Test
  fun favoritePlantFieldAcceptsInput() {
    setContent()
    performTextInputAndAssert(ProfileScreenTestTags.FAVORITE_PLANT_FIELD, "Rose")
  }

  @org.junit.Test
  fun countryFieldAcceptsInput() {
    setContent()
    performTextInputAndAssert(ProfileScreenTestTags.COUNTRY_FIELD, "France")
  }

  @org.junit.Test
  fun experienceDropdownOpensWhenClicked() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD).performClick()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_DROPDOWN_MENU)
        .assertIsDisplayed()
  }

  @org.junit.Test
  fun allGardeningSkillsAreDisplayedInDropdown() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD).performClick()

    GardeningSkill.values().forEach { skill ->
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.getExperienceItemTag(skill))
          .assertIsDisplayed()
          .assertHasClickAction()
    }
  }

  @org.junit.Test
  fun selectingGardeningSkillUpdatesField() {
    setContent()
    val selectedSkill = GardeningSkill.INTERMEDIATE

    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD).performClick()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.getExperienceItemTag(selectedSkill))
        .performClick()

    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD)
        .assertTextContains(selectedSkill.name)
  }

  @org.junit.Test
  fun countryDropdownOpensWhenTyping() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("Fr")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()
  }

  @org.junit.Test
  fun countryDropdownOpensWhenClickingIcon() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_ICON).performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()
  }

  @org.junit.Test
  fun countryDropdownIconWorksWithEmptyField() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_ICON).performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT).assertIsDisplayed()
  }

  @org.junit.Test
  fun countrySearchShowsResultsCount() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("Fr")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT).assertIsDisplayed()
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
        .assertTextEquals("No countries found")
  }

  @org.junit.Test
  fun selectingCountryFromDropdownUpdatesField() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("Fr")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.getCountryItemTag("France")).performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).assertTextContains("France")
  }

  @org.junit.Test
  fun countrySearchFiltersResults() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("France")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.getCountryItemTag("France"))
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT).assertIsDisplayed()
  }

  @org.junit.Test
  fun countrySearchLimitsDisplayedResults() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("a")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT).assertIsDisplayed()
  }

  @org.junit.Test
  fun countryDropdownShowsMoreResultsIndicatorForManyMatches() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_ICON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS))
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS)
        .assertExists()
        .assertTextContains("more countries", substring = true, ignoreCase = false)
  }

  @org.junit.Test
  fun countrySearchShowsMoreResultsIndicatorWithCommonLetter() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("a")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS))
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS).assertExists()
  }

  @org.junit.Test
  fun countryDropdownCheckForCanada() {
    setContent()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("anada")
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU)
        .performScrollToNode(
            matcher = hasTestTag(ProfileScreenTestTags.getCountryItemTag("Canada")))
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.getCountryItemTag("Canada")).performClick()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).assertTextContains("Canada")
  }

  @org.junit.Test
  fun cantRegisterIfOneMandatoryFieldsIsEmpty() {
    setContent()
    performTextInputAndAssert(ProfileScreenTestTags.FIRST_NAME_FIELD, "John")
    performTextInputAndAssert(ProfileScreenTestTags.LAST_NAME_FIELD, "Doe")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    assertEquals(false, onSavePressedCalled)
  }

  // ========== HELPER CLASSES ==========

  /**
   * Subclasses should implement this to set content with a fake repository for testing successful
   * form submissions.
   */
  abstract fun setContentWithFakeRepo()

  @org.junit.Test
  fun canSaveIfMandatoryFieldsAreFilled() {
    setContentWithFakeRepo()

    // Fill in all mandatory fields
    performTextInputAndAssert(ProfileScreenTestTags.FIRST_NAME_FIELD, "John")
    performTextInputAndAssert(ProfileScreenTestTags.LAST_NAME_FIELD, "Doe")
    performTextInputAndAssert(ProfileScreenTestTags.COUNTRY_FIELD, "Canada")

    // Wait for UI to process all input changes
    composeTestRule.waitForIdle()

    // Attempt to save the profile
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()

    // Wait for save callback to be processed
    composeTestRule.waitForIdle()

    // Verify the save callback was successfully triggered
    assertEquals(true, onSavePressedCalled)
  }
}
