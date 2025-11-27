package com.android.mygarden.ui.navigation.navS7

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.android.mygarden.model.friends.FriendsRepositoryProvider
import com.android.mygarden.model.profile.PseudoRepositoryProvider
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.feed.FeedScreenTestTags
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeFriendsRepository
import com.android.mygarden.utils.FakePseudoRepository
import com.android.mygarden.utils.FakeUserProfileRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Class to test the navigation between FeedScreen and AddFriendScreen */
class NavigationS7TestsAddFriendButton {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var navController: NavHostController

  /** Sets up the Compose test environment before each test. */
  @Before
  fun setUp() {
    FriendsRepositoryProvider.repository = FakeFriendsRepository()
    UserProfileRepositoryProvider.repository = FakeUserProfileRepository()
    PseudoRepositoryProvider.repository = FakePseudoRepository()

    composeTestRule.setContent {
      val controller = rememberNavController()
      navController = controller
      MyGardenTheme {
        // We want to start at the FeedScreen
        AppNavHost(navController = controller, startDestination = Screen.Feed.route)
      }
    }
  }

  /** Navigates from the feed to the addFriend screen and assert that the screen is displayed */
  @Test
  fun navigateFromFeedToAddFriendScreen() {
    composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_FRIEND_SCREEN).assertIsDisplayed()
  }

  /**
   * Navigates from the feed to the addFriend screen and back to the feed then assert that the feed
   * screen is displayed
   */
  @Test
  fun navigateFromAddFriendScreenToFeed() {
    composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_FRIEND_SCREEN).assertIsDisplayed()
    // We are on AddFriendScreen and we want to go back to the Feed
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).assertIsDisplayed()
  }
}
