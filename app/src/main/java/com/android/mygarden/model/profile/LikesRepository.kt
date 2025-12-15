package com.android.mygarden.model.profile

import kotlinx.coroutines.flow.Flow

/**
 * Repository responsible for handling likes on user gardens (profiles).
 *
 * A like is represented by a document at:
 * users/{targetUid}/likes/{likerUid}
 *
 * The document existence prevents double-like, while a counter
 * (likesCount) stored on the target profile allows fast display.
 */
interface LikesRepository {

    /**
     * Observes the number of likes of the target user's garden.
     *
     * @param targetUid UID of the garden owner
     * @return a flow emitting the current likes count
     */
    fun observeLikesCount(targetUid: String): Flow<Int>

    /**
     * Observes whether the current user has already liked the target garden.
     *
     * @param targetUid UID of the garden owner
     * @param myUid UID of the current user
     * @return a flow emitting true if already liked, false otherwise
     */
    fun observeHasLiked(targetUid: String, myUid: String): Flow<Boolean>

    /**
     * Toggles a like on the target garden.
     *
     * If the garden is not liked yet, a like is added and the counter incremented.
     * If already liked, the like is removed and the counter decremented.
     *
     * This operation is executed atomically using a Firestore transaction.
     *
     * @param targetUid UID of the garden owner
     * @param myUid UID of the current user
     */
    suspend fun toggleLike(targetUid: String, myUid: String)
}