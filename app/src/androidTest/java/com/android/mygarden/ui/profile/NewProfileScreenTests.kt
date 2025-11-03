package com.android.mygarden.ui.profile

import androidx.compose.ui.test.junit4.createComposeRule
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeProfileRepository
import org.junit.Rule

/**
 * Comprehensive test suite for the NewProfileScreen composable.
 *
 * This test class extends ProfileScreenTestBase which contains all common tests for profile
 * screens. All tests defined in the base class are automatically run for NewProfileScreen.
 */
class NewProfileScreenTests : ProfileScreenTestBase() {

  @get:Rule override val composeTestRule = createComposeRule()

  override val expectedTitle = "New Profile"
  override val expectedButtonText = "Save Profile"

  /** Helper function to set up the composable content for testing. */
  override fun setContent() {
    // Reset the callback flag to ensure test isolation
    onSavePressedCalled = false

    // Set up the NewProfileScreen with a test callback
    composeTestRule.setContent {
      MyGardenTheme { NewProfileScreen(onSavePressed = { onSavePressedCalled = true }) }
    }

    // Wait for the UI to be fully rendered before proceeding with tests
    composeTestRule.waitForIdle()
  }

  override fun setContentWithFakeRepo() {
    val repo = FakeProfileRepository()
    val vm = ProfileViewModel(repo)
    onSavePressedCalled = false
    composeTestRule.setContent {
      MyGardenTheme {
        NewProfileScreen(profileViewModel = vm, onSavePressed = { onSavePressedCalled = true })
      }
    }
  }
}
