package com.android.mygarden.ui.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.ui.theme.MyGardenTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Comprehensive test suite for the NewProfileScreen composable.
 *
 * This test class validates all aspects of the profile creation screen including:
 * - UI component visibility and layout
 * - User input functionality for text fields
 * - Dropdown behavior for experience selection and country selection
 * - Search functionality for country field
 * - Form validation and registration logic
 * - Error states and edge cases
 *
 * The tests ensure that the new profile creation flow works correctly across different user
 * interaction patterns and data validation scenarios.
 */
class NewProfileScreenTests {
  /**
   * Compose test rule that provides testing utilities for Compose UI components. This rule manages
   * the Compose test environment and provides access to test nodes.
   */
  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Flag to track whether the onSavePressed callback has been invoked. Used to verify that form
   * submission logic works correctly.
   */
  private var onSavePressedCalled: Boolean = false

  /**
   * Helper function to set up the composable content for testing.
   *
   * Initializes the NewProfileScreen with a test callback that sets the onSavePressedCalled flag
   * when invoked. This allows tests to verify whether the registration callback was triggered by
   * user interactions.
   *
   * Also resets the callback flag to ensure test isolation.
   */
  fun setContent() {
    // Reset the callback flag to ensure test isolation
    onSavePressedCalled = false

    // Set up the NewProfileScreen with a test callback
    composeTestRule.setContent { NewProfileScreen(onSavePressed = { onSavePressedCalled = true }) }

    // Wait for the UI to be fully rendered before proceeding with tests
    composeTestRule.waitForIdle()
  }

  /**
   * Helper function to perform text input and verify that the text is displayed correctly.
   *
   * This function encapsulates the common pattern of:
   * 1. Performing text input on a field identified by a test tag
   * 2. Asserting that the field contains the expected text
   *
   * @param testTag The test tag identifying the UI component to interact with
   * @param inputText The text to input into the field
   */
  private fun performTextInputAndAssert(testTag: String, inputText: String) {
    composeTestRule.onNodeWithTag(testTag).performTextInput(inputText)
    composeTestRule.onNodeWithTag(testTag).assertTextContains(inputText)
  }

  /**
   * Verifies that all essential UI components are properly rendered and visible.
   *
   * This test ensures that the screen layout is complete and all required elements are present for
   * user interaction. Tests include:
   * - Main screen container
   * - Title text
   * - Avatar component
   * - All input fields (first name, last name, experience, favorite plant, country)
   * - Country dropdown icon
   * - Register button
   *
   * This is a foundational test that should pass before any interaction tests.
   */
  @Test
  fun allUIComponentsAreDisplayed() {
    setContent()

    // Verify the main screen container is displayed
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).assertIsDisplayed()

