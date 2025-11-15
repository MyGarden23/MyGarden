package com.android.mygarden.ui.profile

import android.content.Context
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mygarden.R
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.FakePseudoRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Comprehensive test suite for the EditProfileScreen composable.
 *
 * This test class extends ProfileScreenTestBase which contains all common tests for profile
 * screens. All tests defined in the base class are automatically run for EditProfileScreen.
 *
 * The tests in this class focus specifically on EditProfileScreen's unique behavior:
 * - Pre-filling form fields with existing profile data
 * - Back button navigation functionality
 * - Editing existing profile values
 */
class EditProfileScreenTests : ProfileScreenTestBase() {

  @get:Rule override val composeTestRule = createComposeRule()

  override val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

  override val expectedTitle = context.getString(R.string.edit_profile_screen_title)
  override val expectedButtonText = context.getString(R.string.save_profile_button_text)

  /** Helper function to set up the composable content for testing. */
  override fun setContent() {
    // Reset the callback flags to ensure test isolation
    onSavePressedCalled = false
    onBackPressedCalled = false

    val repo = FakeProfileRepository()
    val repoPseudo = FakePseudoRepository()
    val vm = ProfileViewModel(repo, repoPseudo)

    // Set up the EditProfileScreen with a test callback
    composeTestRule.setContent {
      MyGardenTheme {
        EditProfileScreen(
            profileViewModel = vm,
            onSavePressed = { onSavePressedCalled = true },
            onBackPressed = { onBackPressedCalled = true })
      }
    }

    // Wait for the UI to be fully rendered before proceeding with tests
    composeTestRule.waitForIdle()
  }

  override fun setContentWithFakeRepo() {
    val repo = FakeProfileRepository()
    val repoPseudo = FakePseudoRepository()
    val vm = ProfileViewModel(repo, repoPseudo)
    onSavePressedCalled = false

    composeTestRule.setContent {
      MyGardenTheme {
        EditProfileScreen(profileViewModel = vm, onSavePressed = { onSavePressedCalled = true })
      }
    }
  }

  /** Helper function to set up the screen with a pre-existing profile. */
  private fun setContentWithProfile(profile: Profile, pseudo: String) {
    val repo = FakeProfileRepository(profile)
    val repoPseudo = FakePseudoRepository(pseudo)
    val vm = ProfileViewModel(repo, repoPseudo)
    onSavePressedCalled = false
    onBackPressedCalled = false

    composeTestRule.setContent {
      MyGardenTheme {
        EditProfileScreen(
            profileViewModel = vm,
            onSavePressed = { onSavePressedCalled = true },
            onBackPressed = { onBackPressedCalled = true })
      }
    }

    // Wait for the profile to be loaded and the UI to be initialized
    composeTestRule.waitForIdle()
  }

  // ========== EDIT PROFILE SPECIFIC TESTS ==========

  @Test
  fun fieldsArePrefilledWithExistingProfile() {
    // Given: an existing profile
    val existingProfile =
        Profile(
            firstName = "John",
            lastName = "Doe",
            pseudo = "pseudo",
            gardeningSkill = GardeningSkill.INTERMEDIATE,
            favoritePlant = "Rose",
            country = "Switzerland",
            hasSignedIn = true,
            avatar = Avatar.A10)

    // When: the edit screen is loaded
    setContentWithProfile(existingProfile, "pseudo")

    // Then: all fields should be pre-filled with the existing profile data
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).assertTextContains("John")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).assertTextContains("Doe")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD).assertTextContains("pseudo")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD)
        .assertTextContains(GardeningSkill.INTERMEDIATE.name)
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.FAVORITE_PLANT_FIELD)
        .assertTextContains("Rose")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .assertTextContains("Switzerland")
  }

  @Test
  fun canEditExistingGardeningSkill() {
    // Given: an existing profile with BEGINNER skill level
    val existingProfile =
        Profile(
            firstName = "Jane",
            lastName = "Smith",
            pseudo = "pseudo",
            gardeningSkill = GardeningSkill.BEGINNER,
            favoritePlant = "Orchid",
            country = "Canada",
            hasSignedIn = true,
            avatar = Avatar.A5)

    setContentWithProfile(existingProfile, "pseudo")

    // When: the user changes the gardening skill to EXPERT
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD).performClick()
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.getExperienceItemTag(GardeningSkill.EXPERT))
        .performClick()

    // Then: the field should display the updated skill level
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.EXPERIENCE_FIELD)
        .assertTextContains(GardeningSkill.EXPERT.name)
  }

  @Test
  fun backButtonTriggersCallback() {
    // Given: the edit screen is displayed
    setContent()

    // When: the back button is clicked
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()

    // Then: the back callback should be triggered
    assertEquals(true, onBackPressedCalled)
  }

  @Test
  fun canSaveEditedProfile() {
    // Given: an existing profile
    val existingProfile =
        Profile(
            firstName = "Alice",
            lastName = "Wonder",
            pseudo = "pseudo",
            gardeningSkill = GardeningSkill.INTERMEDIATE,
            favoritePlant = "Cactus",
            country = "Mexico",
            hasSignedIn = true,
            avatar = Avatar.A15)

    setContentWithProfile(existingProfile, "pseudo")

    // When: the user modifies the favorite plant and saves
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.FAVORITE_PLANT_FIELD)
        .performTextInput(" plant")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Then: the save callback should be triggered
    assertEquals(true, onSavePressedCalled)
  }
}
