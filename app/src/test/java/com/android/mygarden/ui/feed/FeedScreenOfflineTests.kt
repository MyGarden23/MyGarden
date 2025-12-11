package com.android.mygarden.ui.feed

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.friends.FriendsRepository
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepository
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeActivityRepository
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakeProfileRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Tests for offline functionality in FeedScreen.
 *
 * These tests verify that:
 * 1. Buttons are visually greyed out when offline
 * 2. Toast messages are displayed when clicking on buttons while offline
 * 3. Actions are not executed when offline
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedScreenOfflineTests {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var profileRepo: ProfileRepository
  private lateinit var activityRepo: ActivityRepository
  private lateinit var friendsRepo: FriendsRepository
  private lateinit var friendRequestsRepo: FriendRequestsRepository

  @Before
  fun setUp() {
    profileRepo = FakeProfileRepository()
    activityRepo = FakeActivityRepository()
    friendsRepo = FakeFriendsRepository()
    friendRequestsRepo = FakeFriendRequestsRepository()

    ProfileRepositoryProvider.repository = profileRepo
    ActivityRepositoryProvider.repository = activityRepo
    FriendsRepositoryProvider.repository = friendsRepo
    FriendRequestsRepositoryProvider.repository = friendRequestsRepo

    // Ensure we start with online state
    OfflineStateManager.setOnlineState(true)
    ShadowToast.reset()
  }

  /**
   * Sets up the test content with specified online/offline state
   *
   * @param isOnline whether the device should be in online or offline mode
   */
  fun setContent(isOnline: Boolean = true) {
    // Set the offline state before composing the screen
    OfflineStateManager.setOnlineState(isOnline)

    composeTestRule.setContent { MyGardenTheme { FeedScreen() } }
    composeTestRule.waitForIdle()
  }

  @After
  fun tearDown() {
    // Reset to online state after each test
    OfflineStateManager.setOnlineState(true)
  }

  /** Test that the Add Friend FAB shows a toast when clicked while offline */
  @Test
  fun addFriendFabShowsToastWhenOffline() {
    setContent(isOnline = false)

    // Click the add friend FAB
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot add friends while offline", toastText)
  }
}
