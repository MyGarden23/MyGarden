package com.android.mygarden.ui.friendList

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.mygarden.R

/** Constants used for UI display (mostly padding) */
private object PopupDp {
  val POPUP_CARD_HEIGHT = 136.dp
  val POPUP_CORNER_SHAPE = 16.dp
  val POPUP_CARD_INNER_PD = 16.dp
  val POPUP_BETWEEN_TEXT_PD = 6.dp
  val POPUP_BETWEEN_TEXT_AND_BUTTONS_PD = 12.dp
}

/** Test tags to ensure correct UI behaviour of the popup */
object DeleteFriendPopupTestTags {
  const val POPUP = "DeleteFriendWholePopup"
  const val CANCEL_BUTTON = "DeleteFriendPopupCancelButton"
  const val DELETE_BUTTON = "DeleteFriendPopupDeleteButton"
}

/**
 * Popup that pops on the screen when the current user wants to delete a friend to ensure he wants
 * to delete
 *
 * @param modifier for UI design
 * @param onDelete the callback to trigger when the user clicks on the delete button
 * @param onCancel the callback to trigger when the user clicks on the keep friend button
 * @param friendPseudo the pseudo of the friend that the user wants to delete
 */
@Composable
fun DeleteFriendPopup(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    friendPseudo: String = ""
) {
  Dialog(
      onDismissRequest = onCancel,
      content = {
        Card(
            modifier =
                modifier.height(PopupDp.POPUP_CARD_HEIGHT).testTag(DeleteFriendPopupTestTags.POPUP),
            shape = RoundedCornerShape(PopupDp.POPUP_CORNER_SHAPE),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
              Column(
                  modifier = modifier.fillMaxSize().padding(PopupDp.POPUP_CARD_INNER_PD),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.delete_friend_title, friendPseudo),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = modifier.height(PopupDp.POPUP_BETWEEN_TEXT_PD))
                    Text(
                        text = stringResource(R.string.delete_friend_description),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = modifier.height(PopupDp.POPUP_BETWEEN_TEXT_AND_BUTTONS_PD))
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                          DeleteFriendPopupButton(
                              onCancel,
                              ButtonDefaults.buttonColors(
                                  containerColor = MaterialTheme.colorScheme.outline,
                                  contentColor = MaterialTheme.colorScheme.onError),
                              stringResource(R.string.keep_friend_button_text),
                              modifier.testTag(DeleteFriendPopupTestTags.CANCEL_BUTTON))
                          DeleteFriendPopupButton(
                              onDelete,
                              ButtonDefaults.buttonColors(
                                  containerColor = MaterialTheme.colorScheme.error,
                                  contentColor = MaterialTheme.colorScheme.onError),
                              stringResource(R.string.delete),
                              modifier.testTag(DeleteFriendPopupTestTags.DELETE_BUTTON))
                        }
                  }
            }
      })
}

/**
 * Template function for the two buttons of the DeletePlantPopup composable.
 *
 * @param onClick the callback called on click of the button
 * @param colors all the colors used for this button
 * @param text the text to display on this button
 * @param modifier the optional modifier of the composable
 */
@Composable
private fun DeleteFriendPopupButton(
    onClick: () -> Unit,
    colors: ButtonColors,
    text: String,
    modifier: Modifier = Modifier
) {
  Button(
      onClick = onClick,
      colors = colors,
      shape = RoundedCornerShape(PopupDp.POPUP_CORNER_SHAPE),
      modifier = modifier) {
        Text(text = text)
      }
}
