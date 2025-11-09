package com.android.mygarden.ui.editPlant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.android.mygarden.R
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.TopBar
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Test tags for [EditPlantScreen]. */
object EditPlantScreenTestTags {
  const val PLANT_IMAGE = "plantImage"
  const val PLANT_NAME = "plantName"
  const val PLANT_LATIN = "plantLatin"
  const val INPUT_PLANT_DESCRIPTION = "inputPlantDescription"
  const val INPUT_LAST_WATERED = "inputLastWatered"
  const val ERROR_MESSAGE_DATE = "errorMessageDate"
  const val ERROR_MESSAGE_DESCRIPTION = "errorMessageDescription"

  const val PLANT_SAVE = "plantSave"
  const val PLANT_DELETE = "plantDelete"
  const val DATE_PICKER_BUTTON = "datePicker"
}

/**
 * Composable screen for editing a plant’s details.
 *
 * Displays the plant’s image, read-only names, editable description, and last watered date (via
 * date picker). Handles validation and error messages, and provides Save/Delete actions.
 *
 * @param ownedPlantId ID of the plant to edit.
 * @param editPlantViewModel ViewModel managing UI state and actions.
 * @param onSaved Called after saving the plant.
 * @param onDeleted Called after deleting the plant. Null if the button does nothing and does not
 *   appears
 * @param goBack Called when navigating back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    ownedPlantId: String,
    editPlantViewModel: EditPlantViewModelInterface = viewModel<EditPlantViewModel>(),
    onSaved: () -> Unit = {},
    onDeleted: (() -> Unit)? = null,
    goBack: () -> Unit = {},
) {
  val context = LocalContext.current

  // Load the plant when the id changes
  LaunchedEffect(ownedPlantId) { editPlantViewModel.loadPlant(ownedPlantId) }

  val plantUIState by editPlantViewModel.uiState.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(plantUIState.errorMsg) {
    plantUIState.errorMsg?.let { msg ->
      snackbarHostState.showSnackbar(
          message = msg, withDismissAction = true, duration = SnackbarDuration.Short)
      editPlantViewModel.clearErrorMsg()
    }
  }

  // Mutable states needed for the UI
  var showDeletePopup by remember { mutableStateOf(false) }
  var touchedDesc by remember { mutableStateOf(false) }
  var touchedLastWatered by remember { mutableStateOf(false) }
  var showDatePicker by remember { mutableStateOf(false) }
  val dateFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

  val isDescriptionError = plantUIState.description.isBlank() && touchedDesc

  if (showDatePicker) {
    val initialMillis = plantUIState.lastWatered?.time
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = { showDatePicker = false },
        confirmButton = {
          TextButton(
              onClick = {
                val millis = pickerState.selectedDateMillis
                if (millis != null) {
                  editPlantViewModel.setLastWatered(Timestamp(millis))
                }
                showDatePicker = false
              }) {
                Text(context.getString(R.string.ok))
              }
        },
        dismissButton = {
          TextButton(onClick = { showDatePicker = false }) {
            Text(context.getString(R.string.cancel))
          }
        }) {
          DatePicker(state = pickerState)
        }
  }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.EDIT_PLANT_SCREEN),
      topBar = {
        TopBar(
            title = context.getString(R.string.edit_plant_screen_title),
            hasGoBackButton = true,
            onGoBack = goBack)
      },
      snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

              // Plant image
              if (plantUIState.image != null) {
                AsyncImage(
                    model =
                        ImageRequest.Builder(context)
                            .data(plantUIState.image)
                            .error(R.drawable.error_image_download)
                            .build(),
                    contentDescription = context.getString(R.string.plant_image_description),
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag(EditPlantScreenTestTags.PLANT_IMAGE),
                    contentScale = ContentScale.Crop)
              } else {
                // Placeholder if no image available
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .height(220.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag(EditPlantScreenTestTags.PLANT_IMAGE),
                    contentAlignment = Alignment.Center) {
                      Text(
                          text = context.getString(R.string.plant_image_no_image_available),
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
              }

              // Name (read-only)
              OutlinedTextField(
                  value = plantUIState.name,
                  onValueChange = {},
                  label = { Text(context.getString(R.string.name)) },
                  singleLine = true,
                  readOnly = true,
                  enabled = false,
                  modifier = Modifier.fillMaxWidth().testTag(EditPlantScreenTestTags.PLANT_NAME))

              // Latin name (read-only)
              OutlinedTextField(
                  value = plantUIState.latinName,
                  onValueChange = {},
                  label = { Text(context.getString(R.string.latin_name)) },
                  singleLine = true,
                  readOnly = true,
                  enabled = false,
                  modifier = Modifier.fillMaxWidth().testTag(EditPlantScreenTestTags.PLANT_LATIN))

              // Description
              OutlinedTextField(
                  value = plantUIState.description,
                  onValueChange = { editPlantViewModel.setDescription(it) },
                  label = { Text(context.getString(R.string.description)) },
                  minLines = 3,
                  isError = isDescriptionError,
                  modifier =
                      Modifier.fillMaxWidth()
                          .heightIn(min = 100.dp)
                          .testTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
                          .onFocusChanged { if (it.isFocused) touchedDesc = true })
              if (plantUIState.description.isBlank() && touchedDesc) {
                Text(
                    text = context.getString(R.string.description_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE_DESCRIPTION))
              }

              // Last time watered
              Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    context.getString(R.string.last_time_watered),
                    style = MaterialTheme.typography.labelLarge)

                val isDateError = plantUIState.lastWatered == null && touchedLastWatered
                OutlinedTextField(
                    value =
                        plantUIState.lastWatered?.let { ts ->
                          Instant.ofEpochMilli(ts.time)
                              .atZone(ZoneId.systemDefault())
                              .toLocalDate()
                              .format(dateFmt)
                        } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    isError = isDateError,
                    placeholder = { Text(context.getString(R.string.select_date)) },
                    trailingIcon = {
                      IconButton(
                          onClick = {
                            touchedLastWatered = true
                            showDatePicker = true
                          },
                          modifier = Modifier.testTag(EditPlantScreenTestTags.DATE_PICKER_BUTTON)) {
                            Icon(
                                Icons.Filled.CalendarMonth,
                                contentDescription =
                                    context.getString(R.string.pick_date_icon_description))
                          }
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                            .testTag(EditPlantScreenTestTags.INPUT_LAST_WATERED)
                            .onFocusChanged { if (it.isFocused) touchedLastWatered = true })
                if (isDateError) {
                  Text(
                      text = context.getString(R.string.last_time_watered_error),
                      color = MaterialTheme.colorScheme.error,
                      style = MaterialTheme.typography.bodySmall,
                      modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE_DATE))
                }
              }

              Spacer(Modifier.height(8.dp))

              // Save
              val isSaveEnabled =
                  plantUIState.description.isNotBlank() && plantUIState.lastWatered != null

              Button(
                  onClick = {
                    // ensure user sees the error if they never touched the field
                    if (plantUIState.lastWatered == null) touchedLastWatered = true
                    if (isSaveEnabled) {
                      editPlantViewModel.editPlant(ownedPlantId)
                      onSaved()
                    }
                  },
                  enabled = isSaveEnabled,
                  modifier =
                      Modifier.fillMaxWidth()
                          .height(56.dp)
                          .testTag(EditPlantScreenTestTags.PLANT_SAVE)) {
                    Text(context.getString(R.string.save))
                  }

              // Delete
              if (onDeleted != null) {
                TextButton(
                    onClick = { showDeletePopup = true },
                    modifier =
                        Modifier.fillMaxWidth().testTag(EditPlantScreenTestTags.PLANT_DELETE)) {
                      Icon(
                          Icons.Filled.Delete,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.error)
                      Spacer(Modifier.width(8.dp))
                      Text(
                          context.getString(R.string.delete),
                          color = MaterialTheme.colorScheme.error)
                    }

                // Show deletion popup when the according button is pressed
                if (showDeletePopup) {
                  DeletePlantPopup(
                      onDelete = {
                        editPlantViewModel.deletePlant(ownedPlantId)
                        showDeletePopup = false
                        onDeleted.invoke()
                      },
                      onCancel = { showDeletePopup = false })
                }
              }
            }
      }
}
