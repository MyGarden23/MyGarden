package com.android.mygarden.ui.friendsRequests

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
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

/**
 * Tests for offline functionality in RequestItem composable.
 *
 * These tests verify that:
 * 1. Toast messages are displayed when clicking buttons while offline
 * 2. Actions are not executed when offline
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RequestItemOfflineTests {

  @get:Rule val composeTestRule = createComposeRule()

  private val testUser =
      UserProfile(
          id = "test-user-123",
          pseudo = "TestUser",
          avatar = Avatar.A1,
          gardeningSkill = GardeningSkill.NOVICE.name,
          favoritePlant = "Rose")

  @Before
  fun setUp() {
    // Ensure we start with online state
    OfflineStateManager.setOnlineState(true)
    ShadowToast.reset()
  }

  @After
  fun tearDown() {
    // Reset to online state after each test
    OfflineStateManager.setOnlineState(true)
  }

  /**
   * Sets up the test content with specified online/offline state
   *
   * @param isOnline whether the device should be in online or offline mode
   * @param onAcceptCalled flag to track if accept button callback was triggered
   * @param onRefuseCalled flag to track if refuse button callback was triggered
   */
  private fun setContent(
      isOnline: Boolean = true,
      onAcceptCalled: () -> Unit = {},
      onRefuseCalled: () -> Unit = {}
  ) {
    OfflineStateManager.setOnlineState(isOnline)
    composeTestRule.setContent {
      MyGardenTheme {
        RequestItem(
            potentialNewFriend = testUser, onAccept = onAcceptCalled, onRefuse = onRefuseCalled)
      }
    }
    composeTestRule.waitForIdle()
  }

  /** Test that accepting a request doesn't execute the callback when offline */
  @Test
  fun acceptingDoesNotExecuteWhenOffline() {
    var acceptCalled = false

    setContent(isOnline = false, onAcceptCalled = { acceptCalled = true })

    // Try to accept while offline
    composeTestRule
        .onNodeWithTag(FriendsRequestsScreenTestTags.getRequestAcceptButtonFromUser("TestUser"))
        .performClick()

    composeTestRule.waitForIdle()

    // Verify that the callback was NOT executed
    assertFalse("Accept callback should not be executed when offline", acceptCalled)

    // Verify the toast was shown
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot accept or reject friend requests while offline", toastText)
  }

  /** Test that declining a request doesn't execute the callback when offline */
  @Test
  fun decliningDoesNotExecuteWhenOffline() {
    var refuseCalled = false

    setContent(isOnline = false, onRefuseCalled = { refuseCalled = true })

    // Try to decline while offline
    composeTestRule
        .onNodeWithTag(FriendsRequestsScreenTestTags.getRequestDeclineButtonFromUser("TestUser"))
        .performClick()

    composeTestRule.waitForIdle()

    // Verify that the callback was NOT executed
    assertFalse("Refuse callback should not be executed when offline", refuseCalled)

    // Verify the toast was shown
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot accept or reject friend requests while offline", toastText)
  }
}
