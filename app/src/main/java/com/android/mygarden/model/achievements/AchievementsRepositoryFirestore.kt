package com.android.mygarden.model.achievements

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed implementation of [AchievementsRepository].
 *
 * This repository stores achievements under: `users/{userId}/achievements/{achievementType}`.
 *
 * Each achievement document contains a single `value` field representing the user's progress for
 * the given achievement type.
 */
class AchievementsRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AchievementsRepository {

  companion object {
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_ACHIEVEMENTS = "achievements"
    private const val VALUE_FIELD = "value"
  }

  /** Returns the current authenticated user's UID, or null if not signed in. */
  override fun getCurrentUserId(): String? = auth.currentUser?.uid

  /** Convenience reference to the achievements subcollection of a user. */
  private fun userAchievements(userId: String) =
      db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_ACHIEVEMENTS)

  /** Fetches a single achievement document and converts it to a domain model. */
  override suspend fun getUserAchievementProgress(
      userId: String,
      achievementType: AchievementType
  ): UserAchievementProgress? {
    val snap = userAchievements(userId).document(achievementType.name).get().await()
    val value = snap.getLong(VALUE_FIELD)?.toInt() ?: return null
    return UserAchievementProgress(achievementType, value)
  }

  /**
   * Streams all achievement documents for the user using a snapshot listener. Emits updates
   * whenever Firestore data changes.
   */
  override fun getAllUserAchievementProgress(userId: String): Flow<List<UserAchievementProgress>> =
      callbackFlow {
        val registration =
            userAchievements(userId).addSnapshotListener { snapshot, error ->
              if (error != null) {
                close(error)
                return@addSnapshotListener
              }

              val progresses: List<UserAchievementProgress> =
                  snapshot?.documents?.mapNotNull { doc ->
                    // The document ID is actually the name of the achievement
                    val type =
                        try {
                          AchievementType.valueOf(doc.id)
                        } catch (_: IllegalArgumentException) {
                          // Unknown achievement type -> skip it
                          return@mapNotNull null
                        }

                    val value = doc.getLong(VALUE_FIELD)?.toInt()
                    value?.let { UserAchievementProgress(type, it) }
                  } ?: emptyList()

              trySend(progresses)
            }

        awaitClose { registration.remove() }
      }

  /** Writes or overwrites an achievement's value using merge semantics. */
  override suspend fun setAchievementValue(
      userId: String,
      achievementType: AchievementType,
      value: Int
  ) {
    userAchievements(userId)
        .document(achievementType.name)
        .set(mapOf(VALUE_FIELD to value), SetOptions.merge())
        .await()
  }

  /** Initializes all achievements for a new user by setting their values to zero. */
  override suspend fun initializeAchievementsForNewUser(userId: String) {
    AchievementType.entries.forEach { achievement -> setAchievementValue(userId, achievement, 0) }
  }

  /** Updates an achievement only if the new value represents progress. */
  override suspend fun updateAchievementValue(
      userId: String,
      achievementType: AchievementType,
      newValue: Int
  ) {
    val currentVal = getUserAchievementProgress(userId, achievementType)
    if (newValue > (currentVal?.currentValue ?: return))
        setAchievementValue(userId, achievementType, newValue)
  }
}
