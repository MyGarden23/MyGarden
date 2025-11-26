package com.android.mygarden.model.users

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FirestoreProfileTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Integration tests for the user profile feature, covering:
 * - [UserProfileRepositoryFirestore] on top of the Firestore emulator,
 * - [UserProfileRepositoryProvider] override mechanism,
 * - [UserProfileRepository] contract.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class UserProfileRepositoryTest : FirestoreProfileTest() {

  private lateinit var userProfileRepo: UserProfileRepository

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Before
  override fun setUp() {
    super.setUp()

    compose.setContent { MyGardenTheme {} }

    // Use the emulator-backed Firestore instance from FirestoreProfileTest
    userProfileRepo = UserProfileRepositoryFirestore(db)

    // Override the provider so the whole app (and tests) use this instance
    UserProfileRepositoryProvider.repository = userProfileRepo
  }

  @After
  override fun tearDown() {
    // If you need to clean Firestore between tests, FirestoreProfileTest should handle it.
    super.tearDown()
  }

  @Test
  fun provider_returns_overridden_repository_instance() {
    val fromProvider = UserProfileRepositoryProvider.repository

    assertSame(userProfileRepo, fromProvider)
    assertTrue(fromProvider is UserProfileRepositoryFirestore)
  }

  @Test
  fun getUserProfile_returns_null_when_document_does_not_exist() = runTest {
    val profile = userProfileRepo.getUserProfile("non-existing-user-id")

    assertNull(profile)
  }

  @Test
  fun getUserProfile_returns_correct_profile_when_document_exists() = runTest {
    val userId = "user-123"
    val pseudo = "Matteo"
    val avatarString = "A9"

    // Arrange: create a user document in the emulator
    db.collection("users")
        .document(userId)
        .set(
            mapOf(
                "pseudo" to pseudo,
                "avatar" to avatarString,
            ))
        .await()

    // Act
    val profile = userProfileRepo.getUserProfile(userId)

    // Assert
    assertNotNull(profile)
    profile!!
    assertEquals(userId, profile.id)
    assertEquals(pseudo, profile.pseudo)
    assertEquals(Avatar.A9, profile.avatar)
  }

  @Test
  fun getUserProfile_uses_default_avatar_when_invalid_avatar_string() = runTest {
    val userId = "user-invalid-avatar"
    val pseudo = "BrokenAvatarUser"

    // Store an invalid avatar value that does not exist in the enum
    db.collection("users")
        .document(userId)
        .set(mapOf("pseudo" to pseudo, "avatar" to "THIS_IS_NOT_A_VALID_AVATAR"))
        .await()

    val profile = userProfileRepo.getUserProfile(userId)

    assertNotNull(profile)
    profile!!
    assertEquals(userId, profile.id)
    assertEquals(pseudo, profile.pseudo)
    // Assuming your implementation falls back to Avatar.A1
    assertEquals(Avatar.A1, profile.avatar)
  }

  @Test
  fun getUserProfile_returns_null_when_pseudo_missing() = runTest {
    val userId = "user-no-pseudo"

    // Create a document without "pseudo" field
    db.collection("users").document(userId).set(mapOf("avatar" to "A1")).await()

    val profile = userProfileRepo.getUserProfile(userId)

    // According to our design, we return null if pseudo is missing
    assertNull(profile)
  }
}
