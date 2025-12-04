package com.android.mygarden.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.profile.PseudoRepository
import com.android.mygarden.model.profile.PseudoRepositoryProvider
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.FakePseudoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/** Tests for offline functionality in ProfileScreen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProfileScreenOfflineTests {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var profileRepo: FakeProfileRepositoryForOfflineTest
  private lateinit var pseudoRepo: PseudoRepository

  private var savePressedCalled = false
  private var avatarClickCalled = false

  @Before
  fun setUp() {
    profileRepo = FakeProfileRepositoryForOfflineTest()
    pseudoRepo = FakePseudoRepository()

    ProfileRepositoryProvider.repository = profileRepo
    PseudoRepositoryProvider.repository = pseudoRepo

    // Ensure we start with online state
    OfflineStateManager.setOnlineState(true)
    ShadowToast.reset()
  }

  /** Sets up the test content with specified online/offline state */
  private fun setContent() {
    // Set the offline state before composing the screen
    OfflineStateManager.setOnlineState(false)

    savePressedCalled = false
    avatarClickCalled = false

    composeTestRule.setContent {
      MyGardenTheme {
        ProfileScreenBase(
            onSavePressed = { savePressedCalled = true },
            onAvatarClick = { avatarClickCalled = true })
      }
    }
    composeTestRule.waitForIdle()
  }

  @After
  fun tearDown() {
    // Reset to online state after each test
    OfflineStateManager.setOnlineState(true)
  }

  /** Test that the Save button shows a toast when clicked while offline */
  @Test
  fun saveButtonShowsToastWhenOffline() {
    setContent()

    // Fill in mandatory fields
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD).performTextInput("johndoe")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .performTextInput("Switzerland")

    composeTestRule.waitForIdle()

    // Click the save button
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot save profile while offline", toastText)
  }

  /** Test that the profile is not saved when clicking offline */
  @Test
  fun saveButtonDoesNotSaveProfileWhenOffline() = runTest {
    setContent()

    // Fill in mandatory fields
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD).performTextInput("John")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD).performTextInput("Doe")
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD).performTextInput("johndoe")
    composeTestRule
        .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
        .performTextInput("Switzerland")

    composeTestRule.waitForIdle()

    val initialSaveCount = profileRepo.saveProfileCallCount

    // Click the save button
    composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Verify no profile was saved
    assertEquals(initialSaveCount, profileRepo.saveProfileCallCount)
    assertFalse(savePressedCalled)
  }

  /**
   * Fake ProfileRepository that tracks save calls for testing.
   *
   * This extends FakeProfileRepository to add call counting functionality.
   */
  private class FakeProfileRepositoryForOfflineTest(profile: Profile? = null) : ProfileRepository {
    var saveProfileCallCount = 0
      private set

    private val baseRepo = FakeProfileRepository(profile)

    override fun getCurrentUserId(): String? = baseRepo.getCurrentUserId()

    override fun getProfile(): Flow<Profile?> = baseRepo.getProfile()

    override suspend fun saveProfile(profile: Profile) {
      saveProfileCallCount++
      baseRepo.saveProfile(profile)
    }

    override suspend fun attachFCMToken(token: String): Boolean = baseRepo.attachFCMToken(token)

    override suspend fun getFCMToken(): String? = baseRepo.getFCMToken()

    override fun cleanup() = baseRepo.cleanup()
  }
}
