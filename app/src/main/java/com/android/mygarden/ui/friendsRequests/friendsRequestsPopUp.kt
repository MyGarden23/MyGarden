package com.android.mygarden.ui.friendsRequests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.mygarden.R
import com.android.mygarden.ui.popup.PopupScreenTestTags
import com.android.mygarden.ui.theme.MyGardenTheme

private val CARD_HEIGHT = 230.dp
private val CARD_PADDING = 16.dp
private val CARD_ROUND_ANGLE = 16.dp
private val DISMISS_BUTTON_PADDING = 6.dp
private val TITLE_PADDING = 8.dp
private val CONFIRM_BUTTON_PADDING = 16.dp

/**
 * Popup displayed when the user receives a friend request.
 *
 * @param senderName Name of the user who sent the request
 * @param onDismiss Callback when clicking outside or on the cross
 * @param onConfirm Callback when clicking on the "Go to Request" button
 */
@Composable
fun FriendsRequestsPopup(
    senderName: String,
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier =
            Modifier.testTag(PopupScreenTestTags.CARD)
                .fillMaxWidth()
                .height(CARD_HEIGHT)
                .padding(CARD_PADDING),
        shape = RoundedCornerShape(CARD_ROUND_ANGLE),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
          Column(
              modifier = Modifier.fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally) {

                // Button to close the popUp
                Row(modifier = Modifier.fillMaxWidth().padding(DISMISS_BUTTON_PADDING)) {
                  IconButton(
                      modifier = Modifier.testTag(PopupScreenTestTags.DISMISS_BUTTON),
                      onClick = onDismiss) {
                        Icon(
                            painter = painterResource(R.drawable.x_circle),
                            contentDescription =
                                stringResource(R.string.popup_dismiss_button_content_description),
                            tint = MaterialTheme.colorScheme.error)
                      }
                }

                // Title of the popUp
                Text(
                    modifier = Modifier.padding(TITLE_PADDING).testTag(PopupScreenTestTags.TITLE),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    text = stringResource(R.string.friend_pop_up_title, senderName))

                // Confirm button
                Button(
                    modifier =
                        Modifier.padding(CONFIRM_BUTTON_PADDING)
                            .testTag(PopupScreenTestTags.CONFIRM_BUTTON),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary),
                    onClick = onConfirm) {
                      Text(text = stringResource(R.string.friend_popup_confirm_button_text))
                    }
              }
        }
  }
}

@Preview
@Composable
fun darkP() {
  MyGardenTheme(true) { FriendsRequestsPopup("matteo") }
}

@Preview
@Composable
fun lightP() {
  MyGardenTheme() { FriendsRequestsPopup("matteo") }
}
