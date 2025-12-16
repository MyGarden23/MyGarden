package com.android.mygarden.model.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FirestoreProfileTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

// Runs the tests on an Android emulator
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfileRepositoryEmulatorTest : FirestoreProfileTest() {

  // Needed to launch a Compose context (for proper Android environment)
  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Before
  override fun setUp() {
    // Start up Firebase emulator, clear data, etc. (handled by FirestoreProfileTest)
    super.setUp()

    // Just sets up a Compose theme — no actual UI content here
    compose.setContent { MyGardenTheme {} }
  }

  // --- HELPER FUNCTIONS ---

  /** Suspend helper: Creates and saves a profile (for use inside runTest blocks) */
  private suspend fun saveProfileSuspend(
      firstName: String = "Test",
      lastName: String = "User",
      pseudo: String = "pseudo",
      gardeningSkill: GardeningSkill = GardeningSkill.BEGINNER,
      favoritePlant: String = "Rose",
      country: String = "US",
      hasSignedIn: Boolean = false
  ): Profile {
    val profile =
        Profile(
            firstName = firstName,
            lastName = lastName,
            pseudo = pseudo,
            gardeningSkill = gardeningSkill,
            favoritePlant = favoritePlant,
            country = country,
            hasSignedIn = hasSignedIn)
    repo.saveProfile(profile)
    return profile
  }

  // --- TESTS START HERE ---

  @Test
  fun canSaveAndRetrieveProfile() = runTest {
    // Create a fake profile and save it to Firestore (via emulator)
    val profile =
        Profile(
            firstName = "Ada",
            lastName = "Lovelace",
            gardeningSkill = GardeningSkill.BEGINNER,
            favoritePlant = "Rose",
            country = "UK",
            hasSignedIn = false)

    repo.saveProfile(profile)

    // Fetch the saved profile from Firestore and check that fields match
    val fetched = repo.getProfile().first()
    assertNotNull(fetched)
    assertEquals("Ada", fetched!!.firstName)
    assertEquals("Lovelace", fetched.lastName)
    assertEquals(GardeningSkill.BEGINNER, fetched.gardeningSkill)
    assertEquals("Rose", fetched.favoritePlant)
    assertFalse(fetched.hasSignedIn)
  }

  @Test
  fun saveProfile_mergesWithoutOverwriting() = runTest {
    // Save an initial profile
    val initial =
        Profile(
            firstName = "Alan",
            lastName = "Turing",
            gardeningSkill = GardeningSkill.INTERMEDIATE,
            favoritePlant = "Tulip",
            country = "UK",
            hasSignedIn = false)
    repo.saveProfile(initial)

    // Update only a few fields (should merge instead of overwrite everything)
    val update = initial.copy(favoritePlant = "Rose", hasSignedIn = true)
    repo.saveProfile(update)

    // Check that the updated fields changed, and the others stayed the same
    val latest = repo.getProfile().first()
    assertNotNull(latest)
    assertEquals("Rose", latest!!.favoritePlant)
    assertTrue(latest.hasSignedIn)
    assertEquals("Alan", latest.firstName)
    assertEquals("Turing", latest.lastName)
    assertEquals(GardeningSkill.INTERMEDIATE, latest.gardeningSkill)
  }

  @Test
  fun hasSignedInFlagTogglesAndPersists() = runTest {
    // Save a profile with hasSignedIn = false
    val base =
        Profile(
            firstName = "Grace",
            lastName = "Hopper",
            gardeningSkill = GardeningSkill.ADVANCED,
            favoritePlant = "Orchid",
            country = "US",
            hasSignedIn = false)
    repo.saveProfile(base)

    // Verify it starts as false
    var cur = repo.getProfile().first()
    assertFalse(cur!!.hasSignedIn)

    // Update to true and check persistence
    repo.saveProfile(cur.copy(hasSignedIn = true))
    cur = repo.getProfile().first()
    assertTrue(cur!!.hasSignedIn)
  }

  @Test
  fun getCurrentUserId_isNotNull() = runTest {
    // Check that the Firebase emulator returns a valid user ID
    val uid = repo.getCurrentUserId()
    assertNotNull(uid)
    assertTrue(uid!!.isNotBlank())
  }

  // Notification test with the profile repository

  @Test
  fun attachFCMToken_attaches_token_when_logged_in() = runTest {
    val profile =
        Profile(
            firstName = "Ada",
            lastName = "Lovelace",
            gardeningSkill = GardeningSkill.BEGINNER,
            favoritePlant = "Rose",
            country = "UK",
            hasSignedIn = false)
    repo.saveProfile(profile)

    // User should be logged in hence the token should attach
    val res = repo.attachFCMToken("fake-token")
    assertTrue(res)
    val token = repo.getFCMToken()
    assertTrue(token != null && token == "fake-token")
  }

  @Test
  fun attachFCMToken_attach_when_not_logged_in() = runTest {
    // No profile corresponding
    val res = repo.attachFCMToken("fake-token")
    assertTrue(res)
    val token = repo.getFCMToken()
    assertTrue(token != null && token == "fake-token")
  }

  @Test
  fun getFCMToken_fail_when_no_token_attached() = runTest {
    // No profile corresponding
    val token = repo.getFCMToken()
    assertTrue(token == null)
  }

  @Test
  fun cleanup_doesNotThrowException() = runTest {
    saveProfileSuspend()

    // Call cleanup - should not throw
    repo.cleanup()
  }

  @Test
  fun cleanup_canBeCalledMultipleTimes() = runTest {
    saveProfileSuspend(firstName = "Multi")

    // Call cleanup multiple times - should not throw exceptions
    repo.cleanup()
    repo.cleanup()
    repo.cleanup()

    // Verify the repository is still functional
    val fetched = repo.getProfile().first()
    assertNotNull(fetched)
    assertEquals("Multi", fetched!!.firstName)
  }

  @Test
  fun cleanup_repositoryStillFunctional() = runTest {
    val profile = saveProfileSuspend(firstName = "Functional")

    // Call cleanup
    repo.cleanup()

    // Verify we can still get the profile
    val fetched = repo.getProfile().first()
    assertNotNull(fetched)
    assertEquals("Functional", fetched!!.firstName)

    // Verify we can still save updates
    val updated = profile.copy(favoritePlant = "Lily")
    repo.saveProfile(updated)
    val fetchedAfterUpdate = repo.getProfile().first()
    assertEquals("Lily", fetchedAfterUpdate!!.favoritePlant)
  }

  @Test
  fun cleanup_doesNotDeleteData() = runTest {
    saveProfileSuspend(firstName = "Persistent", hasSignedIn = true)

    // Verify profile exists before cleanup
    val before = repo.getProfile().first()
    assertNotNull(before)
    assertEquals("Persistent", before!!.firstName)
    assertTrue(before.hasSignedIn)

    // Call cleanup
    repo.cleanup()

    // Verify profile still exists after cleanup
    val after = repo.getProfile().first()
    assertNotNull(after)
    assertEquals("Persistent", after!!.firstName)
    assertTrue(after.hasSignedIn)
  }

  @Test
  fun isCurrentUserPseudo_returnsTrue_whenPseudoMatches() = runTest {
    // Save a profile with a specific pseudo
    saveProfileSuspend(pseudo = "testUser123")

    // Check that the pseudo matches
    val result = repo.isCurrentUserPseudo("testUser123")
    assertTrue(result)
  }

  @Test
  fun isCurrentUserPseudo_returnsFalse_whenPseudoDoesNotMatch() = runTest {
    // Save a profile with a specific pseudo
    saveProfileSuspend(pseudo = "testUser123")

    // Check with a different pseudo
    val result = repo.isCurrentUserPseudo("differentUser")
    assertFalse(result)
  }

  @Test
  fun isCurrentUserPseudo_returnsFalse_whenNoProfileExists() = runTest {
    // Don't save any profile

    // Check with any pseudo
    val result = repo.isCurrentUserPseudo("anyUser")
    assertFalse(result)
  }

  @Test
  fun isCurrentUserPseudo_returnsFalse_whenPseudoIsEmpty() = runTest {
    // Save a profile with a non-empty pseudo
    saveProfileSuspend(pseudo = "testUser123")

    // Check with empty string
    val result = repo.isCurrentUserPseudo("")
    assertFalse(result)
  }
}
