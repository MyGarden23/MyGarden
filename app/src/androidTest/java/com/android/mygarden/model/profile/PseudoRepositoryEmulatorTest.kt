package com.android.mygarden.model.profile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FirestoreProfileTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class PseudoRepositoryEmulatorTest : FirestoreProfileTest() {

  private lateinit var pseudoRepo: PseudoRepository

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  /**
   * Starts up everything needed for Firestore (handled by FirestoreProfileTest) & sets up the
   * repository
   */
  @Before
  override fun setUp() {
    super.setUp()
    compose.setContent { MyGardenTheme {} }

    pseudoRepo = PseudoRepositoryFirestore(db)
  }

  @Test
  fun pseudo_is_available_when_not_saved() = runTest {
    val available = pseudoRepo.isPseudoAvailable("Ada")
    assertTrue(available)
  }

  @Test
  fun savePseudo_makes_it_unavailable() = runTest {
    val pseudo = "lovelace"

    assertTrue(pseudoRepo.isPseudoAvailable(pseudo))

    pseudoRepo.savePseudo(pseudo, "uid")

    assertFalse(pseudoRepo.isPseudoAvailable(pseudo))
  }

  @Test
  fun savePseudo_is_case_insensitive() = runTest {
    pseudoRepo.savePseudo("Turing", "uid")

    // Should normalize to lowercase and detect collision
    val available = pseudoRepo.isPseudoAvailable("tUrInG")
    assertFalse(available)
  }

  @Test
  fun savePseudo_throws_if_already_taken() = runTest {
    pseudoRepo.savePseudo("hopper", "uid")

    val error =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { pseudoRepo.savePseudo("HoPpEr", "uid") }
        }

    assertEquals("Pseudo already taken", error.message)
  }

  @Test
  fun deletePseudo_makes_it_available_again() = runTest {
    val pseudo = "grace"

    pseudoRepo.savePseudo(pseudo, "uid")
    assertFalse(pseudoRepo.isPseudoAvailable(pseudo))

    pseudoRepo.deletePseudo(pseudo)
    assertTrue(pseudoRepo.isPseudoAvailable(pseudo))
  }

  @Test
  fun pseudoNormalization_lowercases_document_id() = runTest {
    pseudoRepo.savePseudo("CamelCasePseudo", "uid")
    val snapshot = db.collection("pseudos").document("camelcasepseudo").get().await()

    assertTrue(snapshot.exists())
  }

  @Test
  fun getUidFromPseudo_returns_null_when_pseudo_does_not_exist() = runTest {
    val uid = pseudoRepo.getUidFromPseudo("unknownPseudo")
    assertNull(uid)
  }

  @Test
  fun getUidFromPseudo_returns_uid_for_existing_pseudo_case_insensitive() = runTest {
    val pseudo = "SomeUser"
    val expectedUid = "uid-123"

    pseudoRepo.savePseudo(pseudo, expectedUid)

    // Lookup with different casing should still work
    val result = pseudoRepo.getUidFromPseudo("sOmEuSeR")

    assertEquals(expectedUid, result)
  }

  @Test
  fun searchPseudoStartingWith_returns_matching_pseudos() = runTest {
    // Given: multiple pseudos saved in the repository
    pseudoRepo.savePseudo("alice", "uid-alice")
    pseudoRepo.savePseudo("alex", "uid-alex")
    pseudoRepo.savePseudo("bob", "uid-bob")
    pseudoRepo.savePseudo("albert", "uid-albert")

    // When: searching for pseudos starting with "al"
    val results = pseudoRepo.searchPseudoStartingWith("al")

    // Then: we should find all the "al*" pseudos, and none of the others
    assertTrue(results.contains("alice"))
    assertTrue(results.contains("alex"))
    assertTrue(results.contains("albert"))
    assertFalse(results.contains("bob"))
  }

  @Test
  fun searchPseudoStartingWith_is_case_insensitive_on_query() = runTest {
    pseudoRepo.savePseudo("charlie", "uid-charlie")
    pseudoRepo.savePseudo("Charlotte", "uid-charlotte")

    // Implementation lowercases both document ids and query,
    // so searching with weird casing should still find them.
    val results = pseudoRepo.searchPseudoStartingWith("Ch")

    // All pseudos are normalized to lowercase in Firestore
    assertTrue(results.contains("charlie"))
    assertTrue(results.contains("charlotte"))
  }
}
