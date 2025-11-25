package com.android.mygarden.model.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PushNotificationsServiceTest {

  private lateinit var mockProfileRepo: ProfileRepository
  private lateinit var service: PushNotificationsService
  private lateinit var context: Context
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    mockProfileRepo = mock(ProfileRepository::class.java)
    ProfileRepositoryProvider.repository = mockProfileRepo

    service = PushNotificationsService()
    // 💡 FIX: Inject the context via the new field
    service.testContext = context

    service.testDispatcher = testDispatcher // Inject test dispatcher

    val prefs =
        context.getSharedPreferences(
            PushNotificationsService.Companion.NOTIFICATIONS_SHARED_PREFS, Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
  }

  /** Make sure onNewToken saves the token locally when the user is not logged in */
  @Test
  fun onNewToken_saves_token_locally_when_user_is_not_logged_in() =
      runTest(testDispatcher) {
        val testToken = "test_fcm_token"

        doReturn(false).`when`(mockProfileRepo).attachFCMToken(testToken)

        service.onNewToken(testToken)

        // Advance the dispatcher to complete all pending coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockProfileRepo, times(1)).attachFCMToken(testToken)
        val prefs =
            context.getSharedPreferences(
                PushNotificationsService.Companion.NOTIFICATIONS_SHARED_PREFS, Context.MODE_PRIVATE)
        val token =
            prefs.getString(PushNotificationsService.Companion.SHARED_PREFS_FCM_TOKEN_ID, null)
        assertTrue("Token should be saved: $token", token != null && token == "test_fcm_token")
      }

  /** Make sure onNewToken attaches the token to the user when the user is logged in */
  @Test
  fun onNewToken_attaches_token_when_user_is_logged_in() =
      runTest(testDispatcher) {
        val testToken = "test_fcm_token"

        doReturn(true).`when`(mockProfileRepo).attachFCMToken(testToken)

        service.onNewToken(testToken)

        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockProfileRepo, times(1)).attachFCMToken(testToken)
        val prefs =
            context.getSharedPreferences(
                PushNotificationsService.Companion.NOTIFICATIONS_SHARED_PREFS, Context.MODE_PRIVATE)
        assertTrue("SharedPreferences should remain empty", prefs.all.isEmpty())
      }
}
