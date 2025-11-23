package com.android.mygarden.ui.editPlant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import com.android.mygarden.model.plant.*
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.TopBar
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min

/** Test tags for [EditPlantScreen]. */
object EditPlantScreenTestTags {
  const val SCROLLABLE_COLUMN = "scrollableColumn"
  const val PLANT_IMAGE = "plantImage"
  const val PLANT_NAME = "plantName"
  const val PLANT_LATIN = "plantLatin"
  const val LIGHT_EXPOSURE = "plantLightExposure"
  const val INPUT_PLANT_DESCRIPTION = "inputPlantDescription"
  const val LOCATION_TEXTFIELD = "plantLocation"
  const val LOCATION_DROPDOWN = "plantLocation"
  const val INPUT_LAST_WATERED = "inputLastWatered"
  const val ERROR_MESSAGE_DATE = "errorMessageDate"
  const val ERROR_MESSAGE_DESCRIPTION = "errorMessageDescription"
  const val ERROR_MESSAGE_NAME = "errorMessageName"
  const val ERROR_MESSAGE_LATIN_NAME = "errorMessageLatinName"
  const val ERROR_MESSAGE_LIGHT_EXPOSURE = "errorMessageLightExposure"

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
    plantUIState.errorMsg?.let { resId ->
      snackbarHostState.showSnackbar(
          message = context.getString(resId),
          withDismissAction = true,
          duration = SnackbarDuration.Short,
      )
      editPlantViewModel.clearErrorMsg()
    }
  }

  // Mutable states needed for the UI
  var showDeletePopup by remember { mutableStateOf(false) }
  var touchedDesc by remember { mutableStateOf(false) }
  var touchedName by remember { mutableStateOf(false) }
  var touchedLatinName by remember { mutableStateOf(false) }
  var touchedLight by remember { mutableStateOf(false) }
  var touchedLastWatered by remember { mutableStateOf(false) }
  var showDatePicker by remember { mutableStateOf(false) }
  val dateFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

  // groups the errors in only one place
  val errorFlags =
      remember(
          plantUIState,
          touchedName,
          touchedLatinName,
          touchedDesc,
          touchedLastWatered,
          touchedLight) {
            computeEditPlantErrorFlags(
                uiState = plantUIState,
                touchedName = touchedName,
                touchedLatinName = touchedLatinName,
                touchedDesc = touchedDesc,
                touchedLastWatered = touchedLastWatered,
                touchedLight = touchedLight)
          }

  if (showDatePicker) {
    EditPlantDatePickerDialog(
        initialMillis = plantUIState.lastWatered?.time,
        onConfirm = { millis ->
          handleDatePicked(millis, editPlantViewModel)
          showDatePicker = false
        },
        onDismiss = { showDatePicker = false },
    )
  }

  // Enable the Save button if the plant has been recognized by the API and the
  // lastWatered field is set and description is not blank or if the fields are all
  // filled if the plant is not recognized.
  val isSaveEnabled = remember(plantUIState) { computeIsSaveEnabled(plantUIState) }

  Scaffold(
      modifier = Modifier.testTag(NavigationTestTags.EDIT_PLANT_SCREEN),
      topBar = {
        TopBar(
            title = context.getString(R.string.edit_plant_screen_title),
            hasGoBackButton = true,
            onGoBack = goBack,
        )
      },
      snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { padding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .testTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // ------- Image -------
      PlantImageSection(imageUrl = plantUIState.image)

      // ------- Name -------
      NameFieldSection(
          name = plantUIState.name,
          isRecognized = plantUIState.isRecognized,
          isNameError = errorFlags.isNameError,
          onTouchedName = { touchedName = true },
          onNameChange = { newName ->
            if (!plantUIState.isRecognized) {
              editPlantViewModel.setName(newName)
            }
          },
      )

      // ------- Latin name -------
      LatinNameFieldSection(
          latinName = plantUIState.latinName,
          isRecognized = plantUIState.isRecognized,
          isLatinNameError = errorFlags.isLatinNameError,
          onTouchedLatinName = { touchedLatinName = true },
          onLatinNameChange = { newLatinName ->
            if (!plantUIState.isRecognized) {
              editPlantViewModel.setLatinName(newLatinName)
            }
          },
      )

      // ------- Description -------
      DescriptionFieldSection(
          description = plantUIState.description,
          isDescriptionError = errorFlags.isDescriptionError,
          onTouchedDesc = { touchedDesc = true },
          onDescriptionChange = { newDesc -> editPlantViewModel.setDescription(newDesc) },
      )

      // ------- Location -------
      LocationDropDownSelection(
          value = plantUIState.location.name,
          isPlantRecognized = plantUIState.isRecognized,
          onClickLocation = { editPlantViewModel.setLocation(it) })

      // ------- Light exposure -------
      LightExposureFieldSection(
          value = plantUIState.lightExposure,
          isLightError = errorFlags.isLightError,
          isPlantRecognized = plantUIState.isRecognized,
          onTouchedLight = { touchedLight = true },
          onLightExposureChange = {
            if (!plantUIState.isRecognized) editPlantViewModel.setLightExposure(it)
          })

      // ------- Last watered -------
      LastWateredSection(
          lastWatered = plantUIState.lastWatered,
          isDateError = errorFlags.isDateError,
          dateFmt = dateFmt,
          onTouchedLastWatered = { touchedLastWatered = true },
          onOpenDatePicker = {
            touchedLastWatered = true
            showDatePicker = true
          },
      )

      Spacer(Modifier.height(8.dp))

      // ------- Save -------
      SaveButtonSection(
          isSaveEnabled = isSaveEnabled,
          onValidate = {
            val touched =
                computeTouchedAfterValidate(
                    plantUIState,
                    touchedName,
                    touchedLatinName,
                    touchedDesc,
                    touchedLastWatered,
                    touchedLight)
            touchedName = touched.touchedName
            touchedLatinName = touched.touchedLatinName
            touchedDesc = touched.touchedDesc
            touchedLastWatered = touched.touchedLastWatered
            touchedLight = touched.touchedLight
          },
          onSave = {
            if (isSaveEnabled) {
              editPlantViewModel.editPlant(ownedPlantId)
              onSaved()
            }
          },
      )

      // ------- Delete -------
      DeleteSection(
          onDeleted = onDeleted,
          showDeletePopup = showDeletePopup,
          onShowDeletePopupChange = { showDeletePopup = it },
          onConfirmDelete = {
            editPlantViewModel.deletePlant(ownedPlantId)
            showDeletePopup = false
            onDeleted?.invoke()
          },
      )
    }
  }
}

