package com.android.mygarden.ui.editPlant

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.android.mygarden.R

/** Dimension constants for DeletePlantPopup */
private val POPUP_HEIGHT = 136.dp
private val POPUP_CORNER_RADIUS = 16.dp
private val POPUP_PADDING = 16.dp
private val SPACER_SMALL = 6.dp
private val SPACER_MEDIUM = 12.dp
private val BUTTON_CORNER_RADIUS = 16.dp

/** Test tags for [DeletePlantPopup]. */
object DeletePlantPopupTestTags {
  const val POPUP = "DeletePlantPopup"
  const val QUESTION = "DeletePlantPopupQuestion"
  const val DESCRIPTION = "DeletePlantPopupDescription"
  const val CONFIRM_BUTTON = "PlantPopupConfirmButton"
  const val CANCEL_BUTTON = "PlantPopupDeleteQuestion"
}

/**
 * Popup that asks users to confirm they want to delete a selected plant.
 *
 * @param onDelete the callback called when the plant is confirmed to be deleted
 * @param onCancel the callback called when the popup is dismissed or user want to keep their plant
 * @param modifier the optional modifier of the composable
 */
@Composable
fun DeletePlantPopup(onDelete: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
  val context = LocalContext.current
  Dialog(
      onDismissRequest = onCancel,
      content = {
        Card(
            modifier = modifier.height(POPUP_HEIGHT).testTag(DeletePlantPopupTestTags.POPUP),
            shape = RoundedCornerShape(POPUP_CORNER_RADIUS),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
              Column(
                  modifier = modifier.fillMaxSize().padding(POPUP_PADDING),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = context.getString(R.string.delete_plant_question),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = modifier.testTag(DeletePlantPopupTestTags.QUESTION))
                    Spacer(modifier = modifier.height(SPACER_SMALL))
                    Text(
                        text = context.getString(R.string.delete_plant_description),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = modifier.testTag(DeletePlantPopupTestTags.DESCRIPTION))
                    Spacer(modifier = modifier.height(SPACER_MEDIUM))
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                          DeletePlantPopupButton(
                              onCancel,
                              ButtonDefaults.buttonColors(
                                  containerColor = MaterialTheme.colorScheme.outline,
                                  contentColor = MaterialTheme.colorScheme.onError),
                              context.getString(R.string.keep_button_text),
                              modifier.testTag(DeletePlantPopupTestTags.CANCEL_BUTTON))
                          DeletePlantPopupButton(
                              onDelete,
                              ButtonDefaults.buttonColors(
                                  containerColor = MaterialTheme.colorScheme.error,
                                  contentColor = MaterialTheme.colorScheme.onError),
                              context.getString(R.string.delete),
                              modifier.testTag(DeletePlantPopupTestTags.CONFIRM_BUTTON))
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
private fun DeletePlantPopupButton(
    onClick: () -> Unit,
    colors: ButtonColors,
    text: String,
    modifier: Modifier = Modifier
) {
  Button(
      onClick = onClick,
      colors = colors,
      shape = RoundedCornerShape(BUTTON_CORNER_RADIUS),
      modifier = modifier) {
        Text(text = text)
      }
}
