package com.android.mygarden.model.notifications

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.utils.FirebaseEmulator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FriendRequestsFirebaseNotifier {

  private lateinit var notifier: FirebaseFriendRequestNotifier

  @Before
  fun setUp() {
    FirebaseEmulator.connectFunctions()
    notifier = FirebaseFriendRequestNotifier()
  }

  @Test
  fun notifyRequestSent_callsCloudFunction_onEmulator() = runBlocking {
    val targetUid = "TEST_TARGET"
    val fromPseudo = "john"

    val result = runCatching { notifier.notifyRequestSent(targetUid, fromPseudo) }

    assertTrue("Function call should not crash", result.isSuccess)
  }
}
