package com.android.mygarden.ui.profile

import com.android.mygarden.model.profile.GardeningSkill

/** Test tags for ProfileScreen components to enable UI testing */
object ProfileScreenTestTags {
  const val SCREEN = "new_profile_screen"
  const val AVATAR = "profile_avatar"
  const val FIRST_NAME_FIELD = "first_name_field"
  const val LAST_NAME_FIELD = "last_name_field"
  const val EXPERIENCE_FIELD = "experience_field"
  const val EXPERIENCE_DROPDOWN = "experience_dropdown"
  const val FAVORITE_PLANT_FIELD = "favorite_plant_field"
  const val COUNTRY_FIELD = "country_field"
  const val COUNTRY_DROPDOWN = "country_dropdown"
  const val COUNTRY_DROPDOWN_ICON = "country_dropdown_icon"
  const val SAVE_BUTTON = "save_button"

  // Dropdown menu items
  const val EXPERIENCE_DROPDOWN_MENU = "experience_dropdown_menu"
  const val COUNTRY_DROPDOWN_MENU = "country_dropdown_menu"
  const val COUNTRY_RESULTS_COUNT = "country_results_count"
  const val COUNTRY_MORE_RESULTS = "country_more_results"
  const val COUNTRY_NO_RESULTS = "country_no_results"

  // Dynamic test tag generators
  fun getExperienceItemTag(skill: GardeningSkill): String =
      "experience_item_${skill.name.lowercase()}"

  fun getCountryItemTag(country: String): String =
      "country_item_${country.replace(" ", "_").lowercase()}"
}
