package com.android.mygarden.utils

import com.android.mygarden.model.profile.LikesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLikesRepository : LikesRepository {
  private val counts = mutableMapOf<String, MutableStateFlow<Int>>()
  private val liked = mutableSetOf<Pair<String, String>>() // (targetUid, myUid)

  override fun observeLikesCount(targetUid: String): Flow<Int> =
      counts.getOrPut(targetUid) { MutableStateFlow(0) }

  override fun observeHasLiked(targetUid: String, myUid: String): Flow<Boolean> =
      MutableStateFlow(liked.contains(targetUid to myUid))

  override suspend fun toggleLike(targetUid: String, myUid: String) {
    val key = targetUid to myUid
    if (liked.add(key)) {
      val flow = counts.getOrPut(targetUid) { MutableStateFlow(0) }
      flow.value = flow.value + 1
    }
  }
}
