package com.android.mygarden.ui.popup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.mygarden.R

object PopupScreenTestTags {
  const val CARD = "Card"
  const val TITLE = "Title"
  const val DISMISS_BUTTON = "DismissButton"
  const val CONFIRM_BUTTON = "ConfirmButton"
}

const val POPUP_DISMISS_BUTTON_CONTENT_DESCRIPTION = "Quit the popup"
const val POPUP_CONFIRM_BUTTON_TEXT = "Go to Garden"

fun getPopupTitle(plantName: String): String = "Your $plantName is thirsty!"

/**
 * Popup that is on screen whenever a plant passes in state NEED_WATER
 *
 * @param plantName the name of the plant that changed status
 * @param onDismiss the action to do when clicking outside the popup or on the cross button
 * @param onConfirm the action to do when clicking on the confirm button (Go to Garden)
 */
@Composable
fun WaterPlantPopup(plantName: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
  Dialog(
      onDismissRequest = onDismiss,
  ) {
    Card(
        modifier =
            Modifier.testTag(PopupScreenTestTags.CARD).fillMaxWidth().height(230.dp).padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
    ) {
      Column(
          modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // A row to have the red cross button only on top and the rest below
            Row(modifier = Modifier.fillMaxWidth().padding(6.dp)) { QuitPopup(onClick = onDismiss) }
            PopupTitle(text = getPopupTitle(plantName))
            // Spacer puts the confirm button at the end of the popup
            Spacer(Modifier.weight(1f))
            PopupButton(onClick = onConfirm, text = POPUP_CONFIRM_BUTTON_TEXT)
          }
    }
  }
}

/**
 * Title of the popup to be displayed
 *
 * @param text the title text
 */
@Composable
fun PopupTitle(text: String) {
  Text(
      modifier = Modifier.padding(8.dp).testTag(PopupScreenTestTags.TITLE),
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      text = text)
}

/**
 * Confirm button to be displayed
 *
 * @param onClick the action to do when clicked on
 * @param text the text inside the button
 */
@Composable
fun PopupButton(onClick: () -> Unit, text: String) {
  Button(
      modifier = Modifier.padding(16.dp).testTag(PopupScreenTestTags.CONFIRM_BUTTON),
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary),
      onClick = onClick) {
        Text(text = text)
      }
}

/**
 * The button to quit the Popup
 *
 * @param onClick the action to do when clicked on
 */
@Composable
fun QuitPopup(onClick: () -> Unit) {
  IconButton(modifier = Modifier.testTag(PopupScreenTestTags.DISMISS_BUTTON), onClick = onClick) {
    Icon(
        painter = painterResource(R.drawable.x_circle),
        contentDescription = POPUP_DISMISS_BUTTON_CONTENT_DESCRIPTION,
        tint = MaterialTheme.colorScheme.error)
  }
}
