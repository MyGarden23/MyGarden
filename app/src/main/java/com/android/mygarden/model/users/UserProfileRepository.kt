package com.android.mygarden.model.users


fun interface UserProfileRepository {
    /**
     * Retrieves the public profile of a user (pseudo, avatar, etc.)
     *
     * @return The profile if it exists, or null otherwise.
     */
    suspend fun getUserProfile(userId: String): UserProfile?
}