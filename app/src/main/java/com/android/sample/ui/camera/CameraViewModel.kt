package com.android.sample.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state contains the orientation of the camera (initially back camera).
 */
data class CameraUIState(var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA)

/**
 * ViewModel responsible for managing camera state and actions. It should be used with a
 * corresponding CameraScreen.
 */
open class CameraViewModel : ViewModel() {
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
          override fun onCaptureSuccess(image: ImageProxy) {
            super.onCaptureSuccess(image)
            onPictureTaken(image.toBitmap())
          }

          override fun onError(exception: ImageCaptureException) {
            super.onError(exception)
            Log.e("CameraPicture", "Picture could not been taken")
          }
        })
  }

  /**
   * Switches the orientation of the camera (Front or Back) by changing the value of the UI
   * state that contains the camera selector.
   */
  fun switchOrientation() {
    _uiState.value.cameraSelector =
      if (_uiState.value.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
        CameraSelector.DEFAULT_FRONT_CAMERA
      } else {
        CameraSelector.DEFAULT_BACK_CAMERA
      }
  }
}
