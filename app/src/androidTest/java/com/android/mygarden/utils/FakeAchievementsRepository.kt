package com.android.mygarden.utils

import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.model.achievements.AchievementsRepository
import com.android.mygarden.model.achievements.UserAchievementProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeAchievementsRepository : AchievementsRepository {

  override fun getCurrentUserId(): String = "fake-uid"

  val addedAchievements = mutableListOf<UserAchievementProgress>()

  override suspend fun getUserAchievementProgress(
      userId: String,
      achievementType: AchievementType
  ): UserAchievementProgress? {
    return addedAchievements.first { it.achievementType == achievementType }
  }

  override fun getAllUserAchievementProgress(userId: String): Flow<List<UserAchievementProgress>> {
    return flowOf(addedAchievements)
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
    }
  }

  override suspend fun initializeAchievementsForNewUser(userId: String) {
    for (type in AchievementType.entries) {
      addedAchievements.add(UserAchievementProgress(type, 0))
    }
  }

  override suspend fun updateAchievementValue(
      userId: String,
      achievementType: AchievementType,
      newValue: Int
  ) {
    val get = getUserAchievementProgress(userId, achievementType)
    get?.let { value ->
      if (value.currentValue < newValue) {
        setAchievementValue(userId, achievementType, newValue)
      }
    }
  }
}
