package com.android.mygarden.model.friends

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import app.cash.turbine.test
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FirestoreProfileTest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for the friend requests feature, covering:
 * - [FriendRequestsRepositoryFirestore] behavior on top of Firestore emulator,
 * - [FriendRequestsRepositoryProvider] override mechanism,
 * - [FriendRequestsRepository] contract (askFriend / acceptRequest / refuseRequest).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FriendRequestsRepositoryTest : FirestoreProfileTest() {

  private lateinit var friendRequestsRepo: FriendRequestsRepository
  private lateinit var friendsRepo: FriendsRepository
  private lateinit var currentUserId: String

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  /**
   * Starts Firestore emulator (via [FirestoreProfileTest]), signs in a test user on the Auth
   * emulator, and wires the repositories.
   */
  @Before
  override fun setUp() = runTest {
    super.setUp()

    compose.setContent { MyGardenTheme {} }

    auth = FirebaseAuth.getInstance()

    // Sign in anonymously
    val result = auth.signInAnonymously().await()
    currentUserId = result.user?.uid ?: error("Failed to sign in anonymously on Auth emulator")

    // Override repository providers
    friendsRepo = FriendsRepositoryFirestore(db, auth)
    FriendsRepositoryProvider.repository = friendsRepo

    friendRequestsRepo = FriendRequestsRepositoryFirestore(db, auth, friendsRepo)
    FriendRequestsRepositoryProvider.repository = friendRequestsRepo
  }

  @After
  override fun tearDown() {
    friendRequestsRepo.cleanup()
    auth.signOut()
    super.tearDown()
  }

  @Test
  fun provider_returns_overridden_repository_instance() {
    val fromProvider = FriendRequestsRepositoryProvider.repository

    assertSame(friendRequestsRepo, fromProvider)
    assertTrue(fromProvider is FriendRequestsRepositoryFirestore)
  }

  @Test
  fun getCurrentUserId_returns_current_user_id() {
    val userId = friendRequestsRepo.getCurrentUserId()

    assertEquals(currentUserId, userId)
  }

  @Test
  fun myRequests_returns_empty_list_initially() = runTest {
    friendRequestsRepo.myRequests().test {
      val requests = awaitItem()

      assertTrue("Expected no requests initially", requests.isEmpty())
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun askFriend_creates_request_in_both_users_subcollections() = runTest {
    val targetUserId = "friend-123"

    friendRequestsRepo.askFriend(targetUserId)

    // Verify in sender's subcollection
    val senderSnapshot =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .whereEqualTo("toUserId", targetUserId)
            .get()
            .await()

    assertFalse(senderSnapshot.isEmpty)
    assertEquals(targetUserId, senderSnapshot.documents[0].getString("toUserId"))
    assertEquals(currentUserId, senderSnapshot.documents[0].getString("fromUserId"))
    assertEquals("PENDING", senderSnapshot.documents[0].getString("status"))

    // Verify in receiver's subcollection
    val receiverSnapshot =
        db.collection("users")
            .document(targetUserId)
            .collection("friend_requests")
            .whereEqualTo("fromUserId", currentUserId)
            .get()
            .await()

    assertFalse(receiverSnapshot.isEmpty)
    assertEquals(currentUserId, receiverSnapshot.documents[0].getString("fromUserId"))
    assertEquals(targetUserId, receiverSnapshot.documents[0].getString("toUserId"))
  }

  @Test
  fun askFriend_throws_when_sending_to_self() = runTest {
    val error = runCatching { friendRequestsRepo.askFriend(currentUserId) }.exceptionOrNull()

    assertNotNull(error)
    assertTrue(error is IllegalArgumentException)
    assertTrue(error!!.message!!.contains("Cannot send friend request to yourself"))
  }

  @Test
  fun askFriend_does_not_duplicate_existing_request() = runTest {
    val targetUserId = "friend-456"

    // Send once
    friendRequestsRepo.askFriend(targetUserId)

    // Try to send again
    friendRequestsRepo.askFriend(targetUserId)

    // Verify only one request exists
    val snapshot =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .whereEqualTo("toUserId", targetUserId)
            .get()
            .await()

    assertEquals(1, snapshot.size())
  }

  @Test
  fun incomingRequests_returns_only_requests_where_current_user_is_receiver() = runTest {
    val otherUserId = "other-user"

    // Create a request FROM otherUser TO currentUser
    val requestData =
        mapOf(
            "fromUserId" to otherUserId,
            "toUserId" to currentUserId,
            "status" to "PENDING",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "seenByReceiver" to false)

    db.collection("users")
        .document(currentUserId)
        .collection("friend_requests")
        .add(requestData)
        .await()

    friendRequestsRepo.incomingRequests().test {
      val requests = awaitItem()

      assertEquals(1, requests.size)
      assertEquals(otherUserId, requests[0].fromUserId)
      assertEquals(currentUserId, requests[0].toUserId)
      assertEquals(FriendRequestStatus.PENDING, requests[0].status)
      assertEquals(false, requests[0].seenByReceiver)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun outgoingRequests_returns_only_requests_where_current_user_is_sender() = runTest {
    val targetUserId = "target-user"

    friendRequestsRepo.askFriend(targetUserId)

    friendRequestsRepo.outgoingRequests().test {
      val requests = awaitItem()

      assertEquals(1, requests.size)
      assertEquals(currentUserId, requests[0].fromUserId)
      assertEquals(targetUserId, requests[0].toUserId)
      assertEquals(FriendRequestStatus.PENDING, requests[0].status)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun acceptRequest_updates_status_and_adds_friend() = runTest {
    val senderUserId = "sender-user"

    // Create a request FROM sender TO currentUser
    val requestData =
        mapOf(
            "fromUserId" to senderUserId,
            "toUserId" to currentUserId,
            "status" to "PENDING",
            "createdAt" to com.google.firebase.Timestamp.now())

    val docRef =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .add(requestData)
            .await()

    // Also add to sender's subcollection (as the real implementation does)
    db.collection("users")
        .document(senderUserId)
        .collection("friend_requests")
        .document(docRef.id)
        .set(requestData)
        .await()

    // Accept the request
    friendRequestsRepo.acceptRequest(docRef.id)

    // Verify status changed to ACCEPTED
    val updatedDoc =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .document(docRef.id)
            .get()
            .await()

    assertEquals("ACCEPTED", updatedDoc.getString("status"))

    // Verify friend was added
    val friendDoc =
        db.collection("users")
            .document(currentUserId)
            .collection("friends")
            .document(senderUserId)
            .get()
            .await()

    assertTrue(friendDoc.exists())
    assertEquals(senderUserId, friendDoc.getString("friendUid"))
  }

  @Test
  fun acceptRequest_throws_when_request_not_found() = runTest {
    val error =
        runCatching { friendRequestsRepo.acceptRequest("non-existent-id") }.exceptionOrNull()

    assertNotNull(error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Friend request not found"))
  }

  @Test
  fun acceptRequest_throws_when_current_user_is_not_receiver() = runTest {
    val targetUserId = "target-user"

    // Create a request FROM currentUser TO target (currentUser is sender, not receiver)
    val requestData =
        mapOf(
            "fromUserId" to currentUserId,
            "toUserId" to targetUserId,
            "status" to "PENDING",
            "createdAt" to com.google.firebase.Timestamp.now())

    val docRef =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .add(requestData)
            .await()

    val error = runCatching { friendRequestsRepo.acceptRequest(docRef.id) }.exceptionOrNull()

    assertNotNull(error)
    assertTrue(error is IllegalArgumentException)
    assertTrue(error!!.message!!.contains("Only the recipient can"))
  }

  @Test
  fun refuseRequest_deletes_request_from_both_users() = runTest {
    val senderUserId = "sender-user"

    // Create a request FROM sender TO currentUser
    val requestData =
        mapOf(
            "fromUserId" to senderUserId,
            "toUserId" to currentUserId,
            "status" to "PENDING",
            "createdAt" to com.google.firebase.Timestamp.now())

    val docRef =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .add(requestData)
            .await()

    // Also add to sender's subcollection
    db.collection("users")
        .document(senderUserId)
        .collection("friend_requests")
        .document(docRef.id)
        .set(requestData)
        .await()

    // Refuse (delete) the request
    friendRequestsRepo.refuseRequest(docRef.id)

    // Verify the request has been deleted for the receiver
    val receiverDoc =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .document(docRef.id)
            .get()
            .await()

    // Verify the request has been deleted for the sender
    val senderDoc =
        db.collection("users")
            .document(senderUserId)
            .collection("friend_requests")
            .document(docRef.id)
            .get()
            .await()

    assertFalse(receiverDoc.exists())
    assertFalse(senderDoc.exists())
  }

  @Test
  fun refuseRequest_throws_when_request_not_found() = runTest {
    val error =
        runCatching { friendRequestsRepo.refuseRequest("non-existent-id") }.exceptionOrNull()

    assertNotNull(error)
    assertTrue(error is IllegalStateException)
    assertTrue(error!!.message!!.contains("Friend request not found"))
  }

  @Test
  fun myRequests_updates_in_real_time() = runTest {
    friendRequestsRepo.myRequests().test {
      // First emission: empty
      assertEquals(emptyList<FriendRequest>(), awaitItem())

      // Send a request
      friendRequestsRepo.askFriend("friend-789")

      // Should receive update
      val requests = awaitItem()
      assertEquals(1, requests.size)
      assertEquals("friend-789", requests[0].toUserId)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun cleanup_removes_all_listeners() {
    // Simply verify cleanup doesn't crash
    // The real test is in tearDown where cleanup is always called
    friendRequestsRepo.cleanup()
  }

  @Test
  fun markRequestAsSeen_updates_seen_flag() = runTest {
    val senderUserId = "someone"

    val requestData =
        mapOf(
            "fromUserId" to senderUserId,
            "toUserId" to currentUserId,
            "status" to "PENDING",
            "createdAt" to com.google.firebase.Timestamp.now(),
            "seenByReceiver" to false)

    val docRef =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .add(requestData)
            .await()

    friendRequestsRepo.markRequestAsSeen(docRef.id)

    val updatedDoc =
        db.collection("users")
            .document(currentUserId)
            .collection("friend_requests")
            .document(docRef.id)
            .get()
            .await()

    assertEquals(true, updatedDoc.getBoolean("seenByReceiver"))
  }
    @Test
    fun isInOutgoingRequests_returnsTrue_afterAskFriend() = runTest {
        val targetUserId = "target-user"

        friendRequestsRepo.askFriend(targetUserId)

        val result = friendRequestsRepo.isInOutgoingRequests(targetUserId)
        assertTrue(result)

        val other = friendRequestsRepo.isInOutgoingRequests("someone-else")
        assertFalse(other)
    }

    private suspend fun createIncomingRequest(fromUserId: String, toUserId: String, requestId: String) {
        val data =
            mapOf(
                "fromUserId" to fromUserId,
                "toUserId" to toUserId,
                "status" to "PENDING",
                "createdAt" to com.google.firebase.Timestamp.now(),
                "seenByReceiver" to false,
            )
        db.collection("users")
            .document(toUserId)
            .collection("friend_requests")
            .document(requestId)
            .set(data)
            .await()

        db.collection("users")
            .document(fromUserId)
            .collection("friend_requests")
            .document(requestId)
            .set(data)
            .await()
    }

    @Test
    fun isInIncomingRequests_returnsTrue_whenPendingIncomingExists() = runTest {
        val otherUserId = "other-user"
        val requestId = "req-1"

        createIncomingRequest(fromUserId = otherUserId, toUserId = currentUserId, requestId = requestId)

        val result = friendRequestsRepo.isInIncomingRequests(otherUserId)
        assertTrue(result)

        val other = friendRequestsRepo.isInIncomingRequests("someone-else")
        assertFalse(other)
    }

    @Test
    fun askFriend_whenIncomingPendingRequest_exists_acceptsAndCreatesFriendship() = runTest {
        val otherUserId = "other-user"
        val requestId = "req-mutual"

        createIncomingRequest(fromUserId = otherUserId, toUserId = currentUserId, requestId = requestId)
        assertTrue(friendRequestsRepo.isInIncomingRequests(otherUserId))

        friendRequestsRepo.askFriend(otherUserId)
        assertTrue(friendsRepo.isFriend(otherUserId))
        assertFalse(friendRequestsRepo.isInIncomingRequests(otherUserId))
    }
}
