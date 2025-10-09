package com.android.sample.ui.authentication

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.android.sample.R

/** Semantic for testing */
val LogoResNameKey = SemanticsPropertyKey<String>("LogoResName")
var SemanticsPropertyReceiver.logoRes by LogoResNameKey

/**
 * Composable function that displays the Sign-In screen for the application.
 *
 * This screen shows the app logo and provides a button to sign in with Google. The logo adapts
 * automatically to the system theme (dark or light mode).
 *
 * @param uiState Current UI state of the authentication flow. Used to display error messages.
 * @param credentialManager Optional [CredentialManager] instance for managing credentials.
 * @param onSignInClick Lambda triggered when the "Sign in with Google" button is clicked.
 * @param isDarkTheme Boolean flag indicating whether the system is in dark theme.
 */
@Preview
@Composable
fun SignInScreen(
    uiState: AuthUIState = AuthUIState(),
    credentialManager: androidx.credentials.CredentialManager =
        CredentialManager.create(LocalContext.current),
    onSignInClick: () -> Unit = {},
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
  val logoRes =
      if (isDarkTheme) {
        R.drawable.app_logo_dark
      } else {
        R.drawable.app_logo_light
      }
  // For testing
  val resName = LocalContext.current.resources.getResourceEntryName(logoRes)
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_BACKGROUND)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Image(
              painter = painterResource(id = logoRes),
              contentDescription = "My Garden logo",
              contentScale = ContentScale.Crop,
              modifier =
                  Modifier.size(350.dp)
                      .clip(RoundedCornerShape(16.dp))
                      .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO)
                      .semantics { this.logoRes = resName })

          Spacer(modifier = Modifier.height(150.dp))

          // To display an error message in case of error
          if (uiState.errorMsg != null) {
            Text(
                text = uiState.errorMsg,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp).testTag("error_message"))
          }
          OutlinedButton(
              onClick = { onSignInClick() },
              modifier =
                  Modifier.height(60.dp)
                      .fillMaxWidth(0.75f)
                      .border(BorderStroke(2.dp, Color.Gray), RoundedCornerShape(50))
                      .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON),
              shape = RoundedCornerShape(50),
              colors =
                  ButtonDefaults.outlinedButtonColors(
                      containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Icon(
                    painter = painterResource(R.drawable.google_logo),
                    contentDescription = "google Icon",
                    tint = Color.Unspecified, // To keep the reel colors of the logo
                    modifier = Modifier.size(20.dp))

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Sign in with Google",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
        }
      }
}
