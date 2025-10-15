package com.android.mygarden.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R

/** Test tags used for testing the camera screen */
object CameraScreenTestTags {
  const val FLIP_CAMERA_BUTTON = "flip_camera"
  const val TAKE_PICTURE_BUTTON = "take_picture"
  const val ACCESS_GALLERY_BUTTON = "access_gallery"
  const val PREVIEW_VIEW = "preview_view"
}

/**
 * Represents the color that the buttons of the camera screen take. It must not change depending on
 * the light or dark theme.
 */
private val BUTTONS_COLOR = Color.White

/**
 * Screen that allows the user to take a picture of a plant or load one from the gallery. The
 * picture can then be processed to a LLM api to extract the wanted information: description,
 * watering, health status, etc.
 *
 * Currently: the screen previews the camera and display the action buttons that do nothing
 *
 * @param modifier the optional modifier of the composable
 * @param cameraViewModel the optional View Model of the camera screen
 * @param onPictureTaken the optional lambda called whenever the user takes a picture
 *
 * TODO: Implement the picture taking and gallery access logic
 */
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier,
    cameraViewModel: CameraViewModel = viewModel(),
    onPictureTaken: (String) -> Unit = {}
) {
  val uiState = cameraViewModel.uiState.collectAsState()
  val context = LocalContext.current
  // Request Camera permission when composing the Camera Screen
  val cameraPermissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(), onResult = { /* Do nothing */})
  // Currently suppose that the user will accept camera access
  // TODO: Implement the workflow for handling the user declining camera access and gallery access
  LaunchedEffect(Unit) {
    if (!hasCameraPermission(context)) {
      cameraPermissionLauncher.launch(CAMERAX_PERMISSION)
    }
  }

  val controller = remember {
    // Enable the camera controller to capture images
    LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE) }
  }

  Scaffold(
      bottomBar = { /* TODO: Add the navigation bottom bar when ready */},
      content = { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
          CameraPreview(controller = controller, modifier = modifier.fillMaxSize())
          // Button for switching between back camera and front camera
          IconButton(
              onClick = {
                cameraViewModel.switchOrientation()
                controller.cameraSelector = uiState.value.cameraSelector
              },
              modifier =
                  modifier.padding(20.dp, 20.dp).testTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON)) {
                Icon(
                    Icons.Default.FlipCameraAndroid,
                    contentDescription = "Flip Camera Icon",
                    modifier = modifier.size(30.dp),
                    tint = BUTTONS_COLOR)
              }
          // Button for taking picture
          IconButton(
              modifier =
                  modifier
                      .align(Alignment.BottomCenter)
                      .padding(bottom = 60.dp)
                      .size(70.dp)
                      .testTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON),
              onClick = { cameraViewModel.takePicture(context, controller, onPictureTaken) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_photo_button_mygarden),
                    contentDescription = "Take picture Icon",
                    modifier = modifier.size(70.dp),
                    tint = BUTTONS_COLOR)
              }
          // Button for accessing the gallery
          IconButton(
              modifier =
                  modifier
                      .align(Alignment.BottomCenter)
                      .offset(x = 100.dp)
                      .padding(bottom = 60.dp)
                      .size(70.dp)
                      .testTag(CameraScreenTestTags.ACCESS_GALLERY_BUTTON),
              onClick = { /* TODO: Handle gallery access logic */}) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = "Open Gallery Icon",
                    modifier = modifier.size(40.dp),
                    tint = BUTTONS_COLOR)
              }
        }
      })
}

/**
 * Composable that displays the camera preview
 *
 * @param controller the controller that manages the camera lifecycle and outputs the preview
 * @param modifier optional modifier of the composable
 */
@Composable
private fun CameraPreview(controller: LifecycleCameraController, modifier: Modifier = Modifier) {
  val lifeCycleOwner = LocalLifecycleOwner.current
  AndroidView(
      factory = { context ->
        PreviewView(context).apply {
          this.controller = controller
          controller.bindToLifecycle(lifeCycleOwner)
        }
      },
      modifier = modifier.testTag(CameraScreenTestTags.PREVIEW_VIEW))
}

/* Gallery and Camera access management
 * Camera permission is for the moment assumed to be granted by the user, access workflow will be
 * implemented later during the project
 * Gallery access is for the moment not used and will be implemented later during the project
 * */

/**
 * Returns true or false depending on whether the user has granted camera permission
 *
 * @param context the context used to access permission state
 * @return true if the user has granted the app camera permission, false otherwise
 */
private fun hasCameraPermission(context: Context): Boolean {
  return ContextCompat.checkSelfPermission(context, CAMERAX_PERMISSION) ==
      PackageManager.PERMISSION_GRANTED
}

/** The camera permission that is required to use CameraX */
private val CAMERAX_PERMISSION = Manifest.permission.CAMERA
