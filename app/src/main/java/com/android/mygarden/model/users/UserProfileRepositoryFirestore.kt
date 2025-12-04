package com.android.mygarden.model.users

import com.android.mygarden.ui.profile.Avatar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val COLLECTION_USERS = "users"

/**
 * Firestore-backed implementation of [UserProfileRepository].
 *
 * Loads public user profile data from the `users/{userId}` document, including pseudo and avatar.
 * Returns `null` if the user does not exist.
 */
class UserProfileRepositoryFirestore(private val db: FirebaseFirestore) : UserProfileRepository {

  override suspend fun getUserProfile(userId: String): UserProfile? {
    val snapshot = db.collection(COLLECTION_USERS).document(userId).get().await()

    if (!snapshot.exists()) return null

    val pseudo = snapshot.getString("pseudo") ?: return null

    val avatar =
        snapshot.getString("avatar")?.let { avatarString ->
          runCatching { Avatar.valueOf(avatarString) }.getOrNull()
        } ?: Avatar.A1

    val gardeningSkill = snapshot.getString("gardeningSkill") ?: return null
    val favoritePlant = snapshot.getString("favoritePlant") ?: return null

    return UserProfile(id = userId, pseudo = pseudo, avatar = avatar, gardeningSkill, favoritePlant)
  }
}
