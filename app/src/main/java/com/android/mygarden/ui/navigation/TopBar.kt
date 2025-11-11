package com.android.mygarden.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.android.mygarden.R

/**
 * Common reusable composable that represents a top bar of a screen with
 *
 * @param title the title of the screen to be displayed in the center
 * @param hasGoBackButton whether or not the screen should have a back button
 * @param hasSignOutButton whether or not the screen should have a sign out button
 * @param onGoBack the callback that clicking on the (potential) back button will trigger
 * @param onSignOut the callback that clicking on the (potential) sign out button will trigger
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    modifier: Modifier = Modifier,
    title: String,
    hasGoBackButton: Boolean = false,
    hasSignOutButton: Boolean = false,
    onGoBack: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {

  val context = LocalContext.current

  CenterAlignedTopAppBar(
      title = {
        Text(
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE),
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            text = title)
      },
      colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      modifier = modifier.testTag(NavigationTestTags.TOP_BAR),
      navigationIcon = {
        Row {
          if (hasGoBackButton) {
            NavigationButton(onClick = onGoBack)
          }
          if (hasSignOutButton) {
            NavigationButton(onClick = onSignOut, isSignOut = true)
          }
        }
      })
}

/**
 * Navigation button of the top bar that can be either a back button or a sign out button. Defaults
 * to back button
 *
 * @param onClick the callback that will be triggered when clicking on the button
 * @param isSignOut boolean that chooses whether the button is a back button or a sign out button.
 *   Defaults to false
 */
@Composable
fun NavigationButton(onClick: () -> Unit = {}, isSignOut: Boolean = false) {
  val testTag =
      if (isSignOut) NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON
      else NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON
  val imageVector =
      if (isSignOut) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.ArrowBack
  val resId = if (isSignOut) R.string.sign_out_button_description else R.string.back_description
  IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
    Icon(imageVector = imageVector, contentDescription = LocalContext.current.getString(resId))
  }
}
