package com.android.mygarden.model.profile

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

  // Returns the currently logged-in user's UID (or null if no one is logged in)
  override fun getCurrentUserId(): String? = auth.currentUser?.uid

  // Shortcut to the current user's document in the "users" collection
  private val userProfile
    get() =
        db.collection("users")
            .document(getCurrentUserId() ?: throw IllegalStateException("User not authenticated"))

  // Listen to the user's profile in Firestore as a Flow (real-time updates)
  override fun getProfile(): Flow<Profile?> {
    val uid = getCurrentUserId() ?: return flowOf(null)
    val docRef = userProfile

    return callbackFlow {
      // Start a Firestore snapshot listener
      val reg: ListenerRegistration =
          docRef.addSnapshotListener { snap, err ->
            if (err != null) {
              // Something went wrong → send null instead of crashing
              trySend(null)
              return@addSnapshotListener
            }
            // Emit the current profile (converted from snapshot)
            trySend(snap?.toProfileOrNull())
          }

      // Clean up listener when the flow collector is closed
      awaitClose { reg.remove() }
    }
  }

  // Save or update the user's profile in Firestore (merge keeps existing fields)
  override suspend fun saveProfile(profile: Profile) {
    userProfile.set(profile.toMap(), SetOptions.merge()).await()
  }

  // Converts a Firestore document to a Profile object
  private fun DocumentSnapshot.toProfileOrNull(): Profile? {
    val data = this.data ?: return null

    val firstName = data["firstName"] as? String ?: return null
    val lastName = data["lastName"] as? String ?: return null
    val favoritePlant = data["favoritePlant"] as? String ?: return null
    val country = data["country"] as? String ?: return null

    // Try to read the gardening skill; fallback to NOVICE if invalid
    val gardeningSkillName = data["gardeningSkill"] as? String ?: return null
    val gardeningSkill =
        runCatching { GardeningSkill.valueOf(gardeningSkillName) }
            .getOrElse { GardeningSkill.NOVICE }

    val hasSignedIn = data["hasSignedIn"] as? Boolean ?: false

    // Default avatar to "A1" if missing (so the app doesn’t crash)
    val avatar = data["avatar"] as? String ?: "A1"

    return Profile(
        firstName = firstName,
        lastName = lastName,
        gardeningSkill = gardeningSkill,
        favoritePlant = favoritePlant,
        country = country,
        hasSignedIn = hasSignedIn,
        avatar = avatar)
  }

  // Converts a Profile object into a Firestore-friendly Map
  private fun Profile.toMap(): Map<String, Any> =
      mapOf(
          "firstName" to firstName,
          "lastName" to lastName,
          "gardeningSkill" to gardeningSkill.name, // store enum as String
          "favoritePlant" to favoritePlant,
          "country" to country,
          "hasSignedIn" to hasSignedIn,
          "avatar" to avatar as Any) // avatar is stored as string like "A1"
}
