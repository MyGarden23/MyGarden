package com.android.mygarden.model.profile

import android.util.Log
import com.android.mygarden.model.notifications.PushNotificationsService
import com.android.mygarden.ui.profile.Avatar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class ProfileRepositoryFirestore(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ProfileRepository {

  companion object {
    private const val COLLECTION_USERS = "users"

    // Firestore field names
    private const val FIELD_FIRST_NAME = "firstName"
    private const val FIELD_LAST_NAME = "lastName"
    private const val FIELD_PSEUDO = "pseudo"
    private const val FIELD_GARDENING_SKILL = "gardeningSkill"
    private const val FIELD_FAVORITE_PLANT = "favoritePlant"
    private const val FIELD_COUNTRY = "country"
    private const val FIELD_HAS_SIGNED_IN = "hasSignedIn"
    private const val FIELD_AVATAR = "avatar"

    // Default values
    private const val DEFAULT_AVATAR = "A1"

    // Error messages
    private const val ERROR_USER_NOT_AUTHENTICATED = "User not authenticated"

    // Log tag
    private const val LOG_TAG = "FirestoreProfile"

    // Log messages
    private fun attachTokenFailedMsg(uid: String) = "Failed to attach FCM token to user $uid"

    private fun fetchTokenFailedMsg(uid: String) = "Failed to fetch FCM token for user $uid"
  }

  // Keep track of active listeners so we can clean them up
  private var activeListenerRegistration: ListenerRegistration? = null

  // Returns the currently logged-in user's UID (or null if no one is logged in)
  override fun getCurrentUserId(): String? = auth.currentUser?.uid

  // Shortcut to the current user's document in the "users" collection
  private val userProfile
    get() =
        db.collection(COLLECTION_USERS)
            .document(
                getCurrentUserId() ?: throw IllegalStateException(ERROR_USER_NOT_AUTHENTICATED))

  // Listen to the user's profile in Firestore as a Flow (real-time updates)
  override fun getProfile(): Flow<Profile?> {
    val uid = getCurrentUserId()

    // If no user is logged in, return a flow that emits null
    if (uid == null) return flowOf(null)

    val docRef = userProfile

    return callbackFlow {
      // Start a Firestore snapshot listener
      val reg: ListenerRegistration =
          docRef.addSnapshotListener { snap, err ->
            if (err != null) {
              // Handle PERMISSION_DENIED gracefully (happens after logout)
              trySend(null)
              return@addSnapshotListener
            }
            // Emit the current profile (converted from snapshot)
            trySend(snap?.toProfileOrNull())
          }

      // Store the registration so we can clean it up manually if needed
      activeListenerRegistration = reg

      // Clean up listener when the flow collector is closed
      awaitClose {
        reg.remove()
        activeListenerRegistration = null
      }
    }
  }

  // Save or update the user's profile in Firestore (merge keeps existing fields)
  override suspend fun saveProfile(profile: Profile) {
    userProfile.set(profile.toMap(), SetOptions.merge()).await()
  }

  /**
   * Attach the given Firebase Cloud Messaging token to the user's Firestore profile in a field
   * called "fcmToken". Create the field if it does not already exist and updates it if it exists.
   * If the user currently has no Firestore profile stored, creates one.
   *
   * @param token the new token that will be attached to the profile
   * @return true if the query succeeded otherwise false
   */
  override suspend fun attachFCMToken(token: String): Boolean {
    val uid = getCurrentUserId() ?: return false

    return try {
      userProfile
          .set(mapOf(PushNotificationsService.FIRESTORE_FCM_TOKEN_ID to token), SetOptions.merge())
          .await()
      true
    } catch (e: Exception) {
      Log.e(LOG_TAG, attachTokenFailedMsg(uid), e)
      false
    }
  }

  /**
   * Return the FCM token currently attached to the current user on Firestore. Return null if there
   * is an error or if the user has no FCM attached yet.
   *
   * @return the FCM token of the current user or null if there is none
   */
  override suspend fun getFCMToken(): String? {
    val uid = getCurrentUserId() ?: return null

    return try {
      val snap = userProfile.get().await()
      snap.getString(PushNotificationsService.FIRESTORE_FCM_TOKEN_ID)
    } catch (e: Exception) {
      Log.e(LOG_TAG, fetchTokenFailedMsg(uid), e)
      null
    }
  }

  override suspend fun isCurrentUserPseudo(pseudo: String): Boolean {
    return try {
      val snap = userProfile.get().await()
      val currentPseudo = snap.getString(FIELD_PSEUDO)
      currentPseudo == pseudo
    } catch (_: Exception) {
      false
    }
  }

  // Converts a Firestore document to a Profile object
  private fun DocumentSnapshot.toProfileOrNull(): Profile? {
    val data = this.data ?: return null

    val firstName = data[FIELD_FIRST_NAME] as? String ?: return null
    val lastName = data[FIELD_LAST_NAME] as? String ?: return null
    val pseudo = data[FIELD_PSEUDO] as? String ?: return null
    val favoritePlant = data[FIELD_FAVORITE_PLANT] as? String ?: return null
    val country = data[FIELD_COUNTRY] as? String ?: return null

    // Try to read the gardening skill; fallback to NOVICE if invalid
    val gardeningSkillName = data[FIELD_GARDENING_SKILL] as? String ?: return null
    val gardeningSkill =
        runCatching { GardeningSkill.valueOf(gardeningSkillName) }
            .getOrElse { GardeningSkill.BEGINNER }

    val hasSignedIn = data[FIELD_HAS_SIGNED_IN] as? Boolean ?: false

    // Deserialize the avatar string (default value is A1)
    val avatarString = data[FIELD_AVATAR] as? String ?: DEFAULT_AVATAR
    val avatar = runCatching { Avatar.valueOf(avatarString) }.getOrElse { Avatar.A1 }

    return Profile(
        firstName = firstName,
        lastName = lastName,
        pseudo = pseudo,
        gardeningSkill = gardeningSkill,
        favoritePlant = favoritePlant,
        country = country,
        hasSignedIn = hasSignedIn,
        avatar = avatar)
  }

  // Converts a Profile object into a Firestore-friendly Map
  private fun Profile.toMap(): Map<String, Any> =
      mapOf(
          FIELD_FIRST_NAME to firstName,
          FIELD_LAST_NAME to lastName,
          FIELD_PSEUDO to pseudo,
          FIELD_GARDENING_SKILL to gardeningSkill.name, // store enum as String
          FIELD_FAVORITE_PLANT to favoritePlant,
          FIELD_COUNTRY to country,
          FIELD_HAS_SIGNED_IN to hasSignedIn,
          FIELD_AVATAR to avatar.name) // avatar is stored as string like "A1"

  // Cleanup method to remove active listeners before logout
  override fun cleanup() {
    activeListenerRegistration?.remove()
    activeListenerRegistration = null
  }
}
