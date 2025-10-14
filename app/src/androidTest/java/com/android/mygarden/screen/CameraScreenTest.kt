package com.android.mygarden.screen

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.ui.camera.CameraScreen
import com.android.mygarden.ui.camera.CameraScreenTestTags
import com.android.mygarden.ui.camera.CameraViewModel
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenWithPermissionAndroidTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Grant camera access to avoid requesting access during test which is not possible
  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
  lateinit var viewModel: CameraViewModel

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand("pm revoke ${context.packageName} android.permission.CAMERA")
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
  fun takePictureButtonCallsViewModel() {
    // TODO: Implement when requires logic works
  }
}

@RunWith(AndroidJUnit4::class)
class CameraScreenWithoutPermissionTest {

  @get:Rule val composeTestRule = createComposeRule()

  lateinit var viewModel: CameraViewModel

  @Before
  fun setup() {
    Intents.init()
    viewModel = CameraViewModel()
    composeTestRule.setContent { CameraScreen(cameraViewModel = viewModel) }
    composeTestRule.waitForIdle()
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  @Test
  fun cameraScreenNoAccessButtonsTestTagsAreDisplayed() {
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.ENABLE_CAMERA_PERMISSION)
        .assertExists()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.ACCESS_GALLERY_NO_CAMERA_ACCESS_BUTTON)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun cameraScreenNoAccessButtonsAreEnabled() {
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.ACCESS_GALLERY_NO_CAMERA_ACCESS_BUTTON)
        .assertIsEnabled()
  }

  @Test
  fun cameraPreviewIsNotVisibleWithNoAccess() {
    // Check that the camera preview is inactive when no camera access is granted
    composeTestRule.onNodeWithTag(CameraScreenTestTags.PREVIEW_VIEW).assertDoesNotExist()
  }

  // Instead of using real intents mock them using intending()

  @Test
  fun reAskForPermissionLaunchesCorrectIntent() {
    composeTestRule.onNodeWithTag(CameraScreenTestTags.ENABLE_CAMERA_PERMISSION).performClick()
    intended(allOf(hasAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)))
  }

  @Test
  fun pressingGalleryAccessButtonDoesNotCrash() {
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.ACCESS_GALLERY_NO_CAMERA_ACCESS_BUTTON)
        .performClick()
    // Should not crash
  }

  @Test
  fun hasCameraAccessWorksWhenNoAccessIsGranted() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertFalse(viewModel.hasCameraPermission(context))
  }
}
