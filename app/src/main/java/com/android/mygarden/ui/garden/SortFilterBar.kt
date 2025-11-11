package com.android.mygarden.ui.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.mygarden.R

/** Test tags for the SortFilterBar */
object SortFilterBarTestTags {
  const val SORT_FILTER_BAR = "SortFilterBar"
  const val SORT_DROPDOWN = "SortDropdown"
  const val FILTER_DROPDOWN = "FilterDropdown"

  // Sort option test tags
  const val SORT_PLANT_NAME = "SortOption_PlantName"
  const val SORT_LATIN_NAME = "SortOption_LatinName"
  const val SORT_LAST_WATERED_ASC = "SortOption_OldestWatered"
  const val SORT_LAST_WATERED_DESC = "SortOption_RecentWatered"

  // Filter option test tags
  const val FILTER_ALL = "FilterOption_All"
  const val FILTER_OVERWATERED = "FilterOption_Overwatered"
  const val FILTER_DRY = "FilterOption_Dry"
  const val FILTER_CRITICAL = "FilterOption_Critical"
  const val FILTER_HEALTHY = "FilterOption_Healthy"
}

/**
 * A horizontal bar containing sorting and filtering controls for the garden plant list.
 *
 * @param currentSort The currently selected sorting option
 * @param currentFilter The currently selected filtering option
 * @param onSortChange Callback invoked when the user selects a new sorting option
 * @param onFilterChange Callback invoked when the user selects a new filtering option
 * @param modifier Optional modifier for the composable
 */
@Composable
fun SortFilterBar(
    currentSort: SortOption,
    currentFilter: FilterOption,
    onSortChange: (SortOption) -> Unit,
    onFilterChange: (FilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
  // Surface container with elevation for visual separation
  Surface(
      modifier =
          modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp)
              .testTag(SortFilterBarTestTags.SORT_FILTER_BAR),
      tonalElevation = 2.dp,
      shape = MaterialTheme.shapes.medium) {
        // Horizontal layout
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically) {
              // Sort dropdown on the left
              SortDropdown(
                  currentSort = currentSort,
                  onSortChange = onSortChange,
                  modifier = Modifier.weight(1f))

              // Filter dropdown on the right
              FilterDropdown(
                  currentFilter = currentFilter,
                  onFilterChange = onFilterChange,
                  modifier = Modifier.weight(1f))
            }
      }
}

