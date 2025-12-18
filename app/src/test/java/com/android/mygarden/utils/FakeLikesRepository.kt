package com.android.mygarden.utils

import com.android.mygarden.model.profile.LikesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLikesRepository : LikesRepository {
  private val likesCounts = mutableMapOf<String, MutableStateFlow<Int>>()
  private val likedPairs = mutableSetOf<Pair<String, String>>() // (targetUid, myUid)
  private val hasLikedFlows = mutableMapOf<Pair<String, String>, MutableStateFlow<Boolean>>()

  override fun observeLikesCount(targetUid: String): Flow<Int> =
      likesCounts.getOrPut(targetUid) { MutableStateFlow(0) }

  override fun observeHasLiked(targetUid: String, myUid: String): Flow<Boolean> =
      hasLikedFlows.getOrPut(targetUid to myUid) {
        MutableStateFlow(likedPairs.contains(targetUid to myUid))
      }

  override suspend fun toggleLike(targetUid: String, myUid: String) {
    val key = targetUid to myUid
    val already = likedPairs.contains(key)
    if (!already) {
      likedPairs.add(key)
      likesCounts.getOrPut(targetUid) { MutableStateFlow(0) }.value += 1
      hasLikedFlows.getOrPut(key) { MutableStateFlow(false) }.value = true
    } else {
      // simulate "toggle off"
      likedPairs.remove(key)
      likesCounts.getOrPut(targetUid) { MutableStateFlow(0) }.value =
          maxOf(0, likesCounts.getOrPut(targetUid) { MutableStateFlow(0) }.value - 1)
      hasLikedFlows.getOrPut(key) { MutableStateFlow(true) }.value = false
    }
  }
}
