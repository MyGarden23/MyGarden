package com.android.sample.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel

/** Represents the different orientations the camera can take. */
enum class CameraOrientation {
  FRONT,
  BACK
}

/**
 * ViewModel responsible for managing camera state and actions. It should be used with a
 * corresponding CameraScreen.
 */
open class CameraViewModel : ViewModel() {

  /** The current orientation of the camera (initially back camera). */
  private var cameraOrientation: CameraOrientation = CameraOrientation.BACK

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
   * Set the current orientation of the camera to the given value
   *
   * @param orientation the new orientation it will take
   */
  fun setOrientation(orientation: CameraOrientation) {
    cameraOrientation = orientation
  }

  /**
   * Returns the current orientation of the camera
   *
   * @return the current orientation
   */
  fun getOrientation(): CameraOrientation {
    return cameraOrientation
  }
}