/**
 * Dropdown menu for selecting sorting options.
 *
 * @param currentSort The currently selected sorting option
 * @param onSortChange Callback invoked when the user selects a new sorting option
 * @param modifier Optional modifier for the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDropdown(
    currentSort: SortOption,
    onSortChange: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
  // Track whether the dropdown is open or closed
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = it },
      modifier = modifier.testTag(SortFilterBarTestTags.SORT_DROPDOWN)) {
        // Display the currently selected sort option
        DropdownTextField(
            value = getSortLabel(currentSort),
            label = stringResource(R.string.sort_label),
            icon = Icons.AutoMirrored.Filled.Sort,
            iconDescription = stringResource(R.string.sort_icon_description),
            expanded = expanded,
            modifier = Modifier)

        // Dropdown menu with all sort options
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          SortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(getSortLabel(option)) },
                onClick = {
                  onSortChange(option) // Notify parent of selection
                  expanded = false
                },
                modifier =
                    Modifier.testTag(
                        when (option) {
                          SortOption.PLANT_NAME -> SortFilterBarTestTags.SORT_PLANT_NAME
                          SortOption.LATIN_NAME -> SortFilterBarTestTags.SORT_LATIN_NAME
                          SortOption.LAST_WATERED_ASC -> SortFilterBarTestTags.SORT_LAST_WATERED_ASC
                          SortOption.LAST_WATERED_DESC ->
                              SortFilterBarTestTags.SORT_LAST_WATERED_DESC
                        }))
          }
        }
      }
}

/**
 * Dropdown menu for selecting filtering options.
 *
 * @param currentFilter The currently selected filtering option
 * @param onFilterChange Callback invoked when the user selects a new filtering option
 * @param modifier Optional modifier for the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    currentFilter: FilterOption,
    onFilterChange: (FilterOption) -> Unit,
    modifier: Modifier = Modifier
) {
  // Track whether the dropdown is open or closed
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = it },
      modifier = modifier.testTag(SortFilterBarTestTags.FILTER_DROPDOWN)) {
        // Display the currently selected filter option
        DropdownTextField(
            value = getFilterLabel(currentFilter),
            label = stringResource(R.string.filter_label),
            icon = Icons.Default.FilterList,
            iconDescription = stringResource(R.string.filter_icon_description),
            expanded = expanded,
            modifier = Modifier)

        // Dropdown menu with all filter options
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          FilterOption.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(getFilterLabel(option)) },
                onClick = {
                  onFilterChange(option) // Notify parent of selection
                  expanded = false
                },
                modifier =
                    Modifier.testTag(
                        when (option) {
                          FilterOption.ALL -> SortFilterBarTestTags.FILTER_ALL
                          FilterOption.OVERWATERED_ONLY -> SortFilterBarTestTags.FILTER_OVERWATERED
                          FilterOption.DRY_PLANTS -> SortFilterBarTestTags.FILTER_DRY
                          FilterOption.CRITICAL_ONLY -> SortFilterBarTestTags.FILTER_CRITICAL
                          FilterOption.HEALTHY_ONLY -> SortFilterBarTestTags.FILTER_HEALTHY
                        }))
          }
        }
      }
}

/**
 * Returns a human-readable label for a sorting option.
 *
 * Maps each sort option enum to its corresponding localized string resource.
 *
 * @param option The sorting option to get a label for
 * @return A user-friendly string describing the sorting option
 */
@Composable
fun getSortLabel(option: SortOption): String {
  return when (option) {
    SortOption.PLANT_NAME -> stringResource(R.string.sort_plant_name)
    SortOption.LATIN_NAME -> stringResource(R.string.sort_latin_name)
    SortOption.LAST_WATERED_ASC -> stringResource(R.string.sort_last_watered_asc)
    SortOption.LAST_WATERED_DESC -> stringResource(R.string.sort_last_watered_desc)
  }
}

/**
 * Returns a human-readable label for a filtering option.
 *
 * Maps each filter option enum to its corresponding localized string resource.
 *
 * @param option The filtering option to get a label for
 * @return A user-friendly string describing the filtering option
 */
@Composable
fun getFilterLabel(option: FilterOption): String {
  return when (option) {
    FilterOption.ALL -> stringResource(R.string.filter_all)
    FilterOption.OVERWATERED_ONLY -> stringResource(R.string.filter_overwatered_only)
    FilterOption.DRY_PLANTS -> stringResource(R.string.filter_dry_plants)
    FilterOption.CRITICAL_ONLY -> stringResource(R.string.filter_critical_only)
    FilterOption.HEALTHY_ONLY -> stringResource(R.string.filter_healthy_only)
  }
}

/**
 * Reusable dropdown text field component for ExposedDropdownMenuBox.
 *
 * This composable provides a consistent styled OutlinedTextField for dropdown menus, reducing code
 * duplication between sort and filter dropdowns.
 *
 * @param value The current selected value to display
 * @param label The label text for the text field
 * @param icon The leading icon to display
 * @param iconDescription Content description for the icon (for accessibility)
 * @param expanded Whether the dropdown menu is currently expanded
 * @param modifier Optional modifier for the composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.DropdownTextField(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconDescription: String,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
  OutlinedTextField(
      value = value,
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      maxLines = 1,
      label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
      leadingIcon = { Icon(icon, contentDescription = iconDescription) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      modifier = modifier.menuAnchor().fillMaxWidth(),
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
      textStyle = MaterialTheme.typography.bodySmall)
}
