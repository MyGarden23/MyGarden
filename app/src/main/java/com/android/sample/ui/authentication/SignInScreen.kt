package com.android.sample.ui.authentication

import android.view.Surface
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.R
import com.android.sample.ui.theme.SampleAppTheme

@Composable
fun SignInScreen(onSignInClick: () -> Unit = {}) {
  val isDarkTheme = isSystemInDarkTheme()

  var buttonColor = Color(0xffe0e0e0)
  var textColor = Color(0xff424242)
  var logoRes = R.drawable.app_logo_light

  if (false) {
    logoRes = R.drawable.app_logo_dark
    buttonColor = Color(0xFF424242)
    textColor = Color(0xFFBDBDBD)
  }
  Box(
      modifier =
          Modifier.fillMaxSize()
              .background(MaterialTheme.colorScheme.background)
              .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_BACKGROUND)) {
        //        Image(
        //            painter = painterResource(id = R.drawable.signinscreen_background_image),
        //            contentDescription = null,
        //            modifier = Modifier.matchParentSize(),
        //            contentScale = ContentScale.Crop
        //        )

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
                      // .border(BorderStroke(2.dp, Color.Gray), RoundedCornerShape(16.dp))
                      // .shadow(8.dp, RoundedCornerShape(16.dp))
                      .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_APP_LOGO))

          Spacer(modifier = Modifier.height(150.dp))

          OutlinedButton(
              onClick = { onSignInClick() },
              modifier =
                  Modifier.height(60.dp)
                      .fillMaxWidth(0.75f)
                      .border(BorderStroke(2.dp, Color.Gray), RoundedCornerShape(50))
                      .testTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON),
              shape = RoundedCornerShape(50),
              colors = ButtonDefaults.outlinedButtonColors(containerColor = buttonColor)) {
                Icon(
                    painter = painterResource(R.drawable.google_logo),
                    contentDescription = "google Icon",
                    tint = Color.Unspecified, // To keep the reel colors of the logo
                    modifier = Modifier.size(20.dp))

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Sign in with Google",
                    color = textColor,
                )
              }
        }
      }
}

@Preview
@Composable
fun SignInScreenPreview() {
  SampleAppTheme { Surface(modifier = Modifier.fillMaxSize()) { SignInScreen() } }
}
