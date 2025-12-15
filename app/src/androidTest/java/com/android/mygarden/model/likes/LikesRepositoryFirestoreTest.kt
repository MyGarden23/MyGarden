package com.android.mygarden.model.likes

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.profile.LikesRepository
import com.android.mygarden.model.profile.LikesRepositoryFirestore
import com.android.mygarden.utils.FirebaseEmulator
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LikesRepositoryFirestoreTest {

  private lateinit var db: FirebaseFirestore
  private lateinit var repo: LikesRepository

  private val targetUid = "TARGET_USER"
  private val myUid = "LIKER_USER"

  @Before
  fun setUp() = runTest {
    FirebaseEmulator.connectFirestore()
    db = FirebaseFirestore.getInstance()
    repo = LikesRepositoryFirestore(db)

    // Ensure target user doc exists (required because toggleLike uses tx.update)
    db.collection("users").document(targetUid).set(mapOf("likesCount" to 0)).await()

    // Ensure no leftover like doc
    db.collection("users").document(targetUid).collection("likes").document(myUid).delete().await()
  }

  @After
  fun tearDown() = runTest {
    // Clean up
    db.collection("users").document(targetUid).collection("likes").document(myUid).delete().await()
    db.collection("users").document(targetUid).delete().await()
  }

  @Test
  fun observeLikesCount_emitsCurrentCount() = runBlocking {
    db.collection("users").document(targetUid).set(mapOf("likesCount" to 7)).await()

    val count = withTimeout(2_000) { repo.observeLikesCount(targetUid).first() }

    assertEquals(7, count)
  }

  @Test
  fun observeHasLiked_isFalseThenTrueAfterToggle() = runBlocking {
    val before = withTimeout(2_000) { repo.observeHasLiked(targetUid, myUid).first() }
    assertFalse(before)

    repo.toggleLike(targetUid, myUid)

    val after = withTimeout(2_000) { repo.observeHasLiked(targetUid, myUid).first() }
    assertTrue(after)
  }

  @Test
  fun toggleLike_incrementsThenDecrementsLikesCount_andCreatesThenDeletesLikeDoc() = runTest {
    // Like
    repo.toggleLike(targetUid, myUid)

    val likesAfterLike =
        db.collection("users").document(targetUid).get().await().getLong("likesCount") ?: -1L
    assertEquals(1L, likesAfterLike)

    val likeDocExistsAfterLike =
        db.collection("users")
            .document(targetUid)
            .collection("likes")
            .document(myUid)
            .get()
            .await()
            .exists()
    assertTrue(likeDocExistsAfterLike)

    // Unlike (second toggle)
    repo.toggleLike(targetUid, myUid)

    val likesAfterUnlike =
        db.collection("users").document(targetUid).get().await().getLong("likesCount") ?: -1L
    assertEquals(0L, likesAfterUnlike)

    val likeDocExistsAfterUnlike =
        db.collection("users")
            .document(targetUid)
            .collection("likes")
            .document(myUid)
            .get()
            .await()
            .exists()
    assertFalse(likeDocExistsAfterUnlike)
  }
}
