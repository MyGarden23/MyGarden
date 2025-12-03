package com.android.mygarden.ui.friendsRequests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import com.android.mygarden.R
import com.android.mygarden.ui.popup.PopUpDimensions
import com.android.mygarden.ui.popup.PopupButton
import com.android.mygarden.ui.popup.PopupScreenTestTags
import com.android.mygarden.ui.popup.PopupTitle
import com.android.mygarden.ui.popup.QuitPopup

/**
 * Popup displayed when the user receives a friend request.
 *
 * @param senderPseudo Pseudo of the user who sent the request
 * @param onDismiss Callback when clicking outside or on the cross
 * @param onConfirm Callback when clicking on the "Go to Request" button
 */
@Composable
fun FriendsRequestsPopup(
    senderPseudo: String,
    onDismiss: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
        modifier =
            Modifier.testTag(PopupScreenTestTags.CARD)
                .fillMaxWidth()
                .height(PopUpDimensions.CARD_HEIGHT)
                .padding(PopUpDimensions.CARD_PADDING),
        shape = RoundedCornerShape(PopUpDimensions.CARD_ROUND_ANGLE),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
          Column(
              modifier = Modifier.fillMaxSize(),
              horizontalAlignment = Alignment.CenterHorizontally) {

                // Button to close the popUp
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(PopUpDimensions.DISMISS_BUTTON_PADDING)) {
                      QuitPopup(onClick = onDismiss)
                    }

                // Title of the popUp
                PopupTitle(text = stringResource(R.string.friend_pop_up_title, senderPseudo))

                // Confirm button
                PopupButton(
                    onClick = onConfirm,
                    text = stringResource(R.string.friend_popup_confirm_button_text))
              }
        }
  }
}
