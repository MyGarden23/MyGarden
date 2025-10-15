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

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresNoCamera

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

@RequiresNoCamera
@RunWith(AndroidJUnit4::class)
class CameraScreenWithoutPermissionTest {

  @get:Rule val composeTestRule = createComposeRule()

  lateinit var viewModel: CameraViewModel

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    // Revoke camera permission
    InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand("pm revoke ${context.packageName} android.permission.CAMERA")

    // Wait for the permission change to apply
    Thread.sleep(1000)

    Intents.init()
    viewModel = CameraViewModel()

    // Set the content and wait for idle to ensure composition is complete
    composeTestRule.setContent { CameraScreen(cameraViewModel = viewModel) }
    composeTestRule.waitForIdle()

    // Additional wait to ensure permission is correctly revoked
    composeTestRule.waitUntil(timeoutMillis = 5000) { !viewModel.hasCameraPermission(context) }
  }

  @After
  fun tearDown() {
    Intents.release()
  }

  @Test
  fun cameraScreenNoAccessButtonsTestTagsAreDisplayed() {
    // Wait for UI to settle after permission revocation
    composeTestRule.waitForIdle()

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
    // Wait for UI to settle after permission revocation
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.ACCESS_GALLERY_NO_CAMERA_ACCESS_BUTTON)
        .assertIsEnabled()
  }

  @Test
  fun cameraPreviewIsNotVisibleWithNoAccess() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertFalse("Camera permission should be revoked", viewModel.hasCameraPermission(context))

    // Check that the camera preview is inactive when no camera access is granted
    composeTestRule.onNodeWithTag(CameraScreenTestTags.PREVIEW_VIEW).assertDoesNotExist()
  }

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

  @Test
  fun noCameraAccessScreenElementsAreVisibleWhenPermissionDenied() {
    // Wait a bit more for UI to stabilize after permission revocation
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Verify that no camera access elements are shown
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.ENABLE_CAMERA_PERMISSION)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.ACCESS_GALLERY_NO_CAMERA_ACCESS_BUTTON)
        .assertExists()
        .assertIsDisplayed()

    // Ensure camera preview is not shown
    composeTestRule.onNodeWithTag(CameraScreenTestTags.PREVIEW_VIEW).assertDoesNotExist()
  }
}