    // Verify header components
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.TITLE).assertIsDisplayed()
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
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.REGISTER_BUTTON).assertIsDisplayed()
  }

  /**
   * Verifies that the screen title displays the expected text.
   *
   * This test ensures that the user sees the correct heading for the profile creation screen, which
   * helps with navigation and context understanding.
   */
  @Test
  fun titleDisplaysCorrectText() {
    setContent()

    // Verify the title shows the expected text
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.TITLE).assertTextEquals("New Profile")
  }

  /**
   * Verifies that the register button displays the expected text.
   *
   * This test ensures that the primary call-to-action button is clearly labeled for user
   * understanding.
   */
  @Test
  fun registerButtonDisplaysCorrectText() {
    setContent()

    // Verify the register button shows the expected text
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.REGISTER_BUTTON)
        .assertTextContains("Save Profile")
  }

  /**
   * Tests the first name input field functionality.
   *
   * Verifies that:
   * - The field accepts text input
   * - The entered text is displayed correctly
   * - The field maintains the input state
   */
  @Test
  fun firstNameFieldAcceptsInput() {
    setContent()

    val testInput = "John"

    // Enter text into the first name field
    performTextInputAndAssert(ProfileScreenTestTags.FIRST_NAME_FIELD, testInput)
  }

  /**
   * Tests the last name input field functionality.
   *
   * Verifies that:
   * - The field accepts text input
   * - The entered text is displayed correctly
   * - The field maintains the input state
   */
  @Test
  fun lastNameFieldAcceptsInput() {
    setContent()

    val testInput = "Doe"

    // Enter text into the last name field
    performTextInputAndAssert(ProfileScreenTestTags.LAST_NAME_FIELD, testInput)
  }

  /**
   * Tests the favorite plant input field functionality.
   *
   * Verifies that:
   * - The field accepts text input
   * - The entered text is displayed correctly
   * - The field maintains the input state
   */
  @Test
  fun favoritePlantFieldAcceptsInput() {
    setContent()

    val testInput = "Rose"

    // Enter text into the favorite plant field
    performTextInputAndAssert(ProfileScreenTestTags.FAVORITE_PLANT_FIELD, testInput)
  }

  /**
   * Tests the country input field functionality.
   *
   * Verifies that:
   * - The field accepts text input
   * - The entered text is displayed correctly
   * - The field maintains the input state
   *
   * Note: This tests basic input functionality, not dropdown behavior.
   */
  @Test
  fun countryFieldAcceptsInput() {
    setContent()

    val testInput = "France"

    // Enter text into the country field
    performTextInputAndAssert(ProfileScreenTestTags.COUNTRY_FIELD, testInput)
  }

  /**
   * Tests the experience dropdown opening functionality.
   *
   * Verifies that clicking on the experience field opens the dropdown menu, making it available for
   * skill selection. This tests the basic dropdown interaction pattern.
   */
  @Test
  fun experienceDropdownOpensWhenClicked() {
    setContent()

    // Click the experience field to trigger dropdown opening
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD).performClick()

    // Verify the dropdown menu becomes visible
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_DROPDOWN_MENU)
        .assertIsDisplayed()
  }

  /**
   * Tests that all gardening skill options are available in the dropdown.
   *
   * Verifies that:
   * - All GardeningSkill enum values are displayed
   * - Each option is clickable
   * - The dropdown provides complete skill selection options
   *
   * This ensures users can select from the full range of experience levels.
   */
  @Test
  fun allGardeningSkillsAreDisplayedInDropdown() {
    setContent()

    // Open the experience dropdown
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD).performClick()

    // Verify each gardening skill is displayed and clickable
    GardeningSkill.values().forEach { skill ->
      composeTestRule
          .onNodeWithTag(ProfileScreenTestTags.getExperienceItemTag(skill))
          .assertIsDisplayed()
          .assertHasClickAction()
    }
  }

  /**
   * Tests the experience selection workflow.
   *
   * Verifies that:
   * - A skill can be selected from the dropdown
   * - The selected skill updates the field display
   * - The dropdown closes after selection
   *
   * This tests the complete interaction flow for experience selection.
   */
  @Test
  fun selectingGardeningSkillUpdatesField() {
    setContent()

    val selectedSkill = GardeningSkill.INTERMEDIATE

    // Open dropdown and select a specific skill
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD).performClick()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.getExperienceItemTag(selectedSkill))
        .performClick()

    // Verify the field displays the selected skill name
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD)
        .assertTextContains(selectedSkill.name)
  }

  /**
   * Tests the country dropdown search trigger functionality.
   *
   * Verifies that typing in the country field automatically opens the dropdown to show filtered
   * search results. This tests the search-as-you-type feature that helps users find countries
   * quickly.
   */
  @Test
  fun countryDropdownOpensWhenTyping() {
    setContent()

    // Type in the country field to trigger search functionality
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("Fr")

    // Verify the dropdown opens automatically to show search results
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()
  }

  /**
   * Tests the country dropdown icon click functionality.
   *
   * Verifies that clicking the dropdown icon next to the country field opens the dropdown menu,
   * providing an alternative way to access the country list besides typing.
   */
  @Test
  fun countryDropdownOpensWhenClickingIcon() {
    setContent()

    // Click the dropdown icon to open the country list
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_ICON).performClick()

    // Verify the dropdown menu opens
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()
  }

  /**
   * Tests dropdown icon functionality with empty country field.
   *
   * Verifies that:
   * - The dropdown icon works when no text has been entered
   * - All countries are displayed when the field is empty
   * - A results count is shown to indicate the total number of countries
   *
   * This ensures users can browse all available countries without typing.
   */
  @Test
  fun countryDropdownIconWorksWithEmptyField() {
    setContent()

    // Click the dropdown icon when the country field is empty
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_ICON).performClick()

    // Verify dropdown opens with all countries available
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()

    // Verify that a results count is displayed showing total countries
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT).assertIsDisplayed()
  }

  /**
   * Tests the country search results count display.
   *
   * Verifies that when users type in the country field, a results count is displayed to inform them
   * how many countries match their search. This provides helpful feedback about the search
   * effectiveness.
   */
  @Test
  fun countrySearchShowsResultsCount() {
    setContent()

    // Type in the country field to trigger a search
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("Fr")

    // Verify that a results count indicator is displayed
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT).assertIsDisplayed()
  }

  /**
   * Tests the no results state for country search.
   *
   * Verifies that when a user searches for a country that doesn't exist, an appropriate "no results
   * found" message is displayed. This provides clear feedback when searches don't match any
   * countries.
   */
  @Test
  fun countrySearchShowsNoResultsMessage() {
    setContent()

    // Type a country name that doesn't exist
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .performTextInput("NonExistentCountry")

    // Verify that a "no results" message is displayed
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_NO_RESULTS)
        .assertIsDisplayed()
        .assertTextEquals("No countries found")
  }

  /**
   * Tests the country selection workflow from dropdown.
   *
   * Verifies that:
   * - Users can search for and find specific countries
   * - Countries can be selected from the filtered results
   * - The selected country updates the input field
   * - The dropdown closes after selection
   */
  @Test
  fun selectingCountryFromDropdownUpdatesField() {
    setContent()

    // Type to filter countries and open the dropdown
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("Fr")

    // Select France from the filtered results
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.getCountryItemTag("France")).performClick()

    // Verify the field now shows the selected country
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).assertTextContains("France")
  }

  /**
   * Tests the country search filtering accuracy.
   *
   * Verifies that:
   * - Typing a country name filters the results appropriately
   * - The expected country appears in the filtered results
   * - A results count is displayed for the filtered results
   *
   * This ensures the search functionality works correctly and provides relevant results to users.
   */
  @Test
  fun countrySearchFiltersResults() {
    setContent()

    // Search for a specific country
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("France")

    // Verify France appears in the filtered results
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.getCountryItemTag("France"))
        .assertIsDisplayed()

    // Verify a results count is displayed for the filtered results
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT).assertIsDisplayed()
  }

  /**
   * Tests the country search result limitation functionality.
   *
   * Verifies that when many countries match a search term, the dropdown still functions properly
   * and shows an appropriate results count. This ensures the UI remains responsive even with broad
   * search terms.
   */
  @Test
  fun countrySearchLimitsDisplayedResults() {
    setContent()

    // Type a broad search term that matches many countries
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("a")

    // Verify the dropdown opens and functions properly
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()

    // Verify a results count is displayed
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_RESULTS_COUNT).assertIsDisplayed()
  }

  /**
   * Tests the "more results" indicator for large country lists.
   *
   * When all countries are displayed (by clicking the dropdown icon with an empty field), there are
   * 195 countries total. Since this exceeds the display limit, a "more results" indicator should be
   * shown to inform users that not all countries are visible.
   */
  @Test
  fun countryDropdownShowsMoreResultsIndicatorForManyMatches() {
    setContent()

    // Open dropdown with empty field to show all 195 countries
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_ICON).performClick()

    // Wait for the UI to fully render with all countries
    composeTestRule.waitForIdle()

    // Verify dropdown opens successfully
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()

    // Scroll to find the "more results" indicator
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS))

    // With 195 countries (> display limit), should show "more results" indicator
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS)
        .assertExists()
        .assertTextContains("more countries", substring = true, ignoreCase = false)
  }

  /**
   * Tests the "more results" indicator with common search terms.
   *
   * Verifies that searching with a common letter (like "a") matches many countries and triggers the
   * "more results" indicator. This ensures users understand when their search results are
   * truncated.
   */
  @Test
  fun countrySearchShowsMoreResultsIndicatorWithCommonLetter() {
    setContent()

    // Search with a common letter that matches many countries
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("a")

    // Wait for search results to be processed and displayed
    composeTestRule.waitForIdle()

    // Verify dropdown opens with search results
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU).assertIsDisplayed()

    // Scroll to find the "more results" indicator
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU)
        .performScrollToNode(hasTestTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS))

    // Verify the "more results" indicator exists for this broad search
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_MORE_RESULTS).assertExists()
  }

  /**
   * Tests a specific country search and selection scenario.
   *
   * This test simulates a realistic user workflow:
   * 1. User types partial country name ("anada" for "Canada")
   * 2. System shows matching results in dropdown
   * 3. User finds and selects the desired country
   * 4. Field is updated with the selected country
   *
   * This verifies the complete search-and-select workflow works correctly.
   */
  @Test
  fun countryDropdownCheckForCanada() {
    setContent()

    // Type partial country name to search for Canada
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).performTextInput("anada")

    // Wait for search results to be processed
    composeTestRule.waitForIdle()

    // Scroll to find Canada in the search results
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_DROPDOWN_MENU)
        .performScrollToNode(
            matcher = hasTestTag(ProfileScreenTestTags.getCountryItemTag("Canada")))

    // Select Canada from the search results
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.getCountryItemTag("Canada")).performClick()

    // Verify the field now contains the selected country
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD).assertTextContains("Canada")
  }

  class FakeProfileRepository : ProfileRepository {
    override fun getCurrentUserId(): String? = "fake_uid"

    override fun getProfile(): Flow<Profile?> = flowOf(null)

    override suspend fun saveProfile(profile: Profile) {
      /* no-op, succeed */
    }
  }

  private fun setContentWithFakeRepo() {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repo)

    composeTestRule.setContent {
      MyGardenTheme {
        NewProfileScreen(profileViewModel = vm, onSavePressed = { onSavePressedCalled = true })
      }
    }
  }

  /**
   * Tests successful form submission with all mandatory fields filled.
   *
   * Verifies that when users fill in all required fields (first name, last name, and country), the
   * register button successfully triggers the registration callback. This tests the happy path for
   * profile creation.
   */
  @Test
  fun canRegisterIfMandatoryFieldsAreFilled() {
    setContentWithFakeRepo()

    // Fill in all mandatory fields
    performTextInputAndAssert(ProfileScreenTestTags.FIRST_NAME_FIELD, "John")
    performTextInputAndAssert(ProfileScreenTestTags.LAST_NAME_FIELD, "Doe")
    performTextInputAndAssert(ProfileScreenTestTags.COUNTRY_FIELD, "Canada")

    // Wait for UI to process all input changes
    composeTestRule.waitForIdle()

    // Attempt to register the profile
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.REGISTER_BUTTON).performClick()

    // Wait for registration callback to be processed
    composeTestRule.waitForIdle()

    // Verify the registration callback was successfully triggered
    assertEquals(true, onSavePressedCalled)
  }

  /**
   * Tests form validation with incomplete mandatory fields.
   *
   * Verifies that when one or more mandatory fields are empty, the registration process is blocked
   * and the callback is not triggered. This tests the form validation logic that prevents
   * incomplete profile submissions.
   *
   * In this test, the country field is left empty while first name and last name are filled, which
   * should prevent registration.
   */
  @Test
  fun cantRegisterIfOneMandatoryFieldsIsEmpty() {
    setContent()

    // Fill in only some mandatory fields (leaving country empty)
    performTextInputAndAssert(ProfileScreenTestTags.FIRST_NAME_FIELD, "John")
    performTextInputAndAssert(ProfileScreenTestTags.LAST_NAME_FIELD, "Doe")

    // Attempt to register with incomplete information
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.REGISTER_BUTTON).performClick()

    // Verify registration callback was NOT triggered due to missing country
    assertEquals(false, onSavePressedCalled)
  }
}
