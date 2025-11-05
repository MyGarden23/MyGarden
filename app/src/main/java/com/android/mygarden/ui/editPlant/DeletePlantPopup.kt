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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Test tags for [DeletePlantPopup]. */
object DeletePlantPopupTestTags {
  const val POPUP = "DeletePlantPopup"
  const val DELETE_QUESTION = "DeletePlantPopupQuestion"
  const val DELETE_DESCRIPTION = "DeletePlantPopupDescription"
  const val CONFIRM_BUTTON = "PlantPopupConfirmButton"
  const val CANCEL_BUTTON = "PlantPopupDeleteQuestion"
}

const val DELETE_PLANT_QUESTION = "Delete this plant?"
const val DELETE_PLANT_DESCRIPTION = "This action can’t be undone."
const val KEEP_BUTTON_TEXT = "Keep in my garden"
const val DELETE_BUTTON_TEXT = "Delete"

/**
 * Popup that asks users to confirm they want to delete a selected plant.
 *
 * @param onDelete the callback called when the plant is confirmed to be deleted
 * @param onCancel the callback called when the popup is dismissed or user want to keep their plant
 * @param modifier the optional modifier of the composable
 */
@Composable
fun DeletePlantPopup(onDelete: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
  Dialog(
      onDismissRequest = onCancel,
      content = {
        Card(
            modifier = modifier.height(136.dp).testTag(DeletePlantPopupTestTags.POPUP),
            shape = RoundedCornerShape(16.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
              Column(
                  modifier = modifier.fillMaxSize().padding(16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DELETE_PLANT_QUESTION,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = modifier.testTag(DeletePlantPopupTestTags.DELETE_QUESTION))
                    Spacer(modifier = modifier.height(6.dp))
                    Text(
                        text = DELETE_PLANT_DESCRIPTION,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = modifier.testTag(DeletePlantPopupTestTags.DELETE_DESCRIPTION))
                    Spacer(modifier = modifier.height(12.dp))
                    Row(
                        modifier = modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly) {
                          DeletePlantPopupButton(
                              onCancel,
                              ButtonDefaults.buttonColors(
                                  containerColor = MaterialTheme.colorScheme.outline,
                                  contentColor = MaterialTheme.colorScheme.onError),
                              KEEP_BUTTON_TEXT,
                              modifier.testTag(DeletePlantPopupTestTags.CANCEL_BUTTON))
                          DeletePlantPopupButton(
                              onDelete,
                              ButtonDefaults.buttonColors(
                                  containerColor = MaterialTheme.colorScheme.error,
                                  contentColor = MaterialTheme.colorScheme.onError),
                              DELETE_BUTTON_TEXT,
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
      onClick = onClick, colors = colors, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Text(text = text)
      }
}
