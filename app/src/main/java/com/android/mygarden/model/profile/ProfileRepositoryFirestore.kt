package com.android.mygarden.model.profile

import android.util.Log
import com.android.mygarden.model.gardenactivity.ActivityMapper
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedActivity
import com.android.mygarden.model.notifications.PushNotificationsService
import com.android.mygarden.ui.profile.Avatar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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
    private const val COLLECTION_ACTIVITIES = "activities"
    private const val ACTIVITIES_ORDER_BY = "createdAt"
  }

  // Keep track of active listeners so we can clean them up
  private var activeListenerRegistration: ListenerRegistration? = null
  private val activeActivitiesListeners = mutableListOf<ListenerRegistration>()

  // Returns the currently logged-in user's UID (or null if no one is logged in)
  override fun getCurrentUserId(): String? = auth.currentUser?.uid

  // Shortcut to the current user's document in the "users" collection
  private val userProfile
    get() =
        db.collection(COLLECTION_USERS)
            .document(getCurrentUserId() ?: throw IllegalStateException("User not authenticated"))

  // Shortcut to the current user's activities subcollection
  private fun userActivities(userId: String) =
      db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_ACTIVITIES)

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
      Log.e("FirestoreProfile", "Failed to attach FCM token to user $uid", e)
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
      Log.e("FirestoreProfile", "Failed to fetch FCM token for user $uid", e)
      null
    }
  }

  /**
   * Returns all activities for the current user's profile as a Flow. Activities are stored in a
   * subcollection under the user document.
   */
  override fun getActivities(): Flow<List<GardenActivity>> {
    val uid = getCurrentUserId() ?: return flowOf(emptyList())
    return getActivitiesForUser(uid)
  }

  /**
   * Returns activities from a specific user. Queries the user's activities subcollection:
   * users/{userId}/activities
   */
  override fun getActivitiesForUser(userId: String): Flow<List<GardenActivity>> {
    return callbackFlow {
      val reg: ListenerRegistration =
          userActivities(userId)
              .orderBy(ACTIVITIES_ORDER_BY, Query.Direction.DESCENDING)
              .addSnapshotListener { snapshots, err ->
                if (err != null) {
                  Log.e("FirestoreProfile", "Failed to listen to activities for user $userId", err)
                  trySend(emptyList())
                  return@addSnapshotListener
                }

                val activities: List<GardenActivity> =
                    snapshots?.documents?.mapNotNull { doc ->
                      val serialized: SerializedActivity? = doc.toSerializedActivity()
                      serialized?.let { ActivityMapper.fromSerializedActivityToActivity(it) }
                    } ?: emptyList()

                trySend(activities)
              }

      activeActivitiesListeners.add(reg)

      awaitClose {
        reg.remove()
        activeActivitiesListeners.remove(reg)
      }
    }
  }

  /**
   * Returns a feed of activities from multiple users (social feed). Makes separate queries to each
   * user's activities subcollection.
   */
  override fun getFeedActivities(userIds: List<String>, limit: Int): Flow<List<GardenActivity>> {
    if (userIds.isEmpty()) return flowOf(emptyList())

    return callbackFlow {
      val allListeners = mutableListOf<ListenerRegistration>()
      val activitiesMap = mutableMapOf<String, List<GardenActivity>>()

      // Listen to each user's activities subcollection
      userIds.forEach { userId ->
        val reg: ListenerRegistration =
            userActivities(userId)
                .orderBy(ACTIVITIES_ORDER_BY, Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .addSnapshotListener { snapshots, err ->
                  if (err != null) {
                    Log.e(
                        "FirestoreProfile", "Failed to listen to activities for user $userId", err)
                    activitiesMap[userId] = emptyList()
                  } else {
                    val activities: List<GardenActivity> =
                        snapshots?.documents?.mapNotNull { doc ->
                          val serialized: SerializedActivity? = doc.toSerializedActivity()
                          serialized?.let { ActivityMapper.fromSerializedActivityToActivity(it) }
                        } ?: emptyList()

                    activitiesMap[userId] = activities
                  }

                  // Merge all activities and sort by timestamp
                  val allActivities =
                      activitiesMap.values.flatten().sortedByDescending { it.createdAt }.take(limit)

                  trySend(allActivities)
                }

        allListeners.add(reg)
      }

      activeActivitiesListeners.addAll(allListeners)

      awaitClose {
        allListeners.forEach { it.remove() }
        activeActivitiesListeners.removeAll(allListeners)
      }
    }
  }

  /**
   * Adds a new activity to the user's activities subcollection. Stored at:
   * users/{userId}/activities/{activityId}
   */
  override suspend fun addActivity(activity: GardenActivity) {
    try {
      // Convert activity to serialized form and add to user's activities subcollection
      val serializedActivity = ActivityMapper.fromActivityToSerializedActivity(activity)
      userActivities(activity.userId).add(serializedActivity).await()
    } catch (e: Exception) {
      Log.e("FirestoreProfile", "Failed to add activity", e)
    }
  }

  /**
   * Converts a Firestore DocumentSnapshot to the appropriate SerializedActivity subclass. Checks
   * the "type" field to determine which class to deserialize to.
   */
  private fun DocumentSnapshot.toSerializedActivity(): SerializedActivity? {
    val type = this.getString("type") ?: return null

    return try {
      val clazz = ActivityMapper.mapTypeToSerializedClass(type) ?: return null
      this.toObject(clazz)
    } catch (e: Exception) {
      Log.e("FirestoreProfile", "Failed to deserialize activity of type $type", e)
      null
    }
  }

  // Converts a Firestore document to a Profile object
  private fun DocumentSnapshot.toProfileOrNull(): Profile? {
    val data = this.data ?: return null

    val firstName = data["firstName"] as? String ?: return null
    val lastName = data["lastName"] as? String ?: return null
    val pseudo = data["pseudo"] as? String ?: return null
    val favoritePlant = data["favoritePlant"] as? String ?: return null
    val country = data["country"] as? String ?: return null

    // Try to read the gardening skill; fallback to NOVICE if invalid
    val gardeningSkillName = data["gardeningSkill"] as? String ?: return null
    val gardeningSkill =
        runCatching { GardeningSkill.valueOf(gardeningSkillName) }
            .getOrElse { GardeningSkill.BEGINNER }

    val hasSignedIn = data["hasSignedIn"] as? Boolean ?: false

    // Deserialize the avatar string (default value is A1)
    val avatarString = data["avatar"] as? String ?: "A1"
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
          "firstName" to firstName,
          "lastName" to lastName,
          "pseudo" to pseudo,
          "gardeningSkill" to gardeningSkill.name, // store enum as String
          "favoritePlant" to favoritePlant,
          "country" to country,
          "hasSignedIn" to hasSignedIn,
          "avatar" to avatar.name) // avatar is stored as string like "A1"

  // Cleanup method to remove active listeners before logout
  override fun cleanup() {
    activeListenerRegistration?.remove()
    activeListenerRegistration = null
    activeActivitiesListeners.forEach { it.remove() }
    activeActivitiesListeners.clear()
  }
}
