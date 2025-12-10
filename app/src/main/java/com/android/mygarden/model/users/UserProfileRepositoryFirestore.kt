package com.android.mygarden.model.users

import com.android.mygarden.ui.profile.Avatar
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val COLLECTION_USERS = "users"

/** Firestore field names */
private const val FIELD_PSEUDO = "pseudo"
private const val FIELD_AVATAR = "avatar"
private const val FIELD_GARDENING_SKILL = "gardeningSkill"
private const val FIELD_FAVORITE_PLANT = "favoritePlant"

/** Default avatar value */
private val DEFAULT_AVATAR = Avatar.A1

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

    val pseudo = snapshot.getString(FIELD_PSEUDO) ?: return null

    val avatar =
        snapshot.getString(FIELD_AVATAR)?.let { avatarString ->
          runCatching { Avatar.valueOf(avatarString) }.getOrNull()
        } ?: DEFAULT_AVATAR

    val gardeningSkill = snapshot.getString(FIELD_GARDENING_SKILL) ?: return null
    val favoritePlant = snapshot.getString(FIELD_FAVORITE_PLANT) ?: return null

    return UserProfile(id = userId, pseudo = pseudo, avatar = avatar, gardeningSkill, favoritePlant)
  }
}
