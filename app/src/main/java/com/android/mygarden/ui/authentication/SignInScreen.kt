package com.android.mygarden.ui.authentication

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R

/** Semantic for testing */
val LogoResNameKey = SemanticsPropertyKey<String>("LogoResName")
var SemanticsPropertyReceiver.logoRes by LogoResNameKey

/**
 * Composable function that displays the Sign-In screen for the application.
 *
 * This screen shows the app logo and provides a button to sign in with Google. The logo adapts
 * automatically to the system theme (dark or light mode).
 *
 * @param authViewModel ViewModel used to handle the logic
 * @param credentialManager Optional [CredentialManager] instance for managing credentials.
 * @param onSignedIn Lambda triggered when the user is successfully signed in
 * @param onLogIn Lambda triggered when the user is already signed in and wants to log in again
 * @param isDarkTheme Boolean flag indicating whether the system is in dark theme.
 */
@Composable
fun SignInScreen(
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignedIn: () -> Unit = {},
    onLogIn: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {

  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsState()

  // Show error message if login fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      authViewModel.clearErrorMsg()
    }
  }
  // Navigate to the next screen on successful login
  LaunchedEffect(uiState.user) {
    uiState.user?.let {
      Toast.makeText(context, context.getString(R.string.login_successful), Toast.LENGTH_SHORT)
          .show()
      if (uiState.isNewUser) onSignedIn() else onLogIn()
    }
  }
  val logoRes =
      if (isDarkTheme) {
        R.drawable.app_logo_dark
      } else {
        R.drawable.app_logo_light
      }
  // For testing
  val resName = context.resources.getResourceEntryName(logoRes)
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_BACKGROUND)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Image(
              painter = painterResource(id = logoRes),
              contentDescription = context.getString(R.string.description_logo),
              contentScale = ContentScale.Fit,
              modifier =
                  Modifier.fillMaxWidth(0.6f)
                      .aspectRatio(1f)
                      .heightIn(max = 180.dp)
                      .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO)
                      .semantics { this.logoRes = resName })

          Spacer(modifier = Modifier.fillMaxHeight(0.4f))

          // Authenticate With Google Button
          if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
          } else {
            OutlinedButton(
                onClick = { authViewModel.signIn(context, credentialManager) },
                modifier =
                    Modifier.height(60.dp)
                        .fillMaxWidth(0.75f)
                        .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON),
                shape = RoundedCornerShape(50),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                  Icon(
                      painter = painterResource(R.drawable.google_logo),
                      contentDescription = context.getString(R.string.description_google_icon),
                      tint = Color.Unspecified, // To keep the reel colors of the logo
                      modifier = Modifier.size(20.dp))

                  Spacer(modifier = Modifier.fillMaxWidth(0.03f))

                  Text(
                      text = context.getString(R.string.sign_in_with_google),
                      color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
          }
        }
      }
}
