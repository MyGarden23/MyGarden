package com.android.mygarden.model.gardenactivity

import android.util.Log
import com.android.mygarden.model.gardenactivity.activitiyclasses.GardenActivity
import com.android.mygarden.model.gardenactivity.serializedactivities.SerializedActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class ActivityRepositoryFirestore(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ActivityRepository {

  companion object {
    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_ACTIVITIES = "activities"
    private const val ACTIVITIES_ORDER_BY = "createdAt"
  }

  // Keep track of active listeners so we can clean them up
  private val activeActivitiesListeners = mutableListOf<ListenerRegistration>()

  // Returns the currently logged-in user's UID (or null if no one is logged in)
  override fun getCurrentUserId(): String? = auth.currentUser?.uid

  // Shortcut to the current user's activities subcollection
  private fun userActivities(userId: String) =
      db.collection(COLLECTION_USERS).document(userId).collection(COLLECTION_ACTIVITIES)

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
                  Log.e(
                      "ActivityRepositoryFirestore",
                      "Failed to listen to activities for user $userId",
                      err)
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
                        "ActivityRepositoryFirestore",
                        "Failed to listen to activities for user $userId",
                        err)
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
      Log.e("ActivityRepositoryFirestore", "Failed to add activity", e)
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
      Log.e("ActivityRepositoryFirestore", "Failed to deserialize activity of type $type", e)
      null
    }
  }

  // Cleanup method to remove active listeners before logout
  override fun cleanup() {
    activeActivitiesListeners.forEach { it.remove() }
    activeActivitiesListeners.clear()
  }
}
