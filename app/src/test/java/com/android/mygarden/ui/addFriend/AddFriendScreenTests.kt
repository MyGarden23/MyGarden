package com.android.mygarden.ui.addFriend

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
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

  /** Set the AddFriendScreen with a default viewModel. */
  fun setupWithEmptyRepos() {
    composeTestRule.setContent {
      MyGardenTheme { AddFriendScreen(addFriendViewModel = createViewModel()) }
    }
  }

  /** Set the AddFriendScreen with repositories containing the user alice. */
  fun setupWithNotEmptyRepos() {

    val fakePseudo =
        TestPseudoRepository().apply {
          // User types "al", search returns "alice"
          searchResults = listOf("alice")
          uidMap["alice"] = "uid-alice"
        }

    val fakeUserProfile =
        FakeUserProfileRepository().apply {
          profiles["uid-alice"] =
              UserProfile(id = "uid-alice", pseudo = "alice", avatar = Avatar.A1)
        }

    val fakeFriends = FakeFriendsRepository()

    // Create the viewModel with the fake repos
    val viewModel =
        createViewModel(
            friendsRepo = fakeFriends, userProfileRepo = fakeUserProfile, pseudoRepo = fakePseudo)

    composeTestRule.setContent { MyGardenTheme { AddFriendScreen(addFriendViewModel = viewModel) } }
  }

  /** Check that everything that should be displayed before searching is displayed. */
  @Test
  fun beforeSearchingEverythingIsDisplayed() {
    setupWithEmptyRepos()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_FRIEND_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.FRIEND_COLUMN).assertIsDisplayed()
  }

  /** Check that a FriendCard with all its elements is displayed when searching a correct pseudo. */
  @Test
  fun afterSearchingAValidPseudo_FriendCardIsDisplayed() {

    setupWithNotEmptyRepos()

    // Type "al"
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_TEXT).performTextInput("al")

    // Click search
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).performClick()

    composeTestRule.waitForIdle()

    val pseudo = "alice"

    // Assert all the elements of the FriendCard for alice is displayed

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForFriendCard(pseudo))
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForRowOnFriendCard(pseudo))
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForAvatarOnFriendCard(pseudo))
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForPseudoOnFriendCard(pseudo))
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(pseudo))
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(pseudo))
        .assertTextContains(FriendRelation.ADD.toString())
  }
}
