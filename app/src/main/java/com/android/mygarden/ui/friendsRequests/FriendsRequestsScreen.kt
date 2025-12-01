package com.android.mygarden.ui.friendsRequests

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.ui.friendsRequests.CONSTANTS as DP
import com.android.mygarden.ui.navigation.TopBar
import com.android.mygarden.ui.theme.ExtendedTheme

/** All constants needed for padding or sizes of the composables below */
private object CONSTANTS {
  val FIRST_REQUEST_SPACER = 20.dp
  val BETWEEN_REQUESTS_SPACE = 32.dp
  val CARD_HORIZONTAL_PADDING = 32.dp
  val CARD_ELEVATION = 6.dp
  val CARD_ROUNDED_CORNER = 24.dp
  val CARD_FULL_INNER_PADDING = 8.dp
  val CARD_PSEUDO_ROW_VERTICAL_PADDING = 8.dp
  val AVATAR_SIZE = 56.dp
  val PSEUDO_SIZE = 20.sp
  val BUTTON_HORIZONTAL_PADDING = 12.dp
  val BUTTON_VERTICAL_PADDING = 8.dp
  val BUTTON_ROUNDED_CORNER = 18.dp
  val BUTTON_TEXT_HORIZONTAL_PADDING = 32.dp
  val BUTTON_TEXT_VERTICAL_PADDING = 4.dp
  val NO_REQUEST_TEXT_PADDING = 40.dp
}

/** All test tags needed to ensure all components are correctly displayed when needed */
object FriendsRequestsScreenTestTags {
  const val NO_REQUEST_TEXT = "NoRequestText"

  fun getRequestCardFromUser(pseudo: String) = "RequestCardFrom${pseudo}"

  fun getRequestAvatarFromUser(pseudo: String) = "RequestAvatarFrom${pseudo}"

  fun getRequestAcceptButtonFromUser(pseudo: String) = "RequestAcceptButtonFrom${pseudo}"

  fun getRequestDeclineButtonFromUser(pseudo: String) = "RequestDeclineButtonFrom${pseudo}"
}

/**
 * Screen displaying all friends requests the current user has. Each request comes with an accept
 * and a decline button.
 *
 * @param modifier the modifier used throughout the whole screen for correct design
 * @param onGoBack the callback to be triggered when the user clicks on the back button
 */
@Composable
fun FriendsRequestsScreen(
    modifier: Modifier = Modifier,
    requestsViewModel: FriendsRequestsViewModel = viewModel(),
    onGoBack: () -> Unit = {}
) {
  val uiState by requestsViewModel.uiState.collectAsState()
  val requestsUsers = uiState.pendingRequestsUsers

  Scaffold(
      modifier = modifier,
      topBar = {
        TopBar(
            title = stringResource(R.string.request_screen_title),
            hasGoBackButton = true,
            onGoBack = onGoBack)
      },
      content = { pd ->
        Column(modifier = modifier.fillMaxSize().padding(pd)) {
          // creates a space between the top bar and the first request for better design
          Spacer(modifier = modifier.height(DP.FIRST_REQUEST_SPACER))
          if (requestsUsers.isEmpty()) {
            // displays a text saying the user has no friend request at the center of the screen if
            // no request
            Box(
                modifier = modifier.fillMaxSize().padding(DP.NO_REQUEST_TEXT_PADDING),
                contentAlignment = Alignment.Center) {
                  Text(
                      modifier = Modifier.testTag(FriendsRequestsScreenTestTags.NO_REQUEST_TEXT),
                      text = stringResource(R.string.no_request_text))
                }
          } else {
            // displays all requests when there are any
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DP.BETWEEN_REQUESTS_SPACE)) {
                  items(requestsUsers.size) {
                    val potentialFriend = requestsUsers[it]
                    RequestItem(
                        potentialNewFriend = potentialFriend,
                        onRefuse = { requestsViewModel.declineRequest(potentialFriend.id) },
                        onAccept = { requestsViewModel.acceptRequest(potentialFriend.id) })
                  }
                }
          }
        }
      })
}

