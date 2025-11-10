package com.android.mygarden.ui.navigation

import android.content.Context
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
            NavBackIcon(onClick = onGoBack, context = context)
          }
          if (hasSignOutButton) {
            SignOutButton(onClick = onSignOut, context = context)
          }
        }
      })
}

/**
 * Potential sign out button
 *
 * @param context Context to be used to retrieve the icon content description from the xml files
 * @param onClick the callback that clicking on the button will trigger
 */
@Composable
fun SignOutButton(context: Context, onClick: () -> Unit = {}) {
  IconButton(
      onClick = onClick, modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Logout,
            contentDescription = context.getString(R.string.sign_out_button_description))
      }
}

/**
 * Potential back button
 *
 * @param context Context to be used to retrieve the icon content description from the xml files
 * @param onClick the callback that clicking on the button will trigger
 */
@Composable
fun NavBackIcon(context: Context, onClick: () -> Unit = {}) {
  IconButton(
      onClick = onClick, modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON)) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = context.getString(R.string.back_description))
      }
}
