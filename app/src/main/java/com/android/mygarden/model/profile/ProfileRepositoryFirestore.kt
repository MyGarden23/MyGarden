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

  override fun getCurrentUserId(): String? = auth.currentUser?.uid

  private val userProfile
    get() =
        db.collection("users")
            .document(getCurrentUserId() ?: throw IllegalStateException("User not authenticated"))

  override fun getProfile(): Flow<Profile?> {
    val uid = getCurrentUserId() ?: return flowOf(null)
    val docRef = userProfile

    return callbackFlow {
      val reg: ListenerRegistration =
          docRef.addSnapshotListener { snap, err ->
            if (err != null) {
              // En prod: log l'erreur si besoin
              trySend(null)
              return@addSnapshotListener
            }
            trySend(snap?.toProfileOrNull())
          }
      awaitClose { reg.remove() }
    }
  }

  override suspend fun saveProfile(profile: Profile) {
    userProfile.set(profile.toMap(), SetOptions.merge()).await()
  }

  private fun DocumentSnapshot.toProfileOrNull(): Profile? {
    val data = this.data ?: return null

    val firstName = data["firstName"] as? String ?: return null
    val lastName = data["lastName"] as? String ?: return null
    val favoritePlant = data["favoritePlant"] as? String ?: return null
    val country = data["country"] as? String ?: return null

    val gardeningSkillName = data["gardeningSkill"] as? String ?: return null
    val gardeningSkill =
        runCatching { GardeningSkill.valueOf(gardeningSkillName) }
            .getOrElse { GardeningSkill.NOVICE } // fallback robuste
    val hasSignedIn = data["hasSignedIn"] as? Boolean ?: false
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

  private fun Profile.toMap(): Map<String, Any> =
      mapOf(
          "firstName" to firstName,
          "lastName" to lastName,
          "gardeningSkill" to gardeningSkill.name, // stocké en String
          "favoritePlant" to favoritePlant,
          "country" to country,
          "hasSignedIn" to hasSignedIn,
          "avatar" to avatar as Any)
}
