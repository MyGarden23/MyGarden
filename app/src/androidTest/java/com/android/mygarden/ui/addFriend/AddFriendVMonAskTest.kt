package com.android.mygarden.ui.addFriend

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.friends.*
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryFirestore
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.model.profile.PseudoRepositoryProvider
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepositoryFirestore
import com.android.mygarden.model.users.UserProfileRepositoryProvider
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.utils.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddFriendOnAskTest : FirestoreProfileTest() {

  private lateinit var fakeRequests: FakeFriendRequestsRepository

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator.connectFunctions()

    // Default fakes
    fakeRequests = FakeFriendRequestsRepository()

    FriendsRepositoryProvider.repository = FakeFriendsRepository()
    FriendRequestsRepositoryProvider.repository = fakeRequests
    UserProfileRepositoryProvider.repository = UserProfileRepositoryFirestore(db)
    PseudoRepositoryProvider.repository = FakePseudoRepository()

    // Profile repo overridden per test if needed
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(db, auth)
  }

  /** Success case */
  @Test
  fun onAsk_success() {
    val uid = auth.currentUser!!.uid
    println("DEBUG currentUserId in test = $uid")

    val fakeUserProfile = FakeUserProfileRepository()
    fakeUserProfile.profiles[uid] =
        UserProfile(
            id = uid,
            pseudo = "john",
            avatar = Avatar.A1,
            gardeningSkill = "INTERMEDIATE",
            favoritePlant = "ZZPlant",
        )

    UserProfileRepositoryProvider.repository = fakeUserProfile

    val vm = AddFriendViewModel()

    val latch = CountDownLatch(1)
    var success = false
    var error = false

    vm.onAsk(
        userId = "target",
        onSuccess = {
          success = true
          latch.countDown()
        },
        onError = {
          error = true
          latch.countDown()
        })

    assertTrue(latch.await(5, TimeUnit.SECONDS))
    assertFalse(error)
    assertTrue(success)

    assertEquals(1, fakeRequests.incomingRequestsFlow.value.size)
    assertEquals("target", fakeRequests.incomingRequestsFlow.value[0].fromUserId)
  }

  /** Fail Case (currentUserId == null) */
  @Test
  fun onAsk_error_when_currentUserId_null() {

    // Override profile provider specifically for this case
    ProfileRepositoryProvider.repository =
        object : ProfileRepository {
          override fun getCurrentUserId(): String? = null

          override fun getProfile() = flowOf(null)

          override suspend fun saveProfile(profile: Profile) {}

          override suspend fun attachFCMToken(token: String) = false

          override suspend fun getFCMToken(): String? = null

          override fun cleanup() {}
        }

    val vm = AddFriendViewModel()

    val latch = CountDownLatch(1)
    var success = false
    var error = false

    vm.onAsk(
        userId = "target",
        onSuccess = {
          success = true
          latch.countDown()
        },
        onError = {
          error = true
          latch.countDown()
        })

    assertTrue(latch.await(5, TimeUnit.SECONDS))
    assertFalse(success)
    assertTrue(error)
  }

  /** Fail Case (fromPseudo == null) */
  @Test
  fun onAsk_error_when_fromPseudo_null() {
    val uid = auth.currentUser!!.uid

    runBlockingWithTimeout {
      db.collection("users").document(uid).set(mapOf("pseudo" to null)).await()
    }

    // Use real FirestoreProfileRepository but missing pseudo
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(db, auth)

    val vm = AddFriendViewModel()

    val latch = CountDownLatch(1)
    var success = false
    var error = false

    vm.onAsk(
        userId = "target",
        onSuccess = {
          success = true
          latch.countDown()
        },
        onError = {
          error = true
          latch.countDown()
        })

    assertTrue(latch.await(5, TimeUnit.SECONDS))
    assertFalse(success)
    assertTrue(error)
  }

  /**
   * Helper function to run a suspend function with a timeout.
   *
   * @param block The suspend function to run.
   */
  private fun runBlockingWithTimeout(block: suspend () -> Unit) = runBlocking {
    withTimeout(5000) { block() }
  }
}
