package com.android.mygarden.model.achievements

import app.cash.turbine.test
import com.android.mygarden.utils.FirestoreProfileTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AchievementsRepositoryFirestoreTests : FirestoreProfileTest() {

  private lateinit var repository: AchievementsRepository
  private lateinit var userId: String

  @Before
  fun setup() = runTest {
    // Start up Firebase emulator, clear data, etc. (handled by FirestoreProfileTest)
    super.setUp()

    // Inject PlantsRepositoryFirestore and initialize it
    repository = AchievementsRepositoryFirestore(db, auth)
    userId = "user_1"
    repository.initializeAchievementsForNewUser(userId)
  }

  @Test
  fun getCurrentUserId_returnsSignedInUser() = runTest {
    val authUser = auth.currentUser
    assertNotNull(authUser)

    val uidFromRepo = repository.getCurrentUserId()
    assertEquals(authUser!!.uid, uidFromRepo)
  }

  @Test
  fun setAndGetUserAchievementProgressWorksFine() = runTest {
    val type = AchievementType.PLANTS_NUMBER
    val value = 7

    repository.setAchievementValue(userId, type, value)

    val progress = repository.getUserAchievementProgress(userId, type)

    assertNotNull(progress)
    assertEquals(type, progress!!.achievementType)
    assertEquals(value, progress.currentValue)
  }

  @Test
  fun getUserAchievementProgress_returnsNullWhenNoValue() = runTest {
    // Ensure no document exists
    db.collection("users")
        .document(userId)
        .collection("achievements")
        .document(AchievementType.PLANTS_NUMBER.name)
        .delete()
        .await()

    val progress = repository.getUserAchievementProgress(userId, AchievementType.PLANTS_NUMBER)
    assertNull(progress)
  }

  @Test
  fun initializeAchievementsForNewUser_actuallySetsTheRightValues() = runTest {
    for (type in AchievementType.entries) {
      assertEquals(
          repository.getUserAchievementProgress(userId, type), UserAchievementProgress(type, 0))
    }
  }

  @Test
  fun updateAchievementValue_onlyUpdatesWhenNewValueIsGreater() = runTest {
    val type = AchievementType.HEALTHY_STREAK
    val docRef =
        db.collection("users").document(userId).collection("achievements").document(type.name)

    // Seed current value = 10
    docRef.set(mapOf("value" to 10), com.google.firebase.firestore.SetOptions.merge()).await()

    repository.updateAchievementValue(userId, type, 12)

    docRef.get().await()
    assertEquals(12, repository.getUserAchievementProgress(userId, type)!!.currentValue)

    repository.updateAchievementValue(userId, type, 11)

    docRef.get().await()
    assertEquals(12, repository.getUserAchievementProgress(userId, type)!!.currentValue)
  }

  @Test
  fun getAllUserAchievementProgress_emitsOnChanges() = runTest {
    val type = AchievementType.PLANTS_NUMBER

    val flow = repository.getAllUserAchievementProgress(userId)

    flow.test {
      val initial = awaitItem() // init not used but needs to stay here

      repository.setAchievementValue(userId, type, 5)

      val first = awaitItem()
      val newVal = first.first { it.achievementType == type }
      assertEquals(newVal.currentValue, 5)

      repository.setAchievementValue(userId, type, 10)

      val second = awaitItem()
      val newVal1 = second.first { it.achievementType == type }
      assertEquals(newVal1.currentValue, 10)
    }
  }

  @Test
  fun cleanup_doesNotThrowException() = runTest {
    // Call cleanup - should not throw
    repository.cleanup()
  }
}
