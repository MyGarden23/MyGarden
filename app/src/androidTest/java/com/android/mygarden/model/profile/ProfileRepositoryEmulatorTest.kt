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

@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfileRepositoryEmulatorTest : FirestoreProfileTest() {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  @Before
  override fun setUp() {
    super.setUp()
    compose.setContent { MyGardenTheme {} }
  }

  @Test
  fun canSaveAndRetrieveProfile() = runTest {
    val profile =
        Profile(
            firstName = "Ada",
            lastName = "Lovelace",
            gardeningSkill = GardeningSkill.BEGINNER,
            favoritePlant = "Rose",
            country = "UK",
            hasSignedIn = false)

    repo.saveProfile(profile)

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
    val initial =
        Profile(
            firstName = "Alan",
            lastName = "Turing",
            gardeningSkill = GardeningSkill.INTERMEDIATE,
            favoritePlant = "Tulip",
            country = "UK",
            hasSignedIn = false)
    repo.saveProfile(initial)

    val update = initial.copy(favoritePlant = "Rose", hasSignedIn = true)
    repo.saveProfile(update)

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
    val base =
        Profile(
            firstName = "Grace",
            lastName = "Hopper",
            gardeningSkill = GardeningSkill.ADVANCED,
            favoritePlant = "Orchid",
            country = "US",
            hasSignedIn = false)
    repo.saveProfile(base)
    var cur = repo.getProfile().first()
    assertFalse(cur!!.hasSignedIn)

    repo.saveProfile(cur.copy(hasSignedIn = true))
    cur = repo.getProfile().first()
    assertTrue(cur!!.hasSignedIn)
  }

  @Test
  fun getCurrentUserId_isNotNull() = runTest {
    val uid = repo.getCurrentUserId()
    assertNotNull(uid)
    assertTrue(uid!!.isNotBlank())
  }
}
