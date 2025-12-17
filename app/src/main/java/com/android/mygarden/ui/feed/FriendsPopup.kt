package com.android.mygarden.ui.feed

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.android.mygarden.R
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.popup.PopUpDimensions
import com.android.mygarden.ui.popup.PopupScreenTestTags
import com.android.mygarden.ui.popup.QuitPopup

// Dimensions for FriendsPopup
private val FRIENDS_POPUP_HEIGHT = 350.dp
private val FRIEND_CARD_HEIGHT = 100.dp
private val FRIEND_CARD_PADDING = 8.dp
private val AVATAR_SIZE = 70.dp
private val SPACE_BETWEEN_CARDS = 12.dp
private const val CARD_ROW_WIDTH_FRACTION = 0.9f
private const val MAX_LINES = 1
private val PSEUDO_FONT_SIZE = 18.sp
private val CONTENT_HORIZONTAL_PADDING = 16.dp
private val ROW_SPACING = 12.dp
private val CARD_CORNER_RADIUS = PopUpDimensions.CARD_ROUND_ANGLE
private val CARD_ELEVATION = 4.dp
private val SPACER_BETWEEN_TEXT_AND_BUTTON = 4.dp
private const val COLUMN_WEIGHT = 1f

/**
 * Displays a popup dialog showing two friends involved in an "Add Friend" activity.
 *
 * The popup displays two cards, one for each user involved in the friendship activity. Each card
 * shows the user's avatar, pseudo, and an action button based on the relationship with the current
 * user (e.g., "See Garden", "Add Friend", etc.).
 *
 * @param onDismiss callback invoked when the popup is dismissed
 * @param feedViewModel the view model containing the UI state and handling user actions
 */
@Composable
fun FriendActivityPopup(
    onDismiss: () -> Unit = {},
    feedViewModel: FeedViewModel,
) {
  val uiState by feedViewModel.uiState.collectAsState()

  Dialog(
      onDismissRequest = onDismiss,
  ) {
    Card(
        modifier =
            Modifier.testTag(PopupScreenTestTags.CARD)
                .fillMaxWidth()
                .height(FRIENDS_POPUP_HEIGHT)
                .padding(PopUpDimensions.CARD_PADDING),
        shape = RoundedCornerShape(PopUpDimensions.CARD_ROUND_ANGLE),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
      Column(
          modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // A row to have the red cross button only on top and the rest below
            Row(
                modifier =
                    Modifier.fillMaxWidth().padding(PopUpDimensions.DISMISS_BUTTON_PADDING)) {
                  QuitPopup(onClick = onDismiss)
                }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = CONTENT_HORIZONTAL_PADDING),
                verticalArrangement = Arrangement.spacedBy(SPACE_BETWEEN_CARDS),
                horizontalAlignment = Alignment.CenterHorizontally) {
                  FriendsPopupCard(
                      uiState.watchedUser1, uiState.relationWithWatchedUser1, feedViewModel)
                  FriendsPopupCard(
                      uiState.watchedUser2, uiState.relationWithWatchedUser2, feedViewModel)
                }
          }
    }
  }
}

/**
 * Displays a card showing a user profile with their avatar, pseudo, and an action button.
 *
 * The button and its action depend on the relationship between the current user and the displayed
 * user:
 * - FRIEND: Shows "See Garden" button to navigate to friend's garden
 * - NOT_FRIEND: Shows "Add Friend" button to send a friend request
 * - SELF: Shows "Your Garden" button to navigate to own garden
 * - REQUEST_SENT: Shows "Request Sent" button (disabled)
 * - REQUEST_RECEIVED: Shows "Add Back" button to accept the friend request
 *
 * @param userProfile the user profile to display, or null to hide the card
 * @param relation the relationship between the current user and the displayed user
 * @param feedViewModel the view model for handling user actions
 */
