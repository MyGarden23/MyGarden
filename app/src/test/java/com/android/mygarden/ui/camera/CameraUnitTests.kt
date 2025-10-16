package com.android.mygarden.ui.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraUnitTests {

  private lateinit var viewModel: CameraViewModel
  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    viewModel = CameraViewModel()
  }

  @Test
  fun `initial UI state has back camera selector`() {
    assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.uiState.value.cameraSelector)
  }

  @Test
  fun `switchOrientation toggles between back and front camera`() {
    // Initial state should be back camera
    assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.uiState.value.cameraSelector)

    // Switch to front camera
    viewModel.switchOrientation()
    assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, viewModel.uiState.value.cameraSelector)

    // Switch back to back camera
    viewModel.switchOrientation()
    assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.uiState.value.cameraSelector)
  }

  @Test
  fun `hasAlreadyDeniedCameraPermission returns false by default`() {
    // Clear any existing preferences first
    context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE).edit().clear().apply()

    assertFalse(viewModel.hasAlreadyDeniedCameraPermission(context))
  }

  @Test
  fun `sethasAlreadyDeniedCameraPermission stores and retrieves value correctly`() {
    // Test setting to true
    viewModel.sethasAlreadyDeniedCameraPermission(context, true)
    assertTrue(viewModel.hasAlreadyDeniedCameraPermission(context))

    // Test setting to false
    viewModel.sethasAlreadyDeniedCameraPermission(context, false)
    assertFalse(viewModel.hasAlreadyDeniedCameraPermission(context))
  }

  @Test
  fun `sethasAlreadyDeniedCameraPermission persists across ViewModel instances`() {
    // Set value with first ViewModel instance
    viewModel.sethasAlreadyDeniedCameraPermission(context, true)

    // Create new ViewModel instance and verify value persists
    val newViewModel = CameraViewModel()
    assertTrue(newViewModel.hasAlreadyDeniedCameraPermission(context))

    // Clean up
    newViewModel.sethasAlreadyDeniedCameraPermission(context, false)
  }

  @Test
  fun `CAMERAX_PERMISSION constant has correct value`() {
    assertEquals(android.Manifest.permission.CAMERA, CAMERAX_PERMISSION)
  }

  @Test
  fun `CameraUIState has correct default camera selector`() {
    val uiState = CameraUIState()
    assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, uiState.cameraSelector)
  }

  @Test
  fun `multiple switch operations work correctly`() {
    // Verify initial state
    assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.uiState.value.cameraSelector)

    // Perform multiple switches
    viewModel.switchOrientation() // To front
    assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, viewModel.uiState.value.cameraSelector)

    viewModel.switchOrientation() // To back
    assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.uiState.value.cameraSelector)

    viewModel.switchOrientation() // To front again
    assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, viewModel.uiState.value.cameraSelector)

    viewModel.switchOrientation() // To back again
    assertEquals(CameraSelector.DEFAULT_BACK_CAMERA, viewModel.uiState.value.cameraSelector)
  }

  @Test
  fun `hasCameraPermission integrates with actual permission system`() {
    // This test verifies the integration with the real Android permission system
    // In Robolectric, permissions are typically granted by default for testing
    val hasPermission = viewModel.hasCameraPermission(context)

    // The result should be consistent when called multiple times
    assertEquals(hasPermission, viewModel.hasCameraPermission(context))
    assertEquals(hasPermission, viewModel.hasCameraPermission(context))
  }

  @Test
  fun `CameraUIState data class behaves correctly`() {
    val state1 = CameraUIState(CameraSelector.DEFAULT_BACK_CAMERA)
    val state2 = CameraUIState(CameraSelector.DEFAULT_BACK_CAMERA)
    val state3 = CameraUIState(CameraSelector.DEFAULT_FRONT_CAMERA)

    // Test equality
    assertEquals(state1.cameraSelector, state2.cameraSelector)
    assertFalse(
        "Different camera selectors should not be equal",
        state1.cameraSelector == state3.cameraSelector)

    // Test copy functionality (data class feature)
    val copiedState = state1.copy(cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA)
    assertEquals(CameraSelector.DEFAULT_FRONT_CAMERA, copiedState.cameraSelector)
  }

  @Test
  fun `CAMERAX_PERMISSION is accessible from companion object`() {
    // Verify the constant is accessible and has the expected value
    val permission = CAMERAX_PERMISSION
    assertEquals(android.Manifest.permission.CAMERA, permission)

    // Should be the same value every time
    assertEquals(permission, CAMERAX_PERMISSION)
  }

  @Test
  fun `SharedPreferences file name is consistent`() {
    val prefs1 = context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE)
    val prefs2 = context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE)

    // Both should refer to the same preferences instance
    prefs1.edit().putBoolean("test_key", true).apply()
    assertTrue("SharedPreferences should be consistent", prefs2.getBoolean("test_key", false))

    // Clean up
    prefs1.edit().remove("test_key").apply()
  }

  @Test
  fun `ViewModel handles context changes gracefully`() {
    // Test with different contexts (though in Robolectric they'll be the same)
    val appContext = context.applicationContext

    viewModel.sethasAlreadyDeniedCameraPermission(context, true)
    assertTrue(viewModel.hasAlreadyDeniedCameraPermission(appContext))

    viewModel.sethasAlreadyDeniedCameraPermission(appContext, false)
    assertFalse(viewModel.hasAlreadyDeniedCameraPermission(context))
  }

  @Test
  fun `multiple rapid orientation switches work correctly`() {
    // Test rapid consecutive switches
    for (i in 0 until 10) {
      viewModel.switchOrientation()
      val expectedSelector =
          if (i % 2 == 0) {
            CameraSelector.DEFAULT_FRONT_CAMERA
          } else {
            CameraSelector.DEFAULT_BACK_CAMERA
          }
      assertEquals("Switch $i failed", expectedSelector, viewModel.uiState.value.cameraSelector)
    }
  }

  @Test
  fun `SharedPreferences key constant is correct`() {
    // Verify the internal key matches what's expected
    // This test ensures consistency if the key ever changes
    viewModel.sethasAlreadyDeniedCameraPermission(context, true)

    val prefs = context.getSharedPreferences("camera_prefs", Context.MODE_PRIVATE)
    assertTrue("Key should be 'has_denied_camera'", prefs.getBoolean("has_denied_camera", false))
  }
}
