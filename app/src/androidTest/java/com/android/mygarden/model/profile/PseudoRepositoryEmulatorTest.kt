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

    pseudoRepo.savePseudo(pseudo)

    assertFalse(pseudoRepo.isPseudoAvailable(pseudo))
  }

  @Test
  fun savePseudo_is_case_insensitive() = runTest {
    pseudoRepo.savePseudo("Turing")

    // Should normalize to lowercase and detect collision
    val available = pseudoRepo.isPseudoAvailable("tUrInG")
    assertFalse(available)
  }

  @Test
  fun savePseudo_throws_if_already_taken() = runTest {
    pseudoRepo.savePseudo("hopper")

    val error =
        assertThrows(IllegalStateException::class.java) {
          runBlocking { pseudoRepo.savePseudo("HoPpEr") }
        }

    assertEquals("Pseudo already taken", error.message)
  }

  @Test
  fun deletePseudo_makes_it_available_again() = runTest {
    val pseudo = "grace"

    pseudoRepo.savePseudo(pseudo)
    assertFalse(pseudoRepo.isPseudoAvailable(pseudo))

    pseudoRepo.deletePseudo(pseudo)
    assertTrue(pseudoRepo.isPseudoAvailable(pseudo))
  }

  @Test
  fun pseudoNormalization_lowercases_document_id() = runTest {
    pseudoRepo.savePseudo("CamelCasePseudo")
    val snapshot = db.collection("pseudos").document("camelcasepseudo").get().await()

    assertTrue(snapshot.exists())
  }
}
