package com.android.mygarden.model.notifications

import android.Manifest
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.android.mygarden.MainActivity
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowApplication

@RunWith(RobolectricTestRunner::class)
class NotificationsUnitTests {

  @get:Rule val composeRule = createComposeRule()
  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    // Clear prefs before each test
    val prefs = context.getSharedPreferences("notifications_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
  }

  /** Checks that the by default the shared prefs are false */
  @Test
  fun `hasAlreadyDeniedNotificationsPermission returns false by default`() {
    // By default should be false
    assertFalse(NotificationsPermissionHandler.hasAlreadyDeniedNotificationsPermission(context))
  }

  /** Checks that the setter method does store and retrieve the correct value */
  @Test
  fun `setHasAlreadyDeniedNotificationsPermission stores and retrieves value correctly`() {
    // Test setting to true
    NotificationsPermissionHandler.setHasAlreadyDeniedNotificationsPermission(context, true)
    assertTrue(NotificationsPermissionHandler.hasAlreadyDeniedNotificationsPermission(context))

    // Test setting to false
    NotificationsPermissionHandler.setHasAlreadyDeniedNotificationsPermission(context, false)
    assertFalse(NotificationsPermissionHandler.hasAlreadyDeniedNotificationsPermission(context))
  }

  /** Tests that the hasNotificationsPermission works when the permission is not granted */
  @Test
  @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
  fun `hasNotificationsPermission returns false when not granted`() {
    // Grant the notifications permission with ShadowApplication
    assertFalse(NotificationsPermissionHandler.hasNotificationsPermission(context))
  }

  /** Tests that the hasNotificationsPermission works when the permission is granted */
  @Test
  @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
  fun `hasNotificationsPermission returns true when granted`() {
    // Grant the notifications permission with ShadowApplication
    ShadowApplication.getInstance().grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

    assertTrue(NotificationsPermissionHandler.hasNotificationsPermission(context))
  }

  /** Tests that the MainActivity does not launch the intent as it is in a test environment */
  @Test
  fun `mainActivity does not ask for notification permission as in a testing environment`() {
    composeRule.setContent {
      MainActivity()
      context = LocalContext.current
    }
    composeRule.waitForIdle()
    val shadowActivity = Shadows.shadowOf(context as Activity)

    // Get the last requested permission (should be null)
    assertNull(shadowActivity.lastRequestedPermission)
  }
}
