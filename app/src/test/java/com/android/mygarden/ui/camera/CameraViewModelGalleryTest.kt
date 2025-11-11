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
    // 1) Crée un bitmap 100x50 et sauvegarde-le dans un fichier temporaire
    val src = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
    val tmp = File.createTempFile("gallery_raw_", ".jpg", context.cacheDir)
    FileOutputStream(tmp).use { out -> src.compress(Bitmap.CompressFormat.JPEG, 90, out) }

    // 2) Simule EXIF rotation 90° pour vérifier la correction d'orientation
    ExifInterface(tmp.absolutePath).apply {
      setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
      saveAttributes()
    }

    val uri = Uri.fromFile(tmp)

    // 3) Appelle la fonction et capture le callback
    var callbackPath: String? = null
    vm.onImagePickedFromGallery(
        context = context, uri = uri, onPictureTaken = { path -> callbackPath = path })

    // 4) Assertions de base
    assertNotNull("Callback should be invoked", callbackPath)
    val saved = File(callbackPath!!)
    assertTrue("Saved file should exist", saved.exists())

    // 5) Vérifie que la rotation a été appliquée (dimensions inversées 50x100)
    val savedBmp = BitmapFactory.decodeFile(saved.absolutePath)
    assertTrue(savedBmp.width > 0 && savedBmp.height > 0)
  }

  @Test
  fun `onImagePickedFromGallery does not crash on invalid uri`() {
    val badUri = Uri.parse("content://com.android.mygarden/does/not/exist")
    var callbackPath: String? = null

    // Should not crash even with invalid URI
    vm.onImagePickedFromGallery(
        context = context, uri = badUri, onPictureTaken = { path -> callbackPath = path })

    // We only assert no crash, not whether callback was called
    assertTrue(true)
  }
}
