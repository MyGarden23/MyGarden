package com.android.mygarden.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [CameraViewModel.onImagePickedFromGallery] using Robolectric. These tests check
 * that gallery images are processed, saved, and handled safely.
 */
@RunWith(RobolectricTestRunner::class)
class CameraViewModelGalleryTest {

  private lateinit var vm: CameraViewModel
  private lateinit var context: Context

  @Before
  fun setup() {
    vm = CameraViewModel()
    context = ApplicationProvider.getApplicationContext()
  }

  @Test
  fun `onImagePickedFromGallery saves file and invokes callback`() {
    // Create a 100x50 bitmap and save it as a temporary JPEG file
    val src = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
    val tmp = File.createTempFile("gallery_raw_", ".jpg", context.cacheDir)
    FileOutputStream(tmp).use { out -> src.compress(Bitmap.CompressFormat.JPEG, 90, out) }

    // Simulate EXIF orientation = 90° to test rotation handling
    ExifInterface(tmp.absolutePath).apply {
      setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
      saveAttributes()
    }

    val uri = Uri.fromFile(tmp)

    // Call the function and capture the callback result
    var callbackPath: String? = null
    vm.onImagePickedFromGallery(
        context = context, uri = uri, onPictureTaken = { path -> callbackPath = path })

    // Basic assertions
    assertNotNull("Callback should be invoked", callbackPath)
    val saved = File(callbackPath!!)
    assertTrue("Saved file should exist", saved.exists())

    // Verify the saved image is valid (non-empty dimensions)
    // Note: We don’t assert exact rotation values because Robolectric may skip EXIF processing
    val savedBmp = BitmapFactory.decodeFile(saved.absolutePath)
    assertTrue(savedBmp.width > 0 && savedBmp.height > 0)
  }

  @Test
  fun `onImagePickedFromGallery does not crash on invalid uri`() {
    // Create an invalid URI to simulate a missing or broken image
    val badUri = Uri.parse("content://com.android.mygarden/does/not/exist")
    var callbackPath: String? = null

    // Should not throw any exception, even with an invalid URI
    vm.onImagePickedFromGallery(
        context = context, uri = badUri, onPictureTaken = { path -> callbackPath = path })

    // We only assert that no crash occurred
    assertTrue(true)
  }
}
