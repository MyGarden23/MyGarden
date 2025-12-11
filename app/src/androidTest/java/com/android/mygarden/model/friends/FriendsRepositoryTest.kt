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
 * Integration tests for the friends feature, covering:
 * - [FriendsRepositoryFirestore] behavior on top of Firestore emulator,
 * - [FriendsRepositoryProvider] override mechanism,
 * - [FriendsRepository] contract (getFriends / addFriend).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FriendsRepositoryTest : FirestoreProfileTest() {

  private lateinit var friendsRepo: FriendsRepository
  private lateinit var currentUserId: String

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  /**
   * Starts Firestore emulator (via [FirestoreProfileTest]), signs in a test user on the Auth
   * emulator, and wires the [FriendsRepositoryProvider] to use a Firestore-backed implementation.
   */
  @Before
  override fun setUp() = runTest {
    super.setUp()

    compose.setContent { MyGardenTheme {} }

    auth = FirebaseAuth.getInstance()

    // Sign in anonymously
    val result = auth.signInAnonymously().await()
    currentUserId = result.user?.uid ?: error("Failed to sign in anonymously on Auth emulator")

    // Override repository provider
    FriendsRepositoryProvider.repository = FriendsRepositoryFirestore(db, auth)
    friendsRepo = FriendsRepositoryProvider.repository
  }

  @After
  override fun tearDown() {
    auth.signOut()
    super.tearDown()
  }

  @Test
  fun provider_returns_overridden_repository_instance() {
    val fromProvider = FriendsRepositoryProvider.repository

    assertSame(friendsRepo, fromProvider)
    assertTrue(fromProvider is FriendsRepositoryFirestore)
  }

  @Test
  fun getFriends_returns_empty_list_when_user_has_no_friends() = runTest {
    val friends = friendsRepo.getFriends(currentUserId)

    assertTrue("Expected no friends initially", friends.isEmpty())
  }

  @Test
  fun addFriend_adds_friend_to_current_user_subcollection() = runTest {
    val friendUid = "friend-123"

    friendsRepo.addFriend(friendUid)

    val snapshot =
        db.collection("users")
            .document(currentUserId)
            .collection("friends")
            .document(friendUid)
            .get()
            .await()

    assertTrue(snapshot.exists())
    assertEquals(friendUid, snapshot.getString("friendUid"))
  }

  @Test
  fun getFriends_returns_all_friends_added_for_current_user() = runTest {
    val friend1 = "friend-1"
    val friend2 = "friend-2"

    friendsRepo.addFriend(friend1)
    friendsRepo.addFriend(friend2)

    val friends = friendsRepo.getFriends(currentUserId)

    assertTrue(friends.contains(friend1))
    assertTrue(friends.contains(friend2))
    assertEquals(2, friends.size)
  }

  @Test
  fun addFriend_throws_when_adding_self() = runTest {
    val error = runCatching { friendsRepo.addFriend(currentUserId) }.exceptionOrNull()

    assertNotNull(error)
    assertTrue(error is IllegalArgumentException)
    assertEquals("You cannot add yourself as a friend.", error!!.message)
  }

  @Test
  fun addFriend_throws_when_user_not_authenticated() = runTest {
    // Sign out to simulate an unauthenticated state
    auth.signOut()

    val error = runCatching { friendsRepo.addFriend("any-friend") }.exceptionOrNull()

    assertNotNull(error)
    assertTrue(error is IllegalStateException)
    assertEquals("User not authenticated", error!!.message)
  }

  @Test
  fun addFriend_updatesFlow() = runTest {
    friendsRepo.friendsFlow(currentUserId).test {
      // first emission always empty
      assertEquals(emptyList<String>(), awaitItem())

      friendsRepo.addFriend("friend1")
      assertEquals(listOf("friend1"), awaitItem())

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun deleteFriendErasesCorrectFriendFromFriendList() = runTest {
    val friend1 = "test-friend-1"
    val friend2 = "test-friend-2"

    friendsRepo.addFriend(friend1)
    friendsRepo.addFriend(friend2)

    val friends = friendsRepo.getFriends(currentUserId)
    assertEquals(2, friends.size)

    friendsRepo.deleteFriend(friend1)

    val updatedFriends = friendsRepo.getFriends(currentUserId)

    assertEquals(1, updatedFriends.size)
    assertFalse(updatedFriends.contains(friend1))
    assertTrue(updatedFriends.contains(friend2))
  }

  @Test
  fun deleteFriendKeepsSameListWhenFirestoreCallFails() = runTest {
    val friend1 = "test-friend-1"
    val friend2 = "test-friend-2"

    friendsRepo.addFriend(friend1)
    friendsRepo.addFriend(friend2)

    val friends = friendsRepo.getFriends(currentUserId)
    assertEquals(2, friends.size)

    friendsRepo.deleteFriend("false-uid")

    val updatedFriends = friendsRepo.getFriends(currentUserId)

    assertEquals(2, updatedFriends.size)
    assertTrue(updatedFriends.contains(friend1))
    assertTrue(updatedFriends.contains(friend2))
  }
}
