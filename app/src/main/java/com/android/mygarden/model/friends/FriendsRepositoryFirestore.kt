package com.android.mygarden.model.friends

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FriendsRepositoryFirestore(
    private val db: FirebaseFirestore
) : FriendsRepository {

    private fun friendsCollection(userId: String) =
        db.collection("users")
            .document(userId)
            .collection("friends")

    override suspend fun getFriends(userId: String): List<String> {
        val snapshot = friendsCollection(userId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.getString("friendUid")
        }
    }

    override suspend fun addFriend(currentUserId: String, friendUserId: String) {
        require(currentUserId != friendUserId) {
            "You cannot add yourself as a friend."
        }

        val now = FieldValue.serverTimestamp()

        // Add friend in current user’s list
        val currentDoc = friendsCollection(currentUserId).document(friendUserId)
        currentDoc.set(
            mapOf(
                "friendUid" to friendUserId,
                "addedAt" to now
            )
        ).await()

        // Optional: symmetric friendship
        val friendDoc = friendsCollection(friendUserId).document(currentUserId)
        friendDoc.set(
            mapOf(
                "friendUid" to currentUserId,
                "addedAt" to now
            )
        ).await()
    }
}