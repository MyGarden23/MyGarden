package com.android.mygarden.ui.camera

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import android.content.res.AssetFileDescriptor
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
    vm.onImagePickedFromGallery(context, uri, onPictureTaken = { cb = it }, onError = {})

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
    // Provider qui échoue systématiquement
    val authority = "com.test.fail"
    val provider = FailingProvider().apply {
      attachInfo(context, ProviderInfo().apply { this.authority = authority })
    }
    ShadowContentResolver.registerProviderInternal(authority, provider)

    val bad = Uri.parse("content://$authority/anything")

    var onSuccessCalled = false
    var onErrorCalled = false

    vm.onImagePickedFromGallery(
      context = context,
      uri = bad,
      onPictureTaken = { onSuccessCalled = true }, // ne doit pas être appelé
      onError = { onErrorCalled = true }           // doit être appelé
    )

    assertFalse("onPictureTaken must not be called on failure", onSuccessCalled)
    assertTrue("onError must be called on failure", onErrorCalled)
  }

  // Provider bound to a specific file; serves both InputStream and FileDescriptor.
  private class PfdProvider(private val file: File) : ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: Uri) = "image/jpeg"

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

    // used by ContentResolver.openInputStream(uri)
    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
      val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
      return AssetFileDescriptor(
          pfd,
          /* startOffset = */ 0,
          /* length      = */ AssetFileDescriptor.UNKNOWN_LENGTH // <- key change
          )
    }

    // used by ContentResolver.openFileDescriptor(uri, "r")
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
      return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
  }

  @Test
  fun `onImagePickedFromGallery hits EXIF block via PFD`() {
    // Create a real JPEG on disk
    val bmp = Bitmap.createBitmap(40, 20, Bitmap.Config.ARGB_8888)
    val tmp = File.createTempFile("exif_src_", ".jpg", context.cacheDir)
    FileOutputStream(tmp).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }

    // (Optional) write an EXIF orientation; we won’t assert rotation in Robolectric
    ExifInterface(tmp.absolutePath).apply {
      setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
      saveAttributes()
    }

    // Attach and register a provider with a real authority
    val authority = "com.test.imageprovider"
    val provider = PfdProvider(tmp)
    val info = ProviderInfo().apply { this.authority = authority }
    provider.attachInfo(context, info)
    ShadowContentResolver.registerProviderInternal(authority, provider)

    // Build a content:// URI (any path segment is fine)
    val uri = Uri.parse("content://$authority/image")

    // Sanity: both resolver calls must succeed, or VM will go to catch {}
    context.contentResolver.openInputStream(uri).use { ins ->
      assertNotNull("openInputStream must work", ins)
    }
    context.contentResolver.openFileDescriptor(uri, "r").use { pfd ->
      assertNotNull("openFileDescriptor must work", pfd)
    }
    // Call VM: if try-block (incl. EXIF via PFD) executes, callback is non-null
    var cb: String? = null
    vm.onImagePickedFromGallery(context, uri, onPictureTaken = { cb = it }, onError = {})

    assertNotNull("Callback should be invoked", cb)
    assertTrue(File(cb!!).exists())
  }
}
