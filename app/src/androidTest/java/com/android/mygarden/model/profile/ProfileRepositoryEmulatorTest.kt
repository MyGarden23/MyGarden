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
}
