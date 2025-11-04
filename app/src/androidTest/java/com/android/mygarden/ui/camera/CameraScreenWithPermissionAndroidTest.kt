package com.android.mygarden.ui.camera

import android.Manifest
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresCamera

@RequiresCamera
@RunWith(AndroidJUnit4::class)
class CameraScreenWithPermissionAndroidTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Grant camera access to avoid requesting access during test which is not possible
  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)
  lateinit var viewModel: CameraViewModel

  @Before
  fun setup() {
    viewModel = CameraViewModel()
    composeTestRule.setContent { CameraScreen(cameraViewModel = viewModel) }
    composeTestRule.waitForIdle()
  }

  @Test
  fun cameraScreenButtonsTestTagsAreDisplayed() {
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON)
        .assertExists()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON)
        .assertExists()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.ACCESS_GALLERY_BUTTON)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun cameraScreenButtonsAreEnabled() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).assertIsEnabled()
    composeTestRule.onNodeWithTag(CameraScreenTestTags.ACCESS_GALLERY_BUTTON).assertIsEnabled()
  }

  @Test
  fun switchCameraButtonWorks() {
    assertEquals(viewModel.uiState.value.cameraSelector, CameraSelector.DEFAULT_BACK_CAMERA)
    composeTestRule.onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON).performClick()
    assertEquals(viewModel.uiState.value.cameraSelector, CameraSelector.DEFAULT_FRONT_CAMERA)
    composeTestRule.onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON).performClick()
    assertEquals(viewModel.uiState.value.cameraSelector, CameraSelector.DEFAULT_BACK_CAMERA)
  }

  @Test
  fun cameraPreviewIsVisible() {
    // Check that the camera preview is active
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.PREVIEW_VIEW)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun takePictureButtonDoNotCrash() {
    // Should not crash
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()
  }

  @Test
  fun accessGalleryButtonDoNotCrash() {
    // Should not crash
    composeTestRule.onNodeWithTag(CameraScreenTestTags.ACCESS_GALLERY_BUTTON).performClick()
  }

  /* ViewModel tests (need context hence not "real" unit tests) */

  @Test
  fun hasCameraAccesWorksWhenAccessIsGranted() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertTrue(viewModel.hasCameraPermission(context))
  }

  @Test
  fun hasAlreadDeniedPermissionIsFalseWhenFirstScreenComposition() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertFalse(viewModel.hasAlreadyDeniedCameraPermission(context))
  }

  @Test
  fun hasAlreadDeniedPermissionIsTrueWhenSet() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    viewModel.sethasAlreadyDeniedCameraPermission(context, true)
    assertTrue(viewModel.hasAlreadyDeniedCameraPermission(context))
    viewModel.sethasAlreadyDeniedCameraPermission(context, false)
    assertFalse(viewModel.hasAlreadyDeniedCameraPermission(context))
  }

  @Test
  fun switchOrientationWorksProperly() {
    assertEquals(viewModel.uiState.value.cameraSelector, CameraSelector.DEFAULT_BACK_CAMERA)
    viewModel.switchOrientation()
    assertEquals(viewModel.uiState.value.cameraSelector, CameraSelector.DEFAULT_FRONT_CAMERA)
    viewModel.switchOrientation()
    assertEquals(viewModel.uiState.value.cameraSelector, CameraSelector.DEFAULT_BACK_CAMERA)
  }

  @Test
  fun cameraUIStateInitializesWithBackCamera() {
    // Test that UI state initializes correctly
    assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.uiState.value.cameraSelector)
  }

  @Test
  fun viewModelSharedPreferencesHandling() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Test initial state
    val initialState = viewModel.hasAlreadyDeniedCameraPermission(context)

    // Set to true and verify
    viewModel.sethasAlreadyDeniedCameraPermission(context, true)
    assertTrue(viewModel.hasAlreadyDeniedCameraPermission(context))

    // Set to false and verify
    viewModel.sethasAlreadyDeniedCameraPermission(context, false)
    assertFalse(viewModel.hasAlreadyDeniedCameraPermission(context))

    // Reset to original state
    viewModel.sethasAlreadyDeniedCameraPermission(context, initialState)
  }

  @Test
  fun cameraPermissionCheckReturnsBoolean() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hasPermission = viewModel.hasCameraPermission(context)

    // Should return a boolean value (either true or false)
    assertTrue("Permission check should return true", hasPermission)
  }
}