@Composable
fun FriendsPopupCard(
    userProfile: UserProfile?,
    relation: RelationWithWatchedUser,
    feedViewModel: FeedViewModel
) {
  if (userProfile == null) return
  val buttonText = getButtonText(relation)
  val avatarDescription =
      stringResource(R.string.avatar_description_friend_screen, userProfile.pseudo)

  Card(
      modifier = Modifier.fillMaxWidth().height(FRIEND_CARD_HEIGHT).padding(FRIEND_CARD_PADDING),
      shape = RoundedCornerShape(CARD_CORNER_RADIUS),
      elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Row(
              modifier = Modifier.fillMaxWidth(CARD_ROW_WIDTH_FRACTION),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ROW_SPACING)) {
                // Avatar
                Card(modifier = Modifier.clip(CircleShape).size(AVATAR_SIZE)) {
                  Image(
                      painter = painterResource(userProfile.avatar.resId),
                      contentDescription = avatarDescription,
                      modifier = Modifier.fillMaxSize())
                }

                // Column with Pseudo and Button
                Column(
                    modifier = Modifier.weight(COLUMN_WEIGHT).fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                      Text(
                          text = userProfile.pseudo,
                          fontWeight = FontWeight.Bold,
                          fontSize = PSEUDO_FONT_SIZE,
                          color = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier,
                          maxLines = MAX_LINES,
                          overflow = TextOverflow.Ellipsis,
                      )

                      Spacer(modifier = Modifier.height(SPACER_BETWEEN_TEXT_AND_BUTTON))

                      Button(
                          modifier =
                              Modifier.testTag(FriendsPopupTestTags.buttonTestTag(userProfile.id)),
                          onClick = {
                            onClickActionForPopup(
                                relationWithWatchedUser = relation,
                                feedViewModel = feedViewModel,
                                userProfile = userProfile)
                          },
                          colors =
                              ButtonDefaults.buttonColors(
                                  containerColor =
                                      when (relation) {
                                        RelationWithWatchedUser.REQUEST_SENT ->
                                            MaterialTheme.colorScheme.surfaceVariant
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                      },
                                  contentColor =
                                      when (relation) {
                                        RelationWithWatchedUser.REQUEST_SENT ->
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                      }),
                          content = { Text(text = buttonText, maxLines = MAX_LINES) })
                    }
              }
        }
      }
}

/**
 * Handles the click action on a friend popup card button based on the relationship type.
 *
 * Delegates to the appropriate FeedViewModel handler method depending on the relationship:
 * - FRIEND: Navigates to the friend's garden
 * - NOT_FRIEND: Sends a friend request
 * - SELF: Navigates to the user's own garden
 * - REQUEST_RECEIVED: Accepts the incoming friend request
 * - REQUEST_SENT: Does nothing (button is disabled)
 *
 * @param relationWithWatchedUser the relationship between the current user and the watched user
 * @param feedViewModel the view model for handling the action
 * @param userProfile the profile of the user whose card was clicked
 */
fun onClickActionForPopup(
    relationWithWatchedUser: RelationWithWatchedUser,
    feedViewModel: FeedViewModel,
    userProfile: UserProfile
) {
  when (relationWithWatchedUser) {
    RelationWithWatchedUser.FRIEND -> {
      feedViewModel.handleFriendActivityClick(userProfile.id)
    }
    RelationWithWatchedUser.NOT_FRIEND -> {
      feedViewModel.handleNotFriendActivityClick(userProfile.id)
    }
    RelationWithWatchedUser.SELF -> {
      feedViewModel.handleSelfActivityClick()
    }
    RelationWithWatchedUser.REQUEST_RECEIVED -> {
      feedViewModel.handleRequestReceivedActivityClick(userProfile.id)
    }
    RelationWithWatchedUser.REQUEST_SENT -> {}
  }
}

/**
 * Returns the appropriate button text based on the relationship with the watched user.
 *
 * @param relation the relationship between the current user and the watched user
 * @return the localized button text for the given relationship
 */
@Composable
fun getButtonText(relation: RelationWithWatchedUser): String {
  return when (relation) {
    RelationWithWatchedUser.FRIEND -> stringResource(R.string.friend_popup_see_garden)
    RelationWithWatchedUser.NOT_FRIEND -> stringResource(R.string.friend_popup_add_friend)
    RelationWithWatchedUser.SELF -> stringResource(R.string.friend_popup_your_garden)
    RelationWithWatchedUser.REQUEST_SENT -> stringResource(R.string.friend_popup_request_sent)
    RelationWithWatchedUser.REQUEST_RECEIVED -> stringResource(R.string.add_back_enum)
  }
}

/** Object containing test tags for the Friends Popup UI components. */
object FriendsPopupTestTags {
  /**
   * Generates a test tag for a friend popup button based on the user ID.
   *
   * @param userId the ID of the user whose button is being tagged
   * @return a unique test tag string for the button
   */
  fun buttonTestTag(userId: String): String = "FriendsPopupButton-$userId"
}
