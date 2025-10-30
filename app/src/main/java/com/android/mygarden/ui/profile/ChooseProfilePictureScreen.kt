package com.android.mygarden.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// Grid proportions and settings
private const val GRID_NUMBER_OF_AVATAR_PER_ROW = 2
private val GRID_PADDING = 16.dp
private val GRID_VERTICAL_ARRANGEMENT = 16.dp
private val GRID_HORIZONTAL_ARRANGEMENT = 16.dp

/** Test tags for [ChooseProfilePictureScreen]. */
object ChooseProfilePictureScreenTestTags {
  const val SCREEN = "chooseProfilePictureScreen"
  const val TOP_APP_BAR = "topAppBar"
  const val BACK_BUTTON = "backButton"
  const val AVATAR_GRID = "avatarGrid"
  const val AVATAR_CARD_PREFIX = "avatarCard_" // for example avatarCard_A1

  /**
   * Helper function to get the test tag for an avatar card.
   *
   * @param avatar The [Avatar] instance for which to generate the test tag
   * @return The test tag string
   */
  fun getTestTagAvatar(avatar: Avatar): String {
    return "$AVATAR_CARD_PREFIX${avatar.name}"
  }
}

/**
 * Screen for choosing a profile picture from a default choice of avatars.
 *
 * @param onAvatarChosen Callback when an avatar is selected
 * @param onBack Callback when the back button is pressed
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChooseProfilePictureScreen(
    modifier: Modifier = Modifier,
    onAvatarChosen: (Avatar) -> Unit,
    onBack: () -> Unit
) {
  Scaffold(
      modifier = modifier.testTag(ChooseProfilePictureScreenTestTags.SCREEN),
      topBar = {
        TopAppBar(
            modifier = modifier.testTag(ChooseProfilePictureScreenTestTags.TOP_APP_BAR),
            title = { Text("Choose an avatar") },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = modifier.testTag(ChooseProfilePictureScreenTestTags.BACK_BUTTON)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back arrow")
                  }
            })
      }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_NUMBER_OF_AVATAR_PER_ROW),
            modifier =
                modifier
                    .padding(padding)
                    .padding(GRID_PADDING)
                    .testTag(ChooseProfilePictureScreenTestTags.AVATAR_GRID),
            verticalArrangement = Arrangement.spacedBy(GRID_VERTICAL_ARRANGEMENT),
            horizontalArrangement = Arrangement.spacedBy(GRID_HORIZONTAL_ARRANGEMENT)) {
              items(Avatar.values()) { avatar ->
                Card(
                    modifier =
                        modifier
                            .clip(CircleShape)
                            .clickable { onAvatarChosen(avatar) }
                            .testTag(ChooseProfilePictureScreenTestTags.getTestTagAvatar(avatar))) {
                      Image(
                          painter = painterResource(avatar.resId),
                          contentDescription = "Avatar ${avatar.name}",
                          modifier = modifier.fillMaxSize())
                    }
              }
            }
      }
}
