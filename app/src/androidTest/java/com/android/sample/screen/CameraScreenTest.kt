package com.android.sample.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.sample.ui.camera.CameraOrientation
import com.android.sample.ui.camera.CameraScreen
import com.android.sample.ui.camera.CameraScreenTestTags
import com.android.sample.ui.camera.CameraViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraScreenAndroidTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Grant camera access to avoid requesting access during test which is not possible
  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(android.Manifest.permission.CAMERA)
  lateinit var viewModel: CameraViewModel

  @Before
  fun setup() {
    viewModel = CameraViewModel()
  }

  @Test
  fun cameraScreenButtonsTestTagsAreDisplayed() {
    composeTestRule.setContent { CameraScreen(cameraViewModel = viewModel) }
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
  fun switchCameraButtonWorks() {
    composeTestRule.setContent { CameraScreen(cameraViewModel = viewModel) }
    assert(viewModel.getOrientation() == CameraOrientation.BACK)
    composeTestRule.onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON).performClick()
    assert(viewModel.getOrientation() == CameraOrientation.FRONT)
    composeTestRule.onNodeWithTag(CameraScreenTestTags.FLIP_CAMERA_BUTTON).performClick()
    assert(viewModel.getOrientation() == CameraOrientation.BACK)
  }

  @Test
  fun cameraPreviewIsVisible() {
    composeTestRule.setContent { CameraScreen(cameraViewModel = viewModel) }
    // Check that the camera preview is active
    composeTestRule
        .onNodeWithTag(CameraScreenTestTags.PREVIEW_VIEW)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun takePictureButtonCallsViewModel() {
    // TODO: Implement when requires logic works
  }
}
