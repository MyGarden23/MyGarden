package com.android.mygarden.model.users

import com.android.mygarden.ui.profile.Avatar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val COLLECTION_USERS = "users"

class UserProfileRepositoryFirestore(private val db: FirebaseFirestore) : UserProfileRepository {

  override suspend fun getUserProfile(userId: String): UserProfile? {
    val snapshot = db.collection(COLLECTION_USERS).document(userId).get().await()

    if (!snapshot.exists()) return null

    val pseudo = snapshot.getString("pseudo") ?: return null
    val avatar =
        snapshot.getString("avatar")?.let { avatarString ->
          runCatching { Avatar.valueOf(avatarString) }.getOrNull()
        } ?: Avatar.A1

    return UserProfile(id = userId, pseudo = pseudo, avatar = avatar)
  }
}
