package com.android.mygarden.ui.profile

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.android.mygarden.R
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.FakePseudoRepository
import org.junit.Rule

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
    val repoPseudo = FakePseudoRepository()
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
}
