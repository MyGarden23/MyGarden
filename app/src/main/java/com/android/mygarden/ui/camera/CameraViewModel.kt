package com.android.mygarden.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The camera permission */
val CAMERAX_PERMISSION = Manifest.permission.CAMERA

/** UI state contains the orientation of the camera (initially back camera). */
data class CameraUIState(var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA)

private const val CAMERA_ERROR_TAG = "CameraPicture"
private const val PREFS_NAME = "camera_prefs"
private const val HAS_DENIED_CAMERA = "has_denied_camera"

/**
 * ViewModel responsible for managing camera state and actions. It should be used with a
 * corresponding CameraScreen.
 */
class CameraViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(CameraUIState())
  val uiState: StateFlow<CameraUIState> = _uiState.asStateFlow()

  /**
   * Captures a picture using the provided controller and pass the resulting Bitmap into the given
   * input onPictureTaken lambda.
   *
   * @param context the context used to obtain executor
   * @param controller the controller that manages the camera lifecycle, image preview and capture
   * @param onPictureTaken the lambda called on the Bitmap resulting from the capture
   * @param onError the lambda called if the picture capture fails
   */
  fun takePicture(
      context: Context,
      controller: LifecycleCameraController,
      onPictureTaken: (String) -> Unit,
      onError: () -> Unit
  ) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
          // If the picture succeed, pass the resulting Bitmap to the given lambda
          override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)
            try {
              // We convert the image to a Bitmap because it will be useful later for sending it to
              // an API
              var bitmap = image.toBitmap()

              // If the conversion to Bitmap rotated the image, we rotate it back such that it
              // doesn't have rotation
              val rotationDegrees = image.imageInfo.rotationDegrees
              if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                bitmap =
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
              }
              image.close()

              // Save the image locally
              val file = saveBitmapToFile(context, bitmap, "plant_${System.currentTimeMillis()}")

              /* Give the image of the plant in Bitmap (to give to an API eventually)
               * and the path of the plant to the next screen
               * */
              onPictureTaken(file.absolutePath)
            } catch (e: Exception) {
              onError()
              Log.e(CAMERA_ERROR_TAG, "ImageProxy could not be converted to a Bitmap", e)
            }
          }

          override fun onError(exception: ImageCaptureException) {
            // If the picture fails, log the exception and keep the stack trace
            super.onError(exception)
            onError()
            Log.e(CAMERA_ERROR_TAG, "Picture could not been taken", exception)
          }
        })
  }

  // Handles an image picked from the gallery and runs it through the same steps as the camera:
  // decode → fix orientation → save locally → trigger onPictureTaken with the file path
  fun onImagePickedFromGallery(
      context: Context,
      uri: Uri,
      onPictureTaken: (String) -> Unit,
      onError: () -> Unit
  ) {
    try {
      // Decode the bitmap from the given URI
      val input =
          context.contentResolver.openInputStream(uri)
              ?: throw IllegalStateException("Cannot open input stream for gallery image")
      var bitmap = input.use { BitmapFactory.decodeStream(it) }

      // 2) Fix rotation if needed using EXIF data
      val orientation =
          runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                  ExifInterface(pfd.fileDescriptor)
                      .getAttributeInt(
                          ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
              }
              .getOrDefault(ExifInterface.ORIENTATION_NORMAL)

      bitmap = rotateBitmapIfNeeded(bitmap, orientation)

      // Save locally (same as camera flow)
      val file = saveBitmapToFile(context, bitmap, "plant_${System.currentTimeMillis()}")

      // Pass the file path back through the same callback
      onPictureTaken(file.absolutePath)
    } catch (e: Exception) {
      onError()
      Log.e(CAMERA_ERROR_TAG, "Failed to import gallery image", e)
    }
  }

  private fun rotateBitmapIfNeeded(src: Bitmap, exifOrientation: Int): Bitmap {
    val degrees =
        when (exifOrientation) {
          ExifInterface.ORIENTATION_ROTATE_90 -> 90f
          ExifInterface.ORIENTATION_ROTATE_180 -> 180f
          ExifInterface.ORIENTATION_ROTATE_270 -> 270f
          else -> 0f
        }
    if (degrees == 0f) return src
    val m = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
  }

  /* Camera permission handling */

  /**
   * Returns true or false depending on whether the user has granted camera permission
   *
   * @param context the context used to access permission state
   * @return true if the user has granted the app camera permission, false otherwise
   */
  fun hasCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, CAMERAX_PERMISSION) ==
        PackageManager.PERMISSION_GRANTED
  }

  /**
   * Returns whether the user has previously denied the camera permission
   *
   * Used to avoid repeatedly asking the user for permission and to provide a better experience even
   * with not every access. The value is stored in SharedPreferences to persist across app launches.
   *
   * @param context the context used to access the Shared Preferences
   */
  fun hasAlreadyDeniedCameraPermission(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getBoolean(HAS_DENIED_CAMERA, false)
  }

  /**
   * Updates the stored Shared Preferences indicating whether the user has already denied the camera
   * permission. Works with hasAlreadyDeniedCameraPermission()
   *
   * @param context the context used to access the Shared Preferences
   * @param value true if the user has already denied camera permission, false otherwise
   */
  fun sethasAlreadyDeniedCameraPermission(context: Context, value: Boolean) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit { putBoolean(HAS_DENIED_CAMERA, value) }
  }

  /* Others useful camera screen functions */

  /**
   * Switches the orientation of the camera (Front or Back) by changing the value of the UI state
   * that contains the camera selector.
   */
  fun switchOrientation() {
    val targetCameraSelector =
        if (_uiState.value.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
          CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
          CameraSelector.DEFAULT_BACK_CAMERA
        }

    // Error handling will be done at the camera binding level
    _uiState.value = _uiState.value.copy(cameraSelector = targetCameraSelector)
  }

  /**
   * Takes the image given in Bitmap type, compress it in JPEG format and store it in
   * context.filesDir which is a local memory place accessible when you have the context of the app.
   *
   * @param context the context of the app
   * @param bitmap the image in bitmap type
   * @param fileName the name we want to give to the image that will be stored
   */
  private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
    val file = File(context.filesDir, "$fileName.jpg")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out) }
    return file
  }
}
