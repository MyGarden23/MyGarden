package com.android.mygarden.ui.feed

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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
private val CARD_CORNER_RADIUS = 10.dp
private val CARD_ELEVATION = 4.dp
private val SPACER_BETWEEN_TEXT_AND_BUTTON = 4.dp
private const val COLUMN_WEIGHT = 1f

@Composable
fun FriendActivityPopup(
    onDismiss: () -> Unit = {},
    feedViewModel: FeedViewModel,
) {
  val context = LocalContext.current

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
                  FriendsPopupCard(uiState.watchedUser1, context)
                  FriendsPopupCard(uiState.watchedUser2, context)
                }
          }
    }
  }
}

@Composable
fun FriendsPopupCard(userProfile: UserProfile?, context: Context) {
  if (userProfile == null) return
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
                      contentDescription =
                          context.getString(
                              R.string.avatar_description_friend_screen, userProfile.pseudo),
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
                          modifier = Modifier,
                          onClick = getOnClickActionForPopup(),
                          colors =
                              ButtonDefaults.buttonColors(
                                  MaterialTheme.colorScheme.primaryContainer),
                          content = {
                            Text(
                                text = getButtonText(),
                                maxLines = MAX_LINES,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                          })
                    }
              }
        }
      }
}

fun getOnClickActionForPopup(): () -> Unit {
  return {}
}

fun getButtonText(): String {
  return "See Garden"
}
