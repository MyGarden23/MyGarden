package com.android.mygarden.ui.editPlant

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.ui.theme.MyGardenTheme
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Test tags to mirror your existing pattern.
 */
object EditPlantScreenTestTags {
    const val PLANT_IMAGE = "plantImage"
    const val PLANT_NAME = "plantName"
    const val PLANT_LATIN = "plantLatin"
    const val INPUT_PLANT_DESCRIPTION = "inputPlantDescription"
    const val INPUT_LAST_WATERED = "inputLastWatered"
    const val ERROR_MESSAGE = "errorMessage"
    const val PLANT_SAVE = "plantSave"
    const val PLANT_DELETE = "plantDelete"
}

/**
 * Route-level Composable, mirroring EditToDoScreen’s structure.
 * Assumes EditPlantViewModel exposes: uiState, loadPlant(), setters, editPlant(), deletePlant(), clearErrorMsg()
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlantScreen(
    ownedPlantId: String,
    editPlantViewModel: EditPlantViewModel = viewModel(),
    onSaved: () -> Unit = {},
    onDeleted: () -> Unit = {},
    goBack: () -> Unit = {}, // ← back to camera
) {
    // Load the plant when the id changes
    LaunchedEffect(ownedPlantId) { editPlantViewModel.loadPlant(ownedPlantId) }

    val plantUIState by editPlantViewModel.uiState.collectAsState()

    val context = LocalContext.current
    LaunchedEffect(plantUIState.errorMsg) {
        plantUIState.errorMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            editPlantViewModel.clearErrorMsg()
        }
    }

    var touchedDesc by remember { mutableStateOf(false) }
    var touchedLastWatered by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    val isDescriptionError = plantUIState.description.isBlank() && touchedDesc

    if (showDatePicker) {
        val initialMillis = plantUIState.lastWatered?.time
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        editPlantViewModel.setLastWatered(Timestamp(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit plant") },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Plant image
            if (plantUIState.image != null) {
                Image(
                    bitmap = plantUIState.image!!,
                    contentDescription = "Plant image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag(EditPlantScreenTestTags.PLANT_IMAGE),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder if no image available
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag(EditPlantScreenTestTags.PLANT_IMAGE),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No image available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Name (read-only)
            OutlinedTextField(
                value = plantUIState.name,
                onValueChange = {},
                label = { Text("Name") },
                singleLine = true,
                readOnly = true,
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(EditPlantScreenTestTags.PLANT_NAME)
            )

            // Latin name (read-only)
            OutlinedTextField(
                value = plantUIState.latinName,
                onValueChange = {},
                label = { Text("Latin name") },
                singleLine = true,
                readOnly = true,
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(EditPlantScreenTestTags.PLANT_LATIN)
            )

            // Description
            OutlinedTextField(
                value = plantUIState.description,
                onValueChange = {editPlantViewModel.setDescription(it) },
                label = { Text("Description") },
                minLines = 3,
                isError = isDescriptionError,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .testTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
                    .onFocusChanged { if (it.isFocused) touchedDesc = true }
            )
            if (plantUIState.description.isBlank() && touchedDesc) {
                Text(
                    text = "Description cannot be empty",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE)
                )
            }

            // Last time watered
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Last time watered", style = MaterialTheme.typography.labelLarge)

                val isDateError = plantUIState.lastWatered == null && touchedLastWatered
                OutlinedTextField(
                    value = plantUIState.lastWatered?.let { ts ->
                        Instant.ofEpochMilli(ts.time).atZone(ZoneId.systemDefault()).toLocalDate()
                            .format(dateFmt)
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    isError = isDateError,
                    placeholder = { Text("Select a date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            touchedLastWatered = true
                            showDatePicker = true
                        }) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick date")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(EditPlantScreenTestTags.INPUT_LAST_WATERED)
                        .onFocusChanged { if (it.isFocused) touchedLastWatered = true }
                )
                if (isDateError) {
                    Text(
                        text = "Last time watered is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.testTag(EditPlantScreenTestTags.ERROR_MESSAGE)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Save (enabled only if lastWatered present and basic fields not empty)
            val isSaveEnabled = plantUIState.description.isNotBlank()
                    && plantUIState.lastWatered != null

            Button(
                onClick = {
                    // ensure user sees the error if they never touched the field
                    if (plantUIState.lastWatered == null) touchedLastWatered = true
                    if (isSaveEnabled) {
                        editPlantViewModel.editPlant(ownedPlantId)
                        onSaved()
                    }
                },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag(EditPlantScreenTestTags.PLANT_SAVE)
            ) {
                Text("Save")
            }

            // Delete (accented with error color icon/text)
            TextButton(
                onClick = {
                    editPlantViewModel.deletePlant(ownedPlantId)
                    onDeleted()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(EditPlantScreenTestTags.PLANT_DELETE)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}


@Preview
@Composable
fun Sr(){
    MyGardenTheme { EditPlantScreen("d") }
}