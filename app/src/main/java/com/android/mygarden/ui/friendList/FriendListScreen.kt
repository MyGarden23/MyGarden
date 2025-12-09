package com.android.mygarden.ui.friendList

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
  const val PLANT_ICON_TO_GARDEN = "plantIconToGarden"
  const val NO_FRIEND = "noFriendText"
}

const val FRACTION_SPACER = 0.02f
val PADDING_VERTICAL = 20.dp
val PADDING_HORIZONTAL = 35.dp
val VERTICAL_ARRANGEMENT_SPACE = 10.dp
val CARD_HEIGHT = 80.dp
const val FRACTION_ROW_WIDTH = 0.94f
const val FRACTION_BOX_WIDTH = 0.6f
val TEXT_FONT_SIZE = 20.sp

val NUMBER_OF_LINES = 1

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
              Spacer(modifier = Modifier.fillMaxHeight(FRACTION_SPACER))

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
                            .padding(horizontal = PADDING_HORIZONTAL, vertical = PADDING_VERTICAL)
                            .testTag(FriendListScreenTestTags.FRIEND_COLUMN),
                    verticalArrangement = Arrangement.spacedBy(VERTICAL_ARRANGEMENT_SPACE)) {
                      uiState.friends.forEach { friend ->
                        FriendCard(friend = friend, onClick = { onFriendClick(friend) })
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
 * - an icon on the right (currently a decorative or future action icon)
 *
 * @param friend user profile to display inside the card
 * @param onClick callback triggered when the card is clicked
 */
@Composable
private fun FriendCard(friend: UserProfile, onClick: () -> Unit) {
  val context = LocalContext.current
  val pseudo = friend.pseudo
  val avatar: Avatar = friend.avatar

  Card(
      onClick = onClick,
      modifier =
          Modifier.fillMaxWidth()
              .height(CARD_HEIGHT)
              .testTag(FriendListScreenTestTags.REQUEST_CARD)) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Row(
              modifier = Modifier.fillMaxWidth(FRACTION_ROW_WIDTH),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                // Avatar
                Card(
                    modifier =
                        Modifier.clip(CircleShape)
                            .size(CARD_HEIGHT)
                            .testTag(FriendListScreenTestTags.FRIEND_AVATAR)) {
                      Image(
                          painter = painterResource(avatar.resId),
                          contentDescription =
                              context.getString(R.string.avatar_description_friend_screen, pseudo),
                          modifier = Modifier.fillMaxSize())
                    }

                // Pseudo + details (gardening skill & favorite plant)
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(FRACTION_BOX_WIDTH),
                    contentAlignment = Alignment.CenterStart) {
                      Column {
                        Text(
                            text = pseudo,
                            fontWeight = FontWeight.Bold,
                            fontSize = TEXT_FONT_SIZE,
                            maxLines = NUMBER_OF_LINES,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag(FriendListScreenTestTags.FRIEND_PSEUDO))
                        Text(
                            text = friend.gardeningSkill,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = NUMBER_OF_LINES,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            text = friend.favoritePlant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = NUMBER_OF_LINES,
                            overflow = TextOverflow.Ellipsis)
                      }
                    }

                // Garden icon (placeholder for future navigation or action)
                Icon(
                    painter = painterResource(R.drawable.potted_plant_icon),
                    contentDescription =
                        stringResource(R.string.friend_list_plant_icon_description),
                    modifier = Modifier.testTag(FriendListScreenTestTags.PLANT_ICON_TO_GARDEN))
              }
        }
      }
}
