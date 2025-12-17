package com.android.mygarden.ui.friendList

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.offline.OfflineStateManager
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeAchievementsRepository
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Tests for offline functionality in FriendListScreen.
 *
 * These tests verify that clicking on a friend's garden and deleting a friend does not work in
 * offline mode, and that toasts are displayed instead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FriendListScreenOfflineTests {

  @get:Rule val composeTestRule = createComposeRule()

  /** Setup two friends Alice and Bob in the list of friends and mock auth and user */
  @Before
  fun setUpOnline() {
    // Populate the screen with some friends
    val profiles =
        mapOf("uid-alice" to UserProfile("uid-alice", "Alice", Avatar.A1, "Beginner", "Rose"))

    val friendsRepo = FakeFriendsRepository().apply { friendsFlow.value = profiles.keys.toList() }
    val userProfileRepo = FakeUserProfileRepository().apply { this.profiles.putAll(profiles) }
    val friendRequestsRepo = FakeFriendRequestsRepository()
    val achievementsRepo = FakeAchievementsRepository()

    // Mock auth FirebaseAuth and FirebaseUser
    val auth: FirebaseAuth = mock()
    val user: FirebaseUser = mock()
    whenever(auth.currentUser).thenReturn(user)
    whenever(user.uid).thenReturn("fake-uid")

    // Initialize achievements repo with base value to be sure
    runBlocking {
      achievementsRepo.initializeAchievementsForNewUser("fake-uid")
      achievementsRepo.initializeAchievementsForNewUser("uid-alice")

      achievementsRepo.updateAchievementValue("fake-uid", AchievementType.FRIENDS_NUMBER, 1)
      achievementsRepo.updateAchievementValue("uid-alice", AchievementType.FRIENDS_NUMBER, 1)
    }

    // Create the ViewModel with the Fake repo that has the two friends
    val viewModel =
        FriendListViewModel(
            friendsRepository = friendsRepo,
            userProfileRepository = userProfileRepo,
            requestRepo = friendRequestsRepo,
            auth = auth,
            achievementsRepo = achievementsRepo)

    composeTestRule.setContent {
      MyGardenTheme { FriendListScreen(friendListViewModel = viewModel) }
    }
    composeTestRule.waitForIdle()

    // Ensure we start offline
    OfflineStateManager.setOnlineState(false)
    ShadowToast.reset()
  }

  @After
  fun tearDown() {
    // Reset to online state after each test
    OfflineStateManager.setOnlineState(true)
  }

  /** Test that clicking on a friend's garden show a Toast while offline */
  @Test
  fun clickingOnFriendsGardenShowToastWhenOffline() {

    // Friend is displayed in the screen
    composeTestRule
        .onNodeWithTag(FriendListScreenTestTags.REQUEST_CARD)
        .assertIsDisplayed()
        .performClick()

    // Verify toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot see a friend's garden while offline", toastText)
  }

  /** Test that deleting a friend keep him in the garden while offline */
  @Test
  fun deletingAFriendDoesNothingWhenOffline() {

    // Friend is displayed in the screen
    composeTestRule
        .onNodeWithTag(FriendListScreenTestTags.REQUEST_CARD)
        .assertIsDisplayed()
        .performClick()

    // Delete button is displayed in the screen and clickable
    composeTestRule
        .onNodeWithTag(FriendListScreenTestTags.getDelButtonForFriend("uid-alice"))
        .assertIsDisplayed()
        .performClick()

    // Can delete a friend -> should show the corresponding toast
    composeTestRule
        .onNodeWithTag(DeleteFriendPopupTestTags.DELETE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot delete friends while offline", toastText)

    // Card with alice still here
    composeTestRule.onNodeWithTag(FriendListScreenTestTags.REQUEST_CARD).assertIsDisplayed()

    composeTestRule.onNodeWithText("Alice").assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(FriendListScreenTestTags.getDelButtonForFriend("uid-alice"))
        .assertIsDisplayed()
  }
}
