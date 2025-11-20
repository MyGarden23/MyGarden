package com.android.mygarden.ui.camera

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.io.File

/**
 * A function used to display an image. At the moment it is only used for tests but it can be used
 * in later implementation of the app. It provides two different versions to display an image
 *
 * @param imagePath the path to the image
 * @param modifier to modify the displayed image (not used at the moment)
 * @param testVersionRemeberAsync to test two different versions of displaying an image
 */
@Composable
fun LocalImageDisplay(
    imagePath: String,
    modifier: Modifier = Modifier,
    contentDescription: String = "default",
    testVersionRemeberAsync: Boolean = false
) {
  if (testVersionRemeberAsync) {
    val painter = rememberAsyncImagePainter(File(imagePath))
    Box(modifier = Modifier.fillMaxSize()) {
      Image(
          painter = painter,
          contentDescription = contentDescription,
          contentScale = ContentScale.Crop,
          modifier = modifier)
    }
  } else {
    Box(modifier = Modifier.fillMaxSize()) {
      AsyncImage(
          model = ImageRequest.Builder(LocalContext.current).data(imagePath).build(),
          contentDescription = contentDescription,
          modifier =
              Modifier.fillMaxWidth(0.6f)
                  .height(220.dp)
                  .clip(MaterialTheme.shapes.medium)
                  .background(MaterialTheme.colorScheme.surfaceVariant),
          contentScale = ContentScale.Crop)
    }
  }
}
