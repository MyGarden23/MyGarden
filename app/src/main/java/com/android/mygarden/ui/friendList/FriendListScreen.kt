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

/** Test tags spécifiques à l'écran de liste d'amis. */
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendListScreen(
    onBackPressed: () -> Unit = {},
    friendListViewModel: FriendListViewModel = viewModel(),
) {
  val context = LocalContext.current
  val uiState by friendListViewModel.uiState.collectAsState()

  // Charge la liste d'amis au premier affichage
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
            title = stringResource(R.string.friend_list_screen), // "All My Friends"
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
                // État vide : aucun ami
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                  Text(
                      text =
                          stringResource(
                              R.string.friend_list_no_friend), // "You don’t have any friend..."
                      modifier = Modifier.testTag(FriendListScreenTestTags.NO_FRIEND),
                      style = MaterialTheme.typography.bodyLarge)
                }
              } else {
                // Liste d'amis
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .padding(horizontal = PADDING_HORIZONTAL, vertical = PADDING_VERTICAL)
                            .testTag(FriendListScreenTestTags.FRIEND_COLUMN),
                    verticalArrangement = Arrangement.spacedBy(VERTICAL_ARRANGEMENT_SPACE)) {
                      uiState.friends.forEach { friend -> FriendCard(friend = friend) }
                    }
              }
            }
      })
}

@Composable
private fun FriendCard(friend: UserProfile) {
  val context = LocalContext.current
  val pseudo = friend.pseudo
  val avatar: Avatar = friend.avatar
  val gardeningSkill = friend.gardeningSkill
  val favoritePlant = friend.favoritePlant

  Card(
      modifier =
          Modifier.fillMaxWidth()
              .height(CARD_HEIGHT)
              .testTag(FriendListScreenTestTags.REQUEST_CARD)) {
        // Same pattern as in AddFriendScreen: Box centering a Row with FRACTION_ROW_WIDTH
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
          Row(
              modifier = Modifier.fillMaxWidth(FRACTION_ROW_WIDTH),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween) {
                // Avatar (same style as AddFriend)
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

                // Text block (pseudo + 2 lines) instead of just pseudo
                Box(
                    modifier = Modifier.fillMaxHeight().fillMaxWidth(FRACTION_BOX_WIDTH),
                    contentAlignment = Alignment.CenterStart) {
                      Column {
                        Text(
                            text = pseudo,
                            fontWeight = FontWeight.Bold,
                            fontSize = TEXT_FONT_SIZE,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag(FriendListScreenTestTags.FRIEND_PSEUDO))
                        Text(
                            text = gardeningSkill,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                        Text(
                            text = favoritePlant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                      }
                    }

                // Icon on the right instead of the button
                Icon(
                    painter = painterResource(R.drawable.potted_plant_icon),
                    contentDescription =
                        stringResource(R.string.friend_list_plant_icon_description),
                    modifier = Modifier.testTag(FriendListScreenTestTags.PLANT_ICON_TO_GARDEN))
              }
        }
      }
}