/**
 * Represents a single request card, with displayed the avatar of the user requesting friendship,
 * their pseudo, and the accept/decline buttons.
 *
 * @param modifier the modifier used for UI design
 * @param potentialNewFriend the profile of the user requesting friendship
 * @param onRefuse the callback to be triggered when the user clicks on the decline button
 * @param onAccept the callback to be triggered when the user clicks on the accept button
 */
@Composable
fun RequestItem(
    modifier: Modifier = Modifier,
    potentialNewFriend: UserProfile,
    onRefuse: () -> Unit = {},
    onAccept: () -> Unit = {}
) {
  Card(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = DP.CARD_HORIZONTAL_PADDING)
              .testTag(
                  FriendsRequestsScreenTestTags.getRequestCardFromUser(potentialNewFriend.pseudo)),
      elevation = CardDefaults.cardElevation(defaultElevation = DP.CARD_ELEVATION),
      shape = RoundedCornerShape(DP.CARD_ROUNDED_CORNER),
      colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(modifier = modifier.fillMaxWidth().padding(DP.CARD_FULL_INNER_PADDING)) {
          // Row with the avatar image and the pseudo
          Row(
              modifier =
                  modifier.fillMaxWidth().padding(vertical = DP.CARD_PSEUDO_ROW_VERTICAL_PADDING),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceEvenly) {
                Image(
                    painter = painterResource(potentialNewFriend.avatar.resId),
                    contentDescription =
                        stringResource(R.string.avatar_description, potentialNewFriend.avatar.name),
                    modifier =
                        modifier
                            .size(DP.AVATAR_SIZE)
                            .clip(CircleShape)
                            .testTag(
                                FriendsRequestsScreenTestTags.getRequestAvatarFromUser(
                                    potentialNewFriend.pseudo)))
                Text(
                    fontSize = DP.PSEUDO_SIZE,
                    textAlign = TextAlign.Center,
                    text = potentialNewFriend.pseudo)
              }
          // Row with the 2 buttons
          Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            RequestButton(
                modifier =
                    modifier.testTag(
                        FriendsRequestsScreenTestTags.getRequestAcceptButtonFromUser(
                            potentialNewFriend.pseudo)),
                accept = false,
                onClick = onRefuse)
            RequestButton(
                modifier =
                    modifier.testTag(
                        FriendsRequestsScreenTestTags.getRequestDeclineButtonFromUser(
                            potentialNewFriend.pseudo)),
                accept = true,
                onClick = onAccept)
          }
        }
      }
}

/**
 * Represents the UI of a button, whether is the accept or decline button
 *
 * @param modifier the modifier to be used for UI design
 * @param accept boolean to decide whether to design an accept or decline button
 * @param onClick callback to be triggered when the user clicks on the button
 */
@Composable
fun RequestButton(modifier: Modifier = Modifier, accept: Boolean, onClick: () -> Unit = {}) {
  Card(
      modifier =
          modifier
              .padding(horizontal = DP.BUTTON_HORIZONTAL_PADDING)
              .padding(vertical = DP.BUTTON_VERTICAL_PADDING)
              .clickable(onClick = onClick),
      shape = RoundedCornerShape(DP.BUTTON_ROUNDED_CORNER),
      colors =
          CardDefaults.cardColors(
              containerColor =
                  if (accept) ExtendedTheme.colors.acceptButtonColor
                  else ExtendedTheme.colors.refuseButtonColor)) {
        Text(
            modifier =
                modifier
                    .padding(horizontal = DP.BUTTON_TEXT_HORIZONTAL_PADDING)
                    .padding(vertical = DP.BUTTON_TEXT_VERTICAL_PADDING),
            color = Color.White,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            text =
                if (accept) stringResource(R.string.accept_button)
                else stringResource(R.string.decline_button))
      }
}