/**
 * Aggregates all validation error states for the Edit Plant screen.
 *
 * Each flag indicates whether a specific field currently displays an error based on the UI state
 * and the user's interaction with the field.
 *
 * @property isNameError True when the name is invalid and should show an error.
 * @property isLatinNameError True when the Latin name is invalid and should show an error.
 * @property isDescriptionError True when the description is blank and marked as touched.
 * @property isDateError True when the last watered date is missing and marked as touched.
 * @property isLightError True when the light exposure is missing and marked as touched.
 */
private data class EditPlantErrorFlags(
    val isNameError: Boolean,
    val isLatinNameError: Boolean,
    val isDescriptionError: Boolean,
    val isDateError: Boolean,
    val isLightError: Boolean
)

/**
 * Tracks which input fields the user has interacted with.
 *
 * These flags allow error messages to be shown only after the user focuses a field, matching the
 * expected UX behavior of the Edit Plant screen.
 *
 * @property touchedName Whether the name field has been focused.
 * @property touchedLatinName Whether the Latin name field has been focused.
 * @property touchedDesc Whether the description field has been focused.
 * @property touchedLastWatered Whether the last watered date picker has been interacted with.
 * @property touchedLight Whether the light exposure field has been interacted with.
 */
private data class TouchedFlags(
    val touchedName: Boolean,
    val touchedLatinName: Boolean,
    val touchedDesc: Boolean,
    val touchedLastWatered: Boolean,
    val touchedLight: Boolean
)

/**
 * Handles the result returned by the date picker.
 *
 * Updates the ViewModel only when the user confirms a valid date.
 *
 * @param millis The selected date in epoch milliseconds, or null if no date was chosen.
 * @param editPlantViewModel ViewModel responsible for updating the last watered timestamp.
 */
private fun handleDatePicked(millis: Long?, editPlantViewModel: EditPlantViewModelInterface) {
  if (millis != null) {
    // Prevent storing a future date: clamp the selected millis to the current time
    val now = System.currentTimeMillis()
    val safeMillis = min(millis, now)
    editPlantViewModel.setLastWatered(Timestamp(safeMillis))
  }
}

