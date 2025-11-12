package com.android.mygarden.model.notifications

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
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
    service.testDispatcher = testDispatcher // Inject test dispatcher

    val prefs = context.getSharedPreferences("notifications_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
  }

  /* @Test
  fun onNewToken_saves_token_locally_when_user_is_not_logged_in() = runTest(testDispatcher) {
      val testToken = "test_fcm_token"

      doReturn(false).`when`(mockProfileRepo).attachFCMToken(testToken)

      service.onNewToken(testToken)

      // Advance the dispatcher to complete all pending coroutines
      testDispatcher.scheduler.advanceUntilIdle()

      verify(mockProfileRepo, times(1)).attachFCMToken(testToken)
      val prefs = context.getSharedPreferences("notifications_prefs", Context.MODE_PRIVATE)
      val token = prefs.getString("fcm_token", null)
      assertTrue("Token should be saved: $token", token != null && token == "test_fcm_token")
  } */

  @Test
  fun onNewToken_attaches_token_when_user_is_logged_in() =
      runTest(testDispatcher) {
        val testToken = "test_fcm_token"

        doReturn(true).`when`(mockProfileRepo).attachFCMToken(testToken)

        service.onNewToken(testToken)

        testDispatcher.scheduler.advanceUntilIdle()

        verify(mockProfileRepo, times(1)).attachFCMToken(testToken)
        val prefs = context.getSharedPreferences("notifications_prefs", Context.MODE_PRIVATE)
        assertTrue("SharedPreferences should remain empty", prefs.all.isEmpty())
      }
}
