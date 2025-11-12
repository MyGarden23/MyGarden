package com.android.mygarden.ui.camera

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.mygarden.R
import com.android.mygarden.ui.navigation.NavigationTestTags

/** Test tags used for testing the camera screen */
object CameraScreenTestTags {
  const val FLIP_CAMERA_BUTTON = "flip_camera"
  const val TAKE_PICTURE_BUTTON = "take_picture"
  const val ACCESS_GALLERY_BUTTON = "access_gallery"
  const val PREVIEW_VIEW = "preview_view"
  const val ACCESS_GALLERY_NO_CAMERA_ACCESS_BUTTON = "access_gallery_no_camera"
  const val ENABLE_CAMERA_PERMISSION = "enable_camera_permission"
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
  val lifecycleOwner = LocalLifecycleOwner.current

  /* Keep the camera permission in a MutableState to make the screen recompose when
   * the camera permission is updated */
  val cameraPermission = remember { mutableStateOf(cameraViewModel.hasCameraPermission(context)) }

  // Composable launchers to access camera or pick a photo from gallery
  val cameraPermissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { cameraPermission.value = it })

  // Opens the system photo picker and sends the selected image to the ViewModel,
  // so it follows the same flow as when a picture is taken with the camera
  val photoPickerLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri
        ->
        uri?.let {
          cameraViewModel.onImagePickedFromGallery(
              context = context,
              uri = it,
              onPictureTaken = onPictureTaken,
              onError = {
                Toast.makeText(
                        context,
                        context.getString(R.string.error_fail_take_picture),
                        Toast.LENGTH_SHORT)
                    .show()
              }) // same callback as the camera button
        }
      }

  LaunchedEffect(Unit) {
    // Refresh the camera permission when the screen is created
    cameraPermission.value = cameraViewModel.hasCameraPermission(context)
    // If not already asked, ask for the permission to use the camera
    if (!cameraViewModel.hasAlreadyDeniedCameraPermission(context) && !cameraPermission.value) {
      cameraPermissionLauncher.launch(CAMERAX_PERMISSION)
      cameraViewModel.sethasAlreadyDeniedCameraPermission(context, true)
    }
  }

  /* Make sure that when the screen resumes the access to the camera is updated
   * (for example when the user comes back from changing preferences in the settings) */
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      // On resume, refresh camera permissions
      if (event == Lifecycle.Event.ON_RESUME) {
        cameraPermission.value = cameraViewModel.hasCameraPermission(context)
      }
    }
    // Mandatory to dispose of the observer
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Scaffold(
      modifier = modifier.testTag(NavigationTestTags.CAMERA_SCREEN),
      content = { paddingValues ->
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)) {
              /* Camera preview displayed when the user gave access to the camera */
              if (cameraPermission.value) {
                val controller =
                    remember(uiState.value.cameraSelector) {
                      // Enable the camera controller to capture images
                      try {
                        LifecycleCameraController(context).apply {
                          cameraSelector = uiState.value.cameraSelector
                          setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                        }
                      } catch (e: Exception) {
                        // If no front camera just go to back camera
                        LifecycleCameraController(context).apply {
                          cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                          setEnabledUseCases(CameraController.IMAGE_CAPTURE)
                        }
                      }
                    }
                CameraPreview(controller = controller, modifier = modifier.fillMaxSize())
                // Button for switching between back camera and front camera
                IconButton(
                    onClick = { cameraViewModel.switchOrientation() },
                    modifier =
                        modifier
                            .padding(20.dp, 20.dp)
                            .testTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON)) {
                      Icon(
                          Icons.Default.FlipCameraAndroid,
                          contentDescription =
                              context.getString(R.string.flip_camera_icon_description),
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
                    onClick = {
                      cameraViewModel.takePicture(
                          context = context,
                          controller = controller,
                          onPictureTaken = onPictureTaken,
                          onError = {
                            Toast.makeText(
                                    context,
                                    context.getString(R.string.error_fail_take_picture),
                                    Toast.LENGTH_SHORT)
                                .show()
                          })
                    }) {
                      Icon(
                          painter = painterResource(R.drawable.ic_photo_button_mygarden),
                          contentDescription =
                              context.getString(R.string.take_picture_icon_description),
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
                    onClick = {
                      photoPickerLauncher.launch(
                          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) {
                      Icon(
                          Icons.Default.Photo,
                          contentDescription =
                              context.getString(R.string.open_gallery_icon_description),
                          modifier = modifier.size(40.dp),
                          tint = BUTTONS_COLOR)
                    }
              } else {
                /* Screen displayed when no camera access is granted. The user should still be
                 * able to upload pictures from the gallery even if no camera access is granted */
                NoCameraAccessScreen(
                    onReaskCameraAccess = {
                      // Open settings on the right permission page
                      val intent =
                          Intent(
                              Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                              Uri.fromParts("package", context.packageName, null))
                      try {
                        context.startActivity(intent)
                      } catch (_: ActivityNotFoundException) {
                        Log.e("NoCameraAccessScreen", "Error accessing the settings app.")
                        Toast.makeText(
                                context,
                                context.getString(R.string.error_accessing_settings_user),
                                Toast.LENGTH_SHORT)
                            .show()
                      }
                    },
                    onGalleryAccess = {
                      photoPickerLauncher.launch(
                          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    })
              }
            }
      })
}

/**
 * Composable that displays the camera preview
 *
 * @param controller the controller that manages the camera lifecycle and outputs the preview
 * @param modifier the optional modifier of the composable
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
      modifier = modifier.testTag(CameraScreenTestTags.PREVIEW_VIEW),
      update = { view ->
        if (view.controller !== controller) {
          view.controller = controller
          controller.bindToLifecycle(lifeCycleOwner)
        }
      })
}

/**
 * Composable that allows the user to upload photos from the gallery even when no camera access is
 * granted
 *
 * @param modifier the optional modifier of the composable
 * @param onGalleryAccess the lambda executed when the gallery access action is performed
 * @param onReaskCameraAccess the lambda executed when the user wants to grant the app camera access
 */
@Composable
private fun NoCameraAccessScreen(
    modifier: Modifier = Modifier,
    onGalleryAccess: () -> Unit = {},
    onReaskCameraAccess: () -> Unit = {}
) {
  val context = LocalContext.current
  Column(
      modifier = modifier.fillMaxSize().wrapContentSize(align = Alignment.Center),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            modifier =
                modifier
                    .padding(bottom = 20.dp)
                    .testTag(CameraScreenTestTags.ACCESS_GALLERY_NO_CAMERA_ACCESS_BUTTON),
            colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primaryContainer),
            onClick = { onGalleryAccess() }) {
              Icon(
                  Icons.Default.Photo,
                  contentDescription = context.getString(R.string.open_gallery_icon_description),
                  modifier = modifier.size(30.dp),
                  tint = BUTTONS_COLOR)
              Text(
                  context.getString(R.string.upload_picture_gallery_text),
                  modifier = modifier.padding(horizontal = 5.dp))
            }
        Text(
            context.getString(R.string.give_camera_access_text),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier =
                modifier
                    .testTag(CameraScreenTestTags.ENABLE_CAMERA_PERMISSION)
                    .clickable(onClick = { onReaskCameraAccess() }))
      }
}