/**
 * Computes all validation error states for the Edit Plant form.
 *
 * Each error flag is evaluated based on the current UI state and whether the corresponding field
 * has been touched. This centralizes the validation logic to keep the composable clean.
 *
 * @param uiState The current UI state of the Edit Plant screen.
 * @param touchedName Whether the name field has been interacted with.
 * @param touchedLatinName Whether the Latin name field has been interacted with.
 * @param touchedDesc Whether the description field has been interacted with.
 * @param touchedLastWatered Whether the date picker has been interacted with.
 * @param touchedLight Whether the light exposure has been interacted with.
 * @return A populated [EditPlantErrorFlags] instance representing current validation errors.
 */
private fun computeEditPlantErrorFlags(
    uiState: EditPlantUIState,
    touchedName: Boolean,
    touchedLatinName: Boolean,
    touchedDesc: Boolean,
    touchedLastWatered: Boolean,
    touchedLight: Boolean
): EditPlantErrorFlags {
  val isNameError = !uiState.isRecognized && uiState.name.isBlank() && touchedName
  val isLatinNameError = !uiState.isRecognized && uiState.latinName.isBlank() && touchedLatinName
  val isDescriptionError = uiState.description.isBlank() && touchedDesc
  val isDateError = uiState.lastWatered == null && touchedLastWatered
  val isLightError = !uiState.isRecognized && uiState.lightExposure.isBlank() && touchedLight

  return EditPlantErrorFlags(
      isNameError = isNameError,
      isLatinNameError = isLatinNameError,
      isDescriptionError = isDescriptionError,
      isDateError = isDateError,
      isLightError = isLightError)
}

/**
 * Determines whether the "Save" button should be enabled.
 *
 * Validation rules:
 * - If the plant was recognized by the API: only description and last watered date are required.
 * - If not recognized: name, Latin name, description, and last watered date must all be provided.
 *
 * @param uiState The current UI state of the Edit Plant screen.
 * @return True if all required fields are valid, false otherwise.
 */
private fun computeIsSaveEnabled(uiState: EditPlantUIState): Boolean {
  return if (uiState.isRecognized) {
    uiState.description.isNotBlank() && uiState.lastWatered != null
  } else {
    uiState.description.isNotBlank() &&
        uiState.name.isNotBlank() &&
        uiState.latinName.isNotBlank() &&
        uiState.lightExposure.isNotBlank() &&
        uiState.lastWatered != null
  }
}

/**
 * Updates the "touched" state of all input fields after the user presses Save.
 *
 * If a field is blank, it is automatically marked as touched so its error message becomes visible.
 * This ensures consistent UX: pressing Save reveals all missing fields.
 *
 * @param uiState The current UI state containing the input values.
 * @param touchedName Whether the name field has been previously touched.
 * @param touchedLatinName Whether the Latin name field has been previously touched.
 * @param touchedDesc Whether the description field has been previously touched.
 * @param touchedLastWatered Whether the last watered field has been previously touched.
 * @param touchedLight Whether the light exposure been previously touched.
 * @return A [TouchedFlags] instance with updated touched states for all fields.
 */
private fun computeTouchedAfterValidate(
    uiState: EditPlantUIState,
    touchedName: Boolean,
    touchedLatinName: Boolean,
    touchedDesc: Boolean,
    touchedLastWatered: Boolean,
    touchedLight: Boolean
): TouchedFlags {
  val newTouchedName = touchedName || uiState.name.isBlank()
  val newTouchedLatin = touchedLatinName || uiState.latinName.isBlank()
  val newTouchedDesc = touchedDesc || uiState.description.isBlank()
  val newTouchedLastWatered = touchedLastWatered || (uiState.lastWatered == null)
  val newTouchedLight = touchedLight || uiState.lightExposure.isBlank()

  return TouchedFlags(
      touchedName = newTouchedName,
      touchedLatinName = newTouchedLatin,
      touchedDesc = newTouchedDesc,
      touchedLastWatered = newTouchedLastWatered,
      touchedLight = newTouchedLight)
}

