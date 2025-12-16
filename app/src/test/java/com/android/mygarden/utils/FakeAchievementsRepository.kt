package com.android.mygarden.utils

import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.UserAchievementProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * This fake repository is used in many tests to make sure there is no interaction with Firestore
 * done in certain tests. I can override the AchievementsRepositoryProvider
 */
class FakeAchievementsRepository : AchievementsRepository {

  override fun getCurrentUserId(): String = "fake-uid"

  val addedAchievements = mutableListOf<UserAchievementProgress>()
  private val _flow = MutableStateFlow<List<UserAchievementProgress>>(emptyList())

  override suspend fun getUserAchievementProgress(
      userId: String,
      achievementType: AchievementType
  ): UserAchievementProgress? {
    return addedAchievements.firstOrNull { it.achievementType == achievementType }
  }

  override fun getAllUserAchievementProgress(userId: String): Flow<List<UserAchievementProgress>> {
    return _flow
  }

  override suspend fun setAchievementValue(
      userId: String,
      achievementType: AchievementType,
      value: Int
  ) {
    val index = addedAchievements.indexOfFirst { it.achievementType == achievementType }
    if (index != -1) {
      val v = addedAchievements[index]
      val newVal = v.copy(currentValue = value)
      addedAchievements[index] = newVal
    } else {
      addedAchievements.add(UserAchievementProgress(achievementType, value))
    }
    _flow.value = addedAchievements.toList()
  }

  override suspend fun initializeAchievementsForNewUser(userId: String) {
    addedAchievements.clear()
    for (type in AchievementType.entries) {
      addedAchievements.add(UserAchievementProgress(type, 0))
    }
    _flow.value = addedAchievements.toList()
  }

  override suspend fun updateAchievementValue(
      userId: String,
      achievementType: AchievementType,
      newValue: Int
  ) {
    val get = getUserAchievementProgress(userId, achievementType)
    if (get != null) {
      // Achievement exists, update if new value is higher
      if (get.currentValue < newValue) {
        setAchievementValue(userId, achievementType, newValue)
      }
    } else {
      // Achievement doesn't exist yet, create it with the new value
      setAchievementValue(userId, achievementType, newValue)
    }
  }

  override fun cleanup() {
    // No-op for fake repository - no real listeners to clean up
  }
}
