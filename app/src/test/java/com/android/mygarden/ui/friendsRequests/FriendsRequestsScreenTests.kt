package com.android.mygarden.ui.friendsRequests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.friends.FriendRequestsRepository
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FriendsRequestsScreenTests {

  @get:Rule val rule = createComposeRule()

  val request1 = FriendRequest(fromUserId = "from-user-1")

  private lateinit var requestRepo: FriendRequestsRepository
  private lateinit var userRepo: UserProfileRepository

  private fun ComposeTestRule.assertRequestForUserIsDisplayed(pseudo: String) {
    onNodeWithTag(FriendsRequestsScreenTestTags.getRequestCardFromUser(pseudo)).assertIsDisplayed()
    onNodeWithTag(FriendsRequestsScreenTestTags.getRequestAvatarFromUser(pseudo))
        .assertIsDisplayed()
    onNodeWithTag(FriendsRequestsScreenTestTags.getRequestAcceptButtonFromUser(pseudo))
        .assertIsDisplayed()
    onNodeWithTag(FriendsRequestsScreenTestTags.getRequestDeclineButtonFromUser(pseudo))
        .assertIsDisplayed()
  }

  // to be called each time to correctly setup the initial requests (if any)
  fun setup(initialRequests: List<FriendRequest> = emptyList()) {
    FriendRequestsRepositoryProvider.repository = FakeFriendRequestsRepository(initialRequests)
    UserProfileRepositoryProvider.repository = FakeUserProfileRepository()
    requestRepo = FriendRequestsRepositoryProvider.repository
    userRepo = UserProfileRepositoryProvider.repository
    rule.setContent { MyGardenTheme { FriendsRequestsScreen() } }
  }

  @Test
  fun noRequestDisplaysCorrectMessage() = runTest {
    setup()
    rule.onNodeWithTag(NavigationTestTags.TOP_BAR).assertIsDisplayed()
    rule.onNodeWithTag(FriendsRequestsScreenTestTags.NO_REQUEST_TEXT).assertIsDisplayed()
  }

  @Test
  fun correctDisplayWithAnInitialRequest() = runTest {
    setup(listOf(request1))
    rule.onNodeWithTag(NavigationTestTags.TOP_BAR).assertIsDisplayed()
    rule.onNodeWithTag(FriendsRequestsScreenTestTags.NO_REQUEST_TEXT).assertIsNotDisplayed()
    // using the fake pseudo that the FakeUserProfileRepository creates each time
    rule.assertRequestForUserIsDisplayed("alice")
  }

  @Test
  fun clickingOnAcceptTriggersCallback() = runTest {
    setup(listOf(request1))
    rule.onNodeWithTag(NavigationTestTags.TOP_BAR).assertIsDisplayed()
    rule.onNodeWithTag(FriendsRequestsScreenTestTags.NO_REQUEST_TEXT).assertIsNotDisplayed()
    rule.assertRequestForUserIsDisplayed("alice")
    // click on accept button
    rule
        .onNodeWithTag(FriendsRequestsScreenTestTags.getRequestAcceptButtonFromUser("alice"))
        .performClick()
    // the request has been handled - no more request
    rule.onNodeWithTag(FriendsRequestsScreenTestTags.NO_REQUEST_TEXT).assertIsDisplayed()
  }

  @Test
  fun clickingOnDeclineButtonTriggersCallback() = runTest {
    setup(listOf(request1))
    rule.onNodeWithTag(NavigationTestTags.TOP_BAR).assertIsDisplayed()
    rule.onNodeWithTag(FriendsRequestsScreenTestTags.NO_REQUEST_TEXT).assertIsNotDisplayed()
    rule.assertRequestForUserIsDisplayed("alice")
    // click on accept button
    rule
        .onNodeWithTag(FriendsRequestsScreenTestTags.getRequestDeclineButtonFromUser("alice"))
        .performClick()
    // the request has been handled - no more request
    rule.onNodeWithTag(FriendsRequestsScreenTestTags.NO_REQUEST_TEXT).assertIsDisplayed()
  }
}
