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

  private val col = db.collection("profiles")

  override fun getCurrentUserId(): String? = auth.currentUser?.uid

  override fun getProfile(): Flow<Profile?> {
    val uid = getCurrentUserId() ?: return flowOf(null)
    val docRef = col.document(uid)

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
    val uid =
        profile.uid.ifBlank {
          getCurrentUserId()
              ?: throw IllegalStateException("No authenticated user; cannot save profile")
        }
    col.document(uid).set(profile.toMap(), SetOptions.merge()).await()
  }

  private fun DocumentSnapshot.toProfileOrNull(): Profile? {
    val data = this.data ?: return null

    val uid = (data["uid"] as? String) ?: this.id
    val firstName = data["firstName"] as? String ?: return null
    val lastName = data["lastName"] as? String ?: return null
    val favoritePlant = data["favoritePlant"] as? String ?: return null
    val country = data["country"] as? String ?: return null

    val gardeningSkillName = data["gardeningSkill"] as? String ?: return null
    val gardeningSkill =
        runCatching { GardeningSkill.valueOf(gardeningSkillName) }
            .getOrElse { GardeningSkill.NOVICE } // fallback robuste

    return Profile(
        uid = uid,
        firstName = firstName,
        lastName = lastName,
        gardeningSkill = gardeningSkill,
        favoritePlant = favoritePlant,
        country = country)
  }

  private fun Profile.toMap(): Map<String, Any> =
      mapOf(
          "uid" to uid,
          "firstName" to firstName,
          "lastName" to lastName,
          "gardeningSkill" to gardeningSkill.name, // stocké en String
          "favoritePlant" to favoritePlant,
          "country" to country)
}
