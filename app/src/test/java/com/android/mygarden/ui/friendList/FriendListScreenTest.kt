package com.android.mygarden.ui.friendList

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FriendListScreenTests {

    @get:Rule val composeTestRule = createComposeRule()

    private fun createViewModelWith(
        friends: List<String> = emptyList(),
        profiles: Map<String, UserProfile> = emptyMap()
    ): FriendListViewModel {
        val fakeFriends = FakeFriendsRepository().apply { friendsFlow.value = friends }
        val fakeProfiles = FakeUserProfileRepository().apply { this.profiles.putAll(profiles) }

        val auth: FirebaseAuth = mock()
        val user: FirebaseUser = mock()
        whenever(auth.currentUser).thenReturn(user)
        whenever(user.uid).thenReturn("current-user-id")

        return FriendListViewModel(
            friendsRepository = fakeFriends,
            userProfileRepository = fakeProfiles,
            auth = auth)
    }

    /** Setup with an empty list of friends. */
    private fun setupWithNoFriends() {
        val vm = createViewModelWith()
        composeTestRule.setContent {
            MyGardenTheme { FriendListScreen(friendListViewModel = vm, onBackPressed = {}) }
        }
    }

    /** Setup with two friends: Alice and Bob. */
    private fun setupWithFriends() {
        val profiles =
            mapOf(
                "uid-alice" to
                        UserProfile("uid-alice", "Alice", Avatar.A1, "Beginner", "Rose"),
                "uid-bob" to
                        UserProfile("uid-bob", "Bob", Avatar.A2, "Expert", "Cactus"))
        val vm = createViewModelWith(friends = profiles.keys.toList(), profiles = profiles)
        composeTestRule.setContent {
            MyGardenTheme { FriendListScreen(friendListViewModel = vm, onBackPressed = {}) }
        }
    }

    /** Check that the empty state UI is displayed correctly. */
    @Test
    fun whenNoFriends_noFriendTextIsDisplayed() {
        setupWithNoFriends()

        composeTestRule.onNodeWithTag(FriendListScreenTestTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(FriendListScreenTestTags.NO_FRIEND).assertIsDisplayed()
    }

    /** Check that friend cards are displayed when there are friends. */
    @Test
    fun whenFriendsExist_friendCardsAreDisplayed() {
        setupWithFriends()

        composeTestRule.onNodeWithTag(FriendListScreenTestTags.SCREEN).assertIsDisplayed()

        // Column/list is shown
        composeTestRule
            .onNodeWithTag(FriendListScreenTestTags.FRIEND_COLUMN)
            .assertIsDisplayed()

        // Pseudos are visible
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }
}