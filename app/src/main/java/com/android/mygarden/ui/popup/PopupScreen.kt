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
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.mygarden.R
import com.android.mygarden.ui.theme.MyGardenTheme

/**
 * Popup that is on screen whenever a plant passes in state NEED_WATER
 *
 * @param plantName the name of the plant that changed status
 * @param onDismiss the action to do when clicking outside the popup or on the cross button
 * @param onConfirm the action to do when clicking on the confirm button (Go to Garden)
 */
@Composable
fun Popup(
  // default plant name and actions for @Preview
  plantName: String = "GegeLeCactus",
  onDismiss: () -> Unit = {},
  onConfirm: () -> Unit = {}
) {
  Dialog(
    onDismissRequest = {onDismiss},
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .height(230.dp)
        .padding(16.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardColors(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        disabledContainerColor = MaterialTheme.colorScheme.background,
        disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      )
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // A row to have the red cross button only on top and the rest below
        Row(
          modifier = Modifier.fillMaxWidth().padding(6.dp)
        ) {
          QuitPopup(onClick = onDismiss)
        }
        PopupTitle("Your $plantName is thirsty !")
        // Spacer puts the confirm button at the end of the popup
        Spacer(Modifier.weight(1f))
        PopupButton(onClick = {onConfirm}, text = "Go to Garden")
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
fun PopupTitle(
  text: String
) {
  Text(
    modifier = Modifier.padding(8.dp),
    textAlign = TextAlign.Center,
    style = MaterialTheme.typography.titleLarge,
    fontWeight = FontWeight.Bold,
    text = text
  )
}

/**
 * Confirm button to be displayed
 *
 * @param onClick the action to do when clicked on
 * @param text the text inside the button
 */
@Composable
fun PopupButton(
  onClick: () -> Unit,
  text: String
) {
  Button(
    modifier = Modifier.padding(16.dp),
    colors = ButtonColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
      disabledContainerColor = MaterialTheme.colorScheme.primary,
      disabledContentColor = MaterialTheme.colorScheme.onPrimary,
    ),
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
fun QuitPopup(
  onClick: () -> Unit
) {
  IconButton(onClick = onClick) {
    Icon(
      painter = painterResource(R.drawable.x_circle),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.error)
  }
}

@Preview
@Composable
fun Preview() {
  MyGardenTheme { Popup() }
}