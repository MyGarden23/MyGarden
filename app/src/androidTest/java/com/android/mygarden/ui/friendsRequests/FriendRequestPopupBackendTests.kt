package com.android.mygarden.ui.friendsRequests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.MyGardenApp
import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.friends.FriendRequestStatus
import com.android.mygarden.model.friends.FriendRequestsRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.popup.PopupScreenTestTags
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendRequestPopupBackendTests {

  @get:Rule val rule = createComposeRule()

  private lateinit var fakeFriendsRepo: FakeFriendRequestsRepository
  private lateinit var fakeUserRepo: FakeUserProfileRepository

  @Before
  fun setupRepo() {
    fakeFriendsRepo = FakeFriendRequestsRepository()
    fakeUserRepo = FakeUserProfileRepository()

    FriendRequestsRepositoryProvider.repository = fakeFriendsRepo
    UserProfileRepositoryProvider.repository = fakeUserRepo
  }

  fun setContent() {
    rule.setContent { MyGardenTheme { MyGardenApp() } }
    rule.waitForIdle()
  }

  @After
  fun cleanup() {
    fakeFriendsRepo.cleanup()
  }

  /** Helper identical to WaterPlant tests * */
  fun ComposeTestRule.wholePopupIsDisplayed() {
    onNodeWithTag(PopupScreenTestTags.CARD).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.TITLE).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsDisplayed()
    onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsDisplayed()
  }

  fun ComposeTestRule.wholePopupIsNotDisplayed() {
    onNodeWithTag(PopupScreenTestTags.CARD).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.TITLE).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).assertIsNotDisplayed()
    onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).assertIsNotDisplayed()
  }

  private fun emitFriendRequest(senderId: String, senderPseudo: String) = runTest {
    fakeUserRepo.profiles[senderId] =
        UserProfile(id = senderId, pseudo = senderPseudo, avatar = Avatar.A11)

    val req =
        FriendRequest(
            id = "req-1",
            fromUserId = senderId,
            toUserId = "user-x",
            status = FriendRequestStatus.PENDING)

    fakeFriendsRepo.incomingRequestsFlow.value = listOf(req)
    rule.waitForIdle()
  }

  @Test
  fun noPopupWhenNoIncomingRequest() {
    setContent()
    rule.wholePopupIsNotDisplayed()
  }

  @Test
  fun popupAppearsWhenNewFriendRequestArrives() {
    setContent()
    emitFriendRequest("alice-id", "Alice")
    rule.wholePopupIsDisplayed()
  }

  @Test
  fun canDismissPopupByClickingOnDismissButton() {
    setContent()
    emitFriendRequest("bob-id", "Bob")
    rule.wholePopupIsDisplayed()

    rule.onNodeWithTag(PopupScreenTestTags.DISMISS_BUTTON).performClick()

    rule.wholePopupIsNotDisplayed()
  }

  @Test
  fun popupLeavesScreenWhenClickingOnConfirmButton() {
    setContent()
    emitFriendRequest("bob1-id", "bob1")
    rule.wholePopupIsDisplayed()

    rule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).performClick()

    rule.wholePopupIsNotDisplayed()
  }

  @Test
  fun popupNavToGardenWhenClickingOnConfirmButton() {
    setContent()
    emitFriendRequest("alice1-id", "alice1")
    rule.wholePopupIsDisplayed()

    rule.onNodeWithTag(PopupScreenTestTags.CONFIRM_BUTTON).performClick()
    rule.wholePopupIsNotDisplayed()

    rule
        .onNodeWithTag(GardenScreenTestTags.EMPTY_GARDEN_MSG)
        .assertIsDisplayed() // TODO(Modify at the same time that the Main)
  }
}
