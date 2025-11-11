package com.android.mygarden.ui.camera

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowContentResolver

/**
 * Quick sanity tests for the gallery flow:
 * - rotation helper covers all EXIF cases
 * - happy path writes a real JPEG under filesDir and calls back
 * - error path doesn’t crash (and doesn’t callback)
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

  /** Provider that always fails so we can hit the catch branch easily. */
  class FailingProvider : ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: Uri) = null

    override fun query(
        u: Uri,
        p: Array<out String>?,
        s: String?,
        a: Array<out String>?,
        o: String?
    ) = null

    override fun insert(u: Uri, v: ContentValues?) = null

    override fun delete(u: Uri, s: String?, a: Array<out String>?) = 0

    override fun update(u: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
      throw IllegalStateException("boom")
    }
  }

  @Test
  fun `rotateBitmapIfNeeded covers 0 90 180 270 in one go`() {
    // Table-driven: one test covers all EXIF branches.
    fun rotate(bmp: Bitmap, exif: Int): Bitmap {
      val m =
          CameraViewModel::class
              .java
              .getDeclaredMethod(
                  "rotateBitmapIfNeeded", Bitmap::class.java, Int::class.javaPrimitiveType)
      m.isAccessible = true
      return m.invoke(vm, bmp, exif) as Bitmap
    }

    val src = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
    val cases =
        listOf(
            ExifInterface.ORIENTATION_NORMAL to (100 to 50),
            ExifInterface.ORIENTATION_ROTATE_90 to (50 to 100),
            ExifInterface.ORIENTATION_ROTATE_180 to (100 to 50),
            ExifInterface.ORIENTATION_ROTATE_270 to (50 to 100))

    for ((exif, expected) in cases) {
      val out = rotate(src, exif)
      assertEquals("width for exif=$exif", expected.first, out.width)
      assertEquals("height for exif=$exif", expected.second, out.height)
    }
  }

  @Test
  fun `onImagePickedFromGallery writes a valid JPEG into app files and invokes callback`() {
    // Build a tiny JPEG on disk to simulate a picked image.
    val src = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
    val tmp = File.createTempFile("exif_src_", ".jpg", context.cacheDir)
    FileOutputStream(tmp).use { out -> src.compress(Bitmap.CompressFormat.JPEG, 90, out) }

    // Use file:// to keep Robolectric happy.
    val uri = Uri.fromFile(tmp)

    var cb: String? = null
    vm.onImagePickedFromGallery(context, uri) { cb = it }

    // Callback should provide the saved path.
    assertNotNull("Callback should be invoked", cb)
    val savedPath = cb!!
    val savedFile = File(savedPath)

    // File exists, not empty, and lives under filesDir.
    assertTrue("Saved file should exist", savedFile.exists())
    assertTrue("Saved file should be non-empty", savedFile.length() > 0L)
    assertTrue("Saved file should be under filesDir", savedPath.startsWith(context.filesDir.path))

    // Quick JPEG check (FF D8).
    val header = savedFile.inputStream().use { it.readNBytes(2) }
    assertEquals(0xFF, header[0].toInt() and 0xFF)
    assertEquals(0xD8, header[1].toInt() and 0xFF)

    // Decode sanity.
    val savedBmp = BitmapFactory.decodeFile(savedPath)
    assertTrue(savedBmp.width > 0 && savedBmp.height > 0)
  }

  @Test
  fun `onImagePickedFromGallery error path does not crash and does not callback`() {
    // Route a content:// URI to a provider that always fails.
    ShadowContentResolver.registerProviderInternal("com.test.fail", FailingProvider())
    val bad = Uri.parse("content://com.test.fail/anything")

    var called = false
    vm.onImagePickedFromGallery(context, bad) { called = true }

    // If VM returns early in catch (recommended), callback must not be hit.
    assertFalse(called)
  }
}
