package com.android.sample.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.android.sample.R

private val cameraIconId = R.drawable.photo_camera_icon
private val profileIconId = R.drawable.plant_profile_icon

/**
 * Representing a button of the bottom bar
 *
 * @param name the name of the page
 * @param destination the screen it refers to
 * @param iconId the id of the icon
 * @param testTag the test tag of the page
 */
sealed class Page(val name: String, val destination: Screen, val iconId: Int, val testTag: String) {
  object Camera : Page("CameraPage", Screen.Camera, cameraIconId, NavigationTestTags.CAMERA_BUTTON)

  object Profile :
      Page("ProfilePage", Screen.Profile, profileIconId, NavigationTestTags.PROFILE_BUTTON)
}

/**
 * Composable of the bottom bar of the app making it possible to switch between the
 * main screens of the app
 */
@Composable
fun BottomBar(selectedPage: Page, onSelect: (Page) -> Unit, modifier: Modifier = Modifier) {
  val pages = listOf(Page.Camera, Page.Profile)
  NavigationBar(
      modifier = modifier.fillMaxWidth().height(100.dp).testTag(NavigationTestTags.BOTTOM_BAR),
      containerColor = MaterialTheme.colorScheme.background,
      content = {
        pages.forEach { page ->
          NavigationBarItem(
              modifier = Modifier.testTag(page.testTag),
              selected = (page == selectedPage),
              onClick = { onSelect(page) },
              icon = {
                Icon(
                    painter = painterResource(page.iconId),
                    contentDescription = page.name,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
              })
        }
      })
}
