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

/** Test tags for [ChooseProfilePictureScreen]. */
object ChooseProfilePictureScreenTestTags {
  const val SCREEN = "chooseProfilePictureScreen"
  const val TOP_APP_BAR = "topAppBar"
  const val BACK_BUTTON = "backButton"
  const val AVATAR_GRID = "avatarGrid"
  const val AVATAR_CARD_PREFIX = "avatarCard_" // for example avatarCard_A1
}

/**
 * Screen for choosing a profile picture from a default choice of avatars.
 *
 * @param onAvatarChosen Callback when an avatar is selected
 * @param onBack Callback when the back button is pressed
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChooseProfilePictureScreen(onAvatarChosen: (Avatar) -> Unit, onBack: () -> Unit) {
  Scaffold(
      modifier = Modifier.testTag(ChooseProfilePictureScreenTestTags.SCREEN),
      topBar = {
        TopAppBar(
            modifier = Modifier.testTag(ChooseProfilePictureScreenTestTags.TOP_APP_BAR),
            title = { Text("Choose an avatar") },
            navigationIcon = {
              IconButton(
                  onClick = onBack,
                  modifier = Modifier.testTag(ChooseProfilePictureScreenTestTags.BACK_BUTTON)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back arrow")
                  }
            })
      }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier.padding(padding)
                    .padding(16.dp)
                    .testTag(ChooseProfilePictureScreenTestTags.AVATAR_GRID),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
              items(Avatar.values()) { avatar ->
                Card(
                    modifier =
                        Modifier.clip(CircleShape)
                            .clickable { onAvatarChosen(avatar) }
                            .testTag(
                                "${ChooseProfilePictureScreenTestTags.AVATAR_CARD_PREFIX}${avatar.name}")) {
                      Image(
                          painter = painterResource(avatar.resId),
                          contentDescription = "Avatar ${avatar.name}",
                          modifier = Modifier.fillMaxSize())
                    }
              }
            }
      }
}
