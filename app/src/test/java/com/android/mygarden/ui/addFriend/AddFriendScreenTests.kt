package com.android.mygarden.ui.addFriend

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.android.mygarden.model.friends.FriendRequest
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeFriendRequestsRepository
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import com.android.mygarden.utils.TestPseudoRepository
import com.android.mygarden.utils.createViewModel
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddFriendScreenTests {

  @get:Rule val composeTestRule = createComposeRule()

  private val pseudoAlice = "alice"
  private val uidAlice = "uid-alice"
  private val currentUserId = "fake-uid" // same value as in FakeFriendRequestsRepository

  /** Sets the AddFriendScreen with a default ViewModel. */
  private fun setupWithEmptyRepos() {
    composeTestRule.setContent {
      MyGardenTheme { AddFriendScreen(addFriendViewModel = createViewModel()) }
    }
  }

  /** Base repositories where Alice can be found by search and has a valid profile. */
  private fun buildBaseRepos():
      Triple<TestPseudoRepository, FakeUserProfileRepository, FakeFriendsRepository> {

    val fakePseudo =
        TestPseudoRepository().apply {
          // User types "al", search returns "alice"
          searchResults = listOf(pseudoAlice)
          uidMap[pseudoAlice] = uidAlice
        }

    val fakeUserProfile =
        FakeUserProfileRepository().apply {
          profiles[uidAlice] =
              UserProfile(
                  id = uidAlice,
                  pseudo = pseudoAlice,
                  avatar = Avatar.A1,
                  GardeningSkill.NOVICE.name,
                  "rose")
        }

    val fakeFriends = FakeFriendsRepository()

    return Triple(fakePseudo, fakeUserProfile, fakeFriends)
  }

  /** Alice is found, but no relation exists → button must display ADD. */
  private fun setupWithAliceAndNoRelation() {
    val (fakePseudo, fakeUserProfile, fakeFriends) = buildBaseRepos()

    val viewModel =
        createViewModel(
            friendsRepo = fakeFriends, userProfileRepo = fakeUserProfile, pseudoRepo = fakePseudo)

    composeTestRule.setContent { MyGardenTheme { AddFriendScreen(addFriendViewModel = viewModel) } }
  }

  /** Alice is already a friend → button must display ADDED. */
  private fun setupWithAliceAlreadyFriend() {
    val (fakePseudo, fakeUserProfile, fakeFriends) = buildBaseRepos()

    // Alice already present in the friend list
    fakeFriends.friendsFlow.value = listOf(uidAlice)

    val viewModel =
        createViewModel(
            friendsRepo = fakeFriends, userProfileRepo = fakeUserProfile, pseudoRepo = fakePseudo)

    composeTestRule.setContent { MyGardenTheme { AddFriendScreen(addFriendViewModel = viewModel) } }
  }

  /** There is an outgoing pending request toward Alice → button must display PENDING. */
  private fun setupWithAlicePendingRequest() {
    val (fakePseudo, fakeUserProfile, fakeFriends) = buildBaseRepos()

    // Simulate an outgoing request currentUserId -> uidAlice
    val fakeRequests =
        FakeFriendRequestsRepository(
            initialRequests =
                listOf(
                    FriendRequest(
                        fromUserId = currentUserId,
                        toUserId = uidAlice,
                    )))

    val viewModel =
        createViewModel(
            friendsRepo = fakeFriends,
            userProfileRepo = fakeUserProfile,
            pseudoRepo = fakePseudo,
            requestsRepo = fakeRequests,
        )

    composeTestRule.setContent { MyGardenTheme { AddFriendScreen(addFriendViewModel = viewModel) } }
  }

  /** Checks that all initial UI elements are displayed before searching. */
  @Test
  fun beforeSearchingEverythingIsDisplayed() {
    setupWithEmptyRepos()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_FRIEND_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.FRIEND_COLUMN).assertIsDisplayed()
  }

  /** ADD: no relation exists → button must display ADD after searching. */
  @Test
  fun afterSearchingValidPseudo_whenNoRelation_buttonShowsAdd() {
    setupWithAliceAndNoRelation()

    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_TEXT).performTextInput("al")
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForFriendCard(pseudoAlice))
        .assertIsDisplayed()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val expectedText = context.getString(FriendRelation.ADD.labelRes)

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(pseudoAlice))
        .assertTextContains(expectedText)
  }

  /** ADDED: Alice is already a friend → button must display ADDED. */
  @Test
  fun afterSearchingValidPseudo_whenAlreadyFriend_buttonShowsAdded() {
    setupWithAliceAlreadyFriend()

    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_TEXT).performTextInput("al")
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val expectedText = context.getString(FriendRelation.ADDED.labelRes)

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(pseudoAlice))
        .assertTextContains(expectedText)
  }

  /** PENDING: an outgoing request exists → button must display PENDING. */
  @Test
  fun afterSearchingValidPseudo_whenPendingRequest_buttonShowsPending() {
    setupWithAlicePendingRequest()

    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_TEXT).performTextInput("al")
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).performClick()
    composeTestRule.waitForIdle()

    val context = ApplicationProvider.getApplicationContext<Context>()
    val expectedText = context.getString(FriendRelation.PENDING.labelRes)

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(pseudoAlice))
        .assertTextContains(expectedText)
  }
}
