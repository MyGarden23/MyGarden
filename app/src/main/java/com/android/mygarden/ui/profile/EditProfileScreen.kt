package com.android.mygarden.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Screen for editing an existing user profile.
 *
 * This screen reuses [NewProfileScreen] with a back navigation button and "Edit Profile" title. All
 * form fields and validation logic remain the same.
 *
 * @param profileViewModel ViewModel managing form state and validation (should be pre-populated
 *   with existing profile data)
 * @param onBackPressed Callback invoked when the back button is clicked
 * @param onSavePressed Callback invoked on successful profile update
 * @param onAvatarClick Callback when avatar is clicked to change profile picture
 */
@Composable
fun EditProfileScreen(
    profileViewModel: ProfileViewModel = viewModel(),
    onBackPressed: (() -> Unit)? = {},
    onSavePressed: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
) {
  LaunchedEffect(Unit) { profileViewModel.initialize() }
  ProfileScreenBase(
      profileViewModel = profileViewModel,
      onSavePressed = onSavePressed,
      onAvatarClick = onAvatarClick,
      onNavBackIconClick = onBackPressed,
      title = "Edit Profile")
}
