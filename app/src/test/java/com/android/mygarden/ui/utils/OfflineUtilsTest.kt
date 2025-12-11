package com.android.mygarden.ui.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.mygarden.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

/**
 * Unit tests for OfflineUtils.kt functions.
 *
 * These tests verify:
 * 1. handleOfflineClick executes the action when online
 * 2. handleOfflineClick shows a toast and doesn't execute the action when offline
 * 3. handleOfflineClick uses the correct offline message
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineUtilsTest {

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    ShadowToast.reset()
  }

  /** Test that the action is executed when online */
  @Test
  fun handleOfflineClick_executesActionWhenOnline() {
    var actionExecuted = false

    handleOfflineClick(
        isOnline = true,
        context = context,
        offlineMessageResId = R.string.offline_cannot_water_plants,
        onlineAction = { actionExecuted = true })

    // Verify the action was executed
    assertTrue("Action should be executed when online", actionExecuted)

    // Verify no toast was shown
    val toastText = ShadowToast.getTextOfLatestToast()
    assertNull("No toast should be shown when online", toastText)
  }

  /** Test that a toast is shown when offline */
  @Test
  fun handleOfflineClick_showsToastWhenOffline() {
    handleOfflineClick(
        isOnline = false,
        context = context,
        offlineMessageResId = R.string.offline_cannot_water_plants,
        onlineAction = {})

    // Verify the toast was shown with the correct message
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot water plants while offline", toastText)
  }

  /** Test that the action is NOT executed when offline */
  @Test
  fun handleOfflineClick_doesNotExecuteActionWhenOffline() {
    var actionExecuted = false

    handleOfflineClick(
        isOnline = false,
        context = context,
        offlineMessageResId = R.string.offline_cannot_water_plants,
        onlineAction = { actionExecuted = true })

    // Verify the action was NOT executed
    assertFalse("Action should not be executed when offline", actionExecuted)

    // Verify a toast was shown
    val toastText = ShadowToast.getTextOfLatestToast()
    assertEquals("Cannot water plants while offline", toastText)
  }
}