/**
 * Displays a Material 3 date picker dialog for selecting the plant's last watered date.
 *
 * The dialog is pre-initialized with the given timestamp, and returns the user’s selection when the
 * confirm button is pressed.
 *
 * @param initialMillis The initially selected date in epoch milliseconds, or null if none.
 * @param onConfirm Callback invoked with the selected date (in millis) when the user confirms.
 * @param onDismiss Callback invoked when the dialog is dismissed without selecting a date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPlantDatePickerDialog(
    initialMillis: Long?,
    onConfirm: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
  val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
  val context = LocalContext.current

  DatePickerDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(onClick = { onConfirm(pickerState.selectedDateMillis) }) {
          Text(context.getString(R.string.ok))
        }
      },
      dismissButton = {
        TextButton(onClick = onDismiss) { Text(context.getString(R.string.cancel)) }
      },
  ) {
    DatePicker(state = pickerState)
  }
}

// Plant image section
@Composable
private fun PlantImageSection(imageUrl: String?) {
  val context = LocalContext.current
  if (imageUrl != null) {
    AsyncImage(
        model =
            ImageRequest.Builder(context)
                .data(imageUrl)
                .error(R.drawable.error_image_download)
                .build(),
        contentDescription = context.getString(R.string.plant_image_description),
        modifier =
            Modifier.fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag(EditPlantScreenTestTags.PLANT_IMAGE),
        contentScale = ContentScale.Crop,
    )
  } else {
    // Placeholder if no image available
    Box(
        modifier =
            Modifier.fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag(EditPlantScreenTestTags.PLANT_IMAGE),
        contentAlignment = Alignment.Center,
    ) {
      Text(
          text = context.getString(R.string.plant_image_no_image_available),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

// Name field
@Composable
private fun NameFieldSection(
    name: String,
    isRecognized: Boolean,
    isNameError: Boolean,
    onTouchedName: () -> Unit,
    onNameChange: (String) -> Unit,
) {
  val context = LocalContext.current

  OutlinedTextField(
      value = name,
      onValueChange = { if (!isRecognized) onNameChange(it) },
      label = { Text(context.getString(R.string.name)) },
      singleLine = true,
      readOnly = isRecognized,
      enabled = !isRecognized,
      isError = isNameError,
      modifier =
          Modifier.fillMaxWidth().testTag(EditPlantScreenTestTags.PLANT_NAME).onFocusChanged {
            if (it.isFocused) onTouchedName()
          },
  )
  if (isNameError) {
    Text(
        text = context.getString(R.string.name_error),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE_NAME),
    )
  }
}

// Latin name field
@Composable
private fun LatinNameFieldSection(
    latinName: String,
    isRecognized: Boolean,
    isLatinNameError: Boolean,
    onTouchedLatinName: () -> Unit,
    onLatinNameChange: (String) -> Unit,
) {
  val context = LocalContext.current

  OutlinedTextField(
      value = latinName,
      onValueChange = { if (!isRecognized) onLatinNameChange(it) },
      label = { Text(context.getString(R.string.latin_name)) },
      singleLine = true,
      readOnly = isRecognized,
      enabled = !isRecognized,
      isError = isLatinNameError,
      modifier =
          Modifier.fillMaxWidth().testTag(EditPlantScreenTestTags.PLANT_LATIN).onFocusChanged {
            if (it.isFocused) onTouchedLatinName()
          },
  )
  if (isLatinNameError) {
    Text(
        text = context.getString(R.string.latin_name_error),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE_LATIN_NAME),
    )
  }
}

// Description field
@Composable
private fun DescriptionFieldSection(
    description: String,
    isDescriptionError: Boolean,
    onTouchedDesc: () -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
  val context = LocalContext.current

  OutlinedTextField(
      value = description,
      onValueChange = { onDescriptionChange(it) },
      label = { Text(context.getString(R.string.description)) },
      minLines = 3,
      isError = isDescriptionError,
      modifier =
          Modifier.fillMaxWidth()
              .heightIn(min = 100.dp)
              .testTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
              .onFocusChanged { if (it.isFocused) onTouchedDesc() },
  )
  if (isDescriptionError) {
    Text(
        text = context.getString(R.string.description_error),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE_DESCRIPTION),
    )
  }
}

// Location drop down menu
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDropDownSelection(
    value: String,
    isPlantRecognized: Boolean,
    onClickLocation: (PlantLocation) -> Unit
) {
  var isLocationExpanded by remember { mutableStateOf(false) }
  val context = LocalContext.current

  ExposedDropdownMenuBox(
      expanded = isLocationExpanded,
      onExpandedChange = { isLocationExpanded = it },
      modifier = Modifier,
      content = {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = !isPlantRecognized,
            label = { Text(context.getString(R.string.location)) },
            trailingIcon = {
              val iconIV =
                  if (isLocationExpanded && !isPlantRecognized) Icons.Default.ArrowDropUp
                  else Icons.Default.ArrowDropDown
              Icon(iconIV, contentDescription = null)
            },
            maxLines = 1,
            modifier =
                Modifier.fillMaxWidth()
                    .width(IntrinsicSize.Min)
                    .menuAnchor()
                    .testTag(EditPlantScreenTestTags.LOCATION_TEXTFIELD))

        DropdownMenu(
            expanded = isLocationExpanded,
            onDismissRequest = { isLocationExpanded = false },
            modifier = Modifier.testTag(EditPlantScreenTestTags.LOCATION_DROPDOWN)) {
              if (!isPlantRecognized) {
                PlantLocation.entries.forEach { location ->
                  DropdownMenuItem(
                      text = { Text(location.name) },
                      onClick = {
                        onClickLocation(location)
                        isLocationExpanded = false
                      },
                      modifier = Modifier.testTag(location.testTag))
                }
              }
            }
      })
}

// Light exposure field
@Composable
private fun LightExposureFieldSection(
    value: String,
    isLightError: Boolean,
    isPlantRecognized: Boolean,
    onTouchedLight: () -> Unit,
    onLightExposureChange: (String) -> Unit,
) {
  val context = LocalContext.current

  OutlinedTextField(
      value = value,
      onValueChange = { onLightExposureChange(it) },
      label = { Text(context.getString(R.string.light_exposure)) },
      singleLine = true,
      enabled = !isPlantRecognized,
      isError = isLightError,
      modifier =
          Modifier.fillMaxWidth().testTag(EditPlantScreenTestTags.LIGHT_EXPOSURE).onFocusChanged {
            if (it.isFocused) onTouchedLight()
          })
  if (isLightError) {
    Text(
        text = context.getString(R.string.light_exposure_error),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE_LIGHT_EXPOSURE))
  }
}

// Last watered field + date picker trigger
@Composable
private fun LastWateredSection(
    lastWatered: Timestamp?,
    isDateError: Boolean,
    dateFmt: DateTimeFormatter,
    onTouchedLastWatered: () -> Unit,
    onOpenDatePicker: () -> Unit,
) {
  val context = LocalContext.current

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
        context.getString(R.string.last_time_watered),
        style = MaterialTheme.typography.labelLarge,
    )

    val dateText =
        lastWatered?.let { ts ->
          Instant.ofEpochMilli(ts.time).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt)
        } ?: ""

    OutlinedTextField(
        value = dateText,
        onValueChange = {},
        readOnly = true,
        isError = isDateError,
        placeholder = { Text(context.getString(R.string.select_date)) },
        trailingIcon = {
          IconButton(
              onClick = { onOpenDatePicker() },
              modifier = Modifier.testTag(EditPlantScreenTestTags.DATE_PICKER_BUTTON),
          ) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = context.getString(R.string.pick_date_icon_description),
            )
          }
        },
        modifier =
            Modifier.fillMaxWidth()
                .testTag(EditPlantScreenTestTags.INPUT_LAST_WATERED)
                .onFocusChanged { if (it.isFocused) onTouchedLastWatered() },
    )
    if (isDateError) {
      Text(
          text = context.getString(R.string.last_time_watered_error),
          color = MaterialTheme.colorScheme.error,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE_DATE),
      )
    }
  }
}

// Save button with same enable logic
@Composable
private fun SaveButtonSection(
    isSaveEnabled: Boolean,
    onValidate: () -> Unit,
    onSave: () -> Unit,
) {
  val context = LocalContext.current
  Button(
      onClick = {
        onValidate()
        onSave()
      },
      enabled = isSaveEnabled,
      modifier = Modifier.fillMaxWidth().height(56.dp).testTag(EditPlantScreenTestTags.PLANT_SAVE),
  ) {
    Text(context.getString(R.string.save))
  }
}

// Delete button + popup.
@Composable
private fun DeleteSection(
    onDeleted: (() -> Unit)?,
    showDeletePopup: Boolean,
    onShowDeletePopupChange: (Boolean) -> Unit,
    onConfirmDelete: () -> Unit,
) {
  val context = LocalContext.current

  if (onDeleted != null) {
    TextButton(
        onClick = { onShowDeletePopupChange(true) },
        modifier = Modifier.fillMaxWidth().testTag(EditPlantScreenTestTags.PLANT_DELETE),
    ) {
      Icon(
          Icons.Filled.Delete,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
      )
      Spacer(Modifier.width(8.dp))
      Text(
          context.getString(R.string.delete),
          color = MaterialTheme.colorScheme.error,
      )
    }

    if (showDeletePopup) {
      DeletePlantPopup(
          onDelete = onConfirmDelete,
          onCancel = { onShowDeletePopupChange(false) },
      )
    }
  }
}
