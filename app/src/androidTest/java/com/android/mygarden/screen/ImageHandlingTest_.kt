package com.android.mygarden.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.R
import com.android.mygarden.ui.camera.LocalImageDisplay
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageHandlingTest_ {

  @get:Rule val composeTestRule = createComposeRule()

  /**
   * Test to check if a bitmap (not a real image for the moment) can be compressed and stored in
   * context.filesDir and then be retrieved with the same dimensions
   */
  @Test
  fun saveAndReadImage_inAppFilesDir() {
    // Give the Android context
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Create a bitmap for the test
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    // Compress and save this image in context.filesDir
    val file = File(context.filesDir, "test_image.jpeg")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }

    assertTrue("Le fichier doit exister après écriture", file.exists())
    val loadedBitmap = BitmapFactory.decodeFile(file.absolutePath)

    // Check that the bitmap exists and has the right dimensions
    assertNotNull("The image should not be null", loadedBitmap)
    assertEquals(100, loadedBitmap.width)
    assertEquals(100, loadedBitmap.height)
  }

  @Test
  fun loadLocalImage_displaysImageSuccessfully_version_1() {
    // Give the Android context
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Take the image app_logo for the test
    val inputStream = context.resources.openRawResource(R.drawable.app_logo)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    // Compress and save this image in context.filesDir
    val file = File(context.filesDir, "test_app_logo.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

    // Display the image with the AsyncImage version
    composeTestRule.setContent {
      LocalImageDisplay(
          imagePath = file.absolutePath,
          testVersionRemeberAsync = false,
          contentDescription = "Plant image")
    }

    // Check that the image is displayed
    composeTestRule.onNodeWithContentDescription("Plant image").assertExists()
  }

  @Test
  fun loadLocalImage_displaysImageSuccessfully_version_2() {
    // Give the Android context
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Take the image app_logo for the test
    val inputStream = context.resources.openRawResource(R.drawable.app_logo)
    val bitmap = BitmapFactory.decodeStream(inputStream)
    inputStream.close()

    // Compress and save this image in context.filesDir
    val file = File(context.filesDir, "test_app_logo.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }

    // Display the image with the rememberAsyncImagePainter version
    composeTestRule.setContent {
      LocalImageDisplay(
          imagePath = file.absolutePath,
          testVersionRemeberAsync = true,
          contentDescription = "Plant image")
    }

    // Check that the image is displayed
    composeTestRule.onNodeWithContentDescription("Plant image").assertExists()
  }
}
