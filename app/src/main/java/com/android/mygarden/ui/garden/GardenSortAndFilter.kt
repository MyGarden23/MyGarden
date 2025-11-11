package com.android.mygarden.ui.garden

/**
 * Represents the available sorting options for the garden plant list.
 *
 * These options determine the order in which plants are displayed in the garden screen.
 */
enum class SortOption {
  // Sort by plant common name alphabetically (default)
  PLANT_NAME,

  // Sort by plant latin/scientific name alphabetically
  LATIN_NAME,

  // Sort by last watered date, oldest first (ascending)
  LAST_WATERED_ASC,

  // Sort by last watered date, most recent first (descending)
  LAST_WATERED_DESC
}

/**
 * Represents the available filtering options for the garden plant list.
 *
 * These options determine which plants are visible based on their health status.
 */
enum class FilterOption {
  // Show all plants regardless of health status (default, no filter)
  ALL,

  // Show only overwatered and severely overwatered plants
  OVERWATERED_ONLY,

  // Show only dry plants that need water (needs water, slightly dry or severely dry)
  DRY_PLANTS,

  // Show only critically unhealthy plants (severely dry or severely overwatered)
  CRITICAL_ONLY,

  // Show only healthy plants
  HEALTHY_ONLY
}
