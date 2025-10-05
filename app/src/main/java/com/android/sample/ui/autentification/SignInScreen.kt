package com.android.sample.ui.autentification

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.sample.R

@Preview
@Composable
fun SignInScreen(
    onSignInClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.signinscreen_background_image),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop
        )
        //TODO : Check if we keep that 10% opacity
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color.Black.copy(alpha = 0.1f)) //10% opacity
        )

        Column (
            modifier = Modifier.fillMaxSize()
                .padding(top = 160.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ){
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "My Garden logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        BorderStroke(2.dp, Color.Gray),
                        RoundedCornerShape(16.dp)
                    )
                    .shadow(8.dp, RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(200.dp))

            OutlinedButton(
                onClick = { onSignInClick() },
                modifier = Modifier.height(60.dp)
                    .fillMaxWidth(0.75f)
                    .border(BorderStroke(2.dp, Color.Gray), RoundedCornerShape(50)),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Icon(
                    painter = painterResource(R.drawable.google_logo),
                    contentDescription = "google Icon",
                    tint = Color.Unspecified, //To keep the reel colors of the logo
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Sign in with Google",
                    color = Color.Black,
                )
            }

        }
    }
}