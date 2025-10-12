package com.android.mygarden.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** UI state contains the orientation of the camera (initially back camera). */
data class CameraUIState(var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA)

/** Error tag used to indicate errors coming from taking picture */
private const val CAMERA_ERROR_TAG = "CameraPicture"

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
   */
  fun takePicture(
      context: Context,
      controller: LifecycleCameraController,
      onPictureTaken: (Bitmap) -> Unit
  ) {
    controller.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
          // If the picture succeed, pass the resulting Bitmap to the given lambda
          override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)
            try {
              val bitmap = image.toBitmap()
              image.close()

              // Save the image locally
              val file = saveBitmapToFile(context, bitmap, "plant_${System.currentTimeMillis()}")

              /*TODO : Check if still useful to do that*/
              onPictureTaken(bitmap)

              /*TODO : Check if we keep that*/
              Log.d("CameraViewModel", "Image saved at: ${file.absolutePath}")
            } catch (e: Exception) {
              toastPictureFail(context)
              Log.e(CAMERA_ERROR_TAG, "ImageProxy could not be converted to a Bitmap", e)
            }
          }

          override fun onError(exception: ImageCaptureException) {
            // If the picture fails, log the exception and keep the stack trace
            super.onError(exception)
            toastPictureFail(context)
            Log.e(CAMERA_ERROR_TAG, "Picture could not been taken", exception)
          }
        })
  }

  /**
   * Display a Toast saying "Failed to take picture." on a fail to take a picture.
   *
   * @param context the context of the application used to display the Toast
   */
  private fun toastPictureFail(context: Context) {
    Toast.makeText(context, "Failed to take picture.", Toast.LENGTH_SHORT).show()
  }

  /**
   * Switches the orientation of the camera (Front or Back) by changing the value of the UI state
   * that contains the camera selector.
   */
  fun switchOrientation() {
    _uiState.value.cameraSelector =
        if (_uiState.value.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
          CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
          CameraSelector.DEFAULT_BACK_CAMERA
        }
  }

  private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File {
    val file = File(context.filesDir, "$fileName.jpg")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out) }
    return file
  }
}
