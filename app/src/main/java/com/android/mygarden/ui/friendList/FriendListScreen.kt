package com.android.mygarden.ui.friendList

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.navigation.TopBar
import com.android.mygarden.ui.profile.Avatar

/** Test tags used for accessing FriendListScreen UI elements in automated tests. */
object FriendListScreenTestTags {
  const val SCREEN = "FriendListScreen"
  const val FRIEND_COLUMN = "friendListColumn"
  const val REQUEST_CARD = "friendRequestCard"
  const val FRIEND_AVATAR = "friendAvatar"
  const val FRIEND_PSEUDO = "friendPseudo"
  const val NO_FRIEND = "noFriendText"

  fun getDelButtonForFriend(friendId: String) = "DeleteFriendButtonForId$friendId"
}

private object CONSTANTS {
  const val FRACTION_SPACER = 0.02f
  val PADDING_VERTICAL = 20.dp
  val PADDING_HORIZONTAL = 35.dp
  val VERTICAL_ARRANGEMENT_SPACE = 10.dp
  val CARD_HEIGHT = 80.dp
  val AVATAR_SIZE = 70.dp
  const val FRACTION_ROW_WIDTH = 0.94f
  const val FRACTION_BOX_WIDTH = 0.6f
  val TEXT_FONT_SIZE = 20.sp
  const val NUMBER_OF_LINES = 1
  val DEL_BUTTON_BORDER_STROKE = 2.dp
  val DEL_BUTTON_SIZE = 40.dp
}

/**
 * Displays the friend list screen, showing either the list of friends or an empty-state message.
 *
 * @param onBackPressed callback triggered when the user presses the top bar back button
 * @param onFriendClick callback triggered when the user clicks on a friend card
 * @param friendListViewModel ViewModel providing the friend list data
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(
    onBackPressed: () -> Unit = {},
    onFriendClick: (UserProfile) -> Unit = {},
    friendListViewModel: FriendListViewModel = viewModel(),
) {
  val context = LocalContext.current
  val uiState by friendListViewModel.uiState.collectAsState()

  // Load the friend list when the screen first appears
  LaunchedEffect(Unit) {
    friendListViewModel.getFriends(
        onError = {
          Toast.makeText(
                  context, context.getString(R.string.error_loading_friends), Toast.LENGTH_SHORT)
              .show()
        })
  }

  Scaffold(
      topBar = {
        TopBar(
            title = stringResource(R.string.friend_list_screen),
            hasGoBackButton = true,
            onGoBack = onBackPressed)
      },
      modifier = Modifier.testTag(FriendListScreenTestTags.SCREEN),
      content = { paddingValues ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally) {
              Spacer(modifier = Modifier.fillMaxHeight(CONSTANTS.FRACTION_SPACER))

              if (uiState.friends.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = stringResource(R.string.friend_list_no_friend),
                          modifier = Modifier.testTag(FriendListScreenTestTags.NO_FRIEND),
                          style = MaterialTheme.typography.bodyLarge,
                          textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
              } else {
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(
                                horizontal = CONSTANTS.PADDING_HORIZONTAL,
                                vertical = CONSTANTS.PADDING_VERTICAL)
                            .testTag(FriendListScreenTestTags.FRIEND_COLUMN),
                    verticalArrangement =
                        Arrangement.spacedBy(CONSTANTS.VERTICAL_ARRANGEMENT_SPACE)) {
                      uiState.friends.forEach { friend ->
                        FriendCard(
                            friend = friend,
                            onClick = { onFriendClick(friend) },
                            onConfirmDelete = { friendListViewModel.deleteFriend(it) })
                      }
                    }
              }
            }
      })
}

/**
 * UI representation of a single friend in the list.
 *
 * This card shows:
 * - the friend's avatar
 * - their pseudo
 * - their gardening skill
 * - their favorite plant
 * - an icon on the right to delete a friend
 *
 * @param friend user profile to display inside the card
 * @param onClick callback triggered when the card is clicked
 * @param onConfirmDelete the callback to trigger when the user confirmed the deletion of the friend
 *   via the popup
 */
@Composable
private fun FriendCard(
    friend: UserProfile,
    onClick: () -> Unit,
    onConfirmDelete: (UserProfile) -> Unit
) {
  val context = LocalContext.current
  val pseudo = friend.pseudo
  val avatar: Avatar = friend.avatar
  var showPopup by remember { mutableStateOf(false) }

  Card(
      onClick = onClick,
      modifier =
          Modifier.fillMaxWidth()
              .height(CONSTANTS.CARD_HEIGHT)
              .testTag(FriendListScreenTestTags.REQUEST_CARD)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Row(
              modifier = Modifier.fillMaxWidth(CONSTANTS.FRACTION_ROW_WIDTH),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                // Avatar
                Card(
                    modifier =
                        Modifier.clip(CircleShape)
                            .size(CONSTANTS.AVATAR_SIZE)
                            .testTag(FriendListScreenTestTags.FRIEND_AVATAR)) {
                      Image(
                          painter = painterResource(avatar.resId),
                          contentDescription =
                              context.getString(R.string.avatar_description_friend_screen, pseudo),
                          modifier = Modifier.fillMaxSize())
                    }

                // Pseudo + details (gardening skill & favorite plant)
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(CONSTANTS.FRACTION_BOX_WIDTH),
                    contentAlignment = Alignment.CenterStart) {
                      Column {
                        Text(
                            text = pseudo,
                            fontWeight = FontWeight.Bold,
                            fontSize = CONSTANTS.TEXT_FONT_SIZE,
                            maxLines = CONSTANTS.NUMBER_OF_LINES,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag(FriendListScreenTestTags.FRIEND_PSEUDO))
                        Text(
                            text = friend.gardeningSkill,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = CONSTANTS.NUMBER_OF_LINES,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            text = friend.favoritePlant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = CONSTANTS.NUMBER_OF_LINES,
                            overflow = TextOverflow.Ellipsis)
                      }
                    }

                // Garden icon (placeholder for future navigation or action)
                Card(
                    modifier =
                        Modifier.size(CONSTANTS.DEL_BUTTON_SIZE)
                            .clickable(onClick = { showPopup = true })
                            .testTag(FriendListScreenTestTags.getDelButtonForFriend(friend.id)),
                    border =
                        BorderStroke(
                            CONSTANTS.DEL_BUTTON_BORDER_STROKE, MaterialTheme.colorScheme.error),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error)) {
                      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(R.drawable.delete_friend_icon),
                            contentDescription =
                                stringResource(R.string.friend_list_bin_icon_description),
                        )
                      }
                    }
              }
        }
      }
  // show the popup if needed and dismiss it when either button is clicked
  if (showPopup) {
    DeleteFriendPopup(
        onDelete = {
          onConfirmDelete(friend)
          showPopup = false
        },
        onCancel = { showPopup = false },
        friendPseudo = friend.pseudo)
  }
}
