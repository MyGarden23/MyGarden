package com.android.mygarden.ui.profile

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [ChooseProfilePictureScreenTest]. */
@RunWith(AndroidJUnit4::class)
class ChooseProfilePictureScreenTest {

  @get:Rule val composeRule = createComposeRule()

  /**
   * Helper function to set the content of the Compose rule to the [ChooseProfilePictureScreen].
   *
   * @param onChosen A mutable list to capture the [Avatar] passed to the `onAvatarChosen` callback.
   * @param onBackCalled A mutable list to track calls to the `onBack` callback.
   */
  private fun setContentWith(
      onChosen: MutableList<Avatar> = mutableListOf(),
      onBackCalled: MutableList<Boolean> = mutableListOf(),
  ) {
    composeRule.setContent {
      ChooseProfilePictureScreen(
          onAvatarChosen = { onChosen += it }, onBack = { onBackCalled += true })
    }
  }

  /** Verifies that the essential UI components of the screen are displayed. */
  @Test
  fun screen_displaysTopAppBarAndAvatarGrid() {
    setContentWith()

    composeRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.SCREEN).assertIsDisplayed()
    composeRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.TOP_APP_BAR).assertIsDisplayed()
    composeRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.AVATAR_GRID).assertIsDisplayed()
  }

  /** Verifies that clicking the back button in the top app bar triggers the `onBack` callback. */
  @Test
  fun backButton_triggersOnBackCallback() {
    val backCalls = mutableListOf<Boolean>()
    setContentWith(onBackCalled = backCalls)

    composeRule.onNodeWithTag(ChooseProfilePictureScreenTestTags.BACK_BUTTON).performClick()

    assertTrue(backCalls.isNotEmpty())
  }

  /**
   * Verifies that all available avatars are rendered within the grid, are visible, and are
   * clickable.
   */
  @Test
  fun avatars_areDisplayedAndClickable() {
    setContentWith()

    Avatar.values().forEach { avatar ->
      val cardTag = "${ChooseProfilePictureScreenTestTags.AVATAR_CARD_PREFIX}${avatar.name}"

      composeRule
          .onNodeWithTag(ChooseProfilePictureScreenTestTags.AVATAR_GRID)
          .performScrollToNode(hasTestTag(cardTag))

      composeRule.onNodeWithTag(cardTag).assertIsDisplayed().assertHasClickAction()
    }
  }

  /**
   * Verifies that clicking on an avatar triggers the `onAvatarChosen` callback with the correct
   * [Avatar] instance.
   */
  @Test
  fun clickingAvatar_callsOnAvatarChosen() {
    val chosen = mutableListOf<Avatar>()
    setContentWith(onChosen = chosen)

    val first = Avatar.values().first()
    val cardTag = "${ChooseProfilePictureScreenTestTags.AVATAR_CARD_PREFIX}${first.name}"

    composeRule.onNodeWithTag(cardTag).performClick()

    assertEquals(listOf(first), chosen)
  }
}
