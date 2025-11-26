package com.android.mygarden.ui.addFriend

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.theme.MyGardenTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddFriendScreenTests {

  @get:Rule val composeTestRule = createComposeRule()

  /** Set the AddFriendScreen at the beginning of every test. */
  @Before
  fun setup() {
    composeTestRule.setContent { MyGardenTheme { AddFriendScreen() } }
  }

  /** Check that everything that should be displayed before searching is displayed. */
  @Test
  fun beforeSearchingEverythingIsDisplayed() {
    composeTestRule.onNodeWithTag(NavigationTestTags.ADD_FRIEND_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddFriendTestTags.FRIEND_COLUMN).assertIsDisplayed()
  }
}
