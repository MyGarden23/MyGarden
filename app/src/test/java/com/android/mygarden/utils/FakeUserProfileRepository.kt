package com.android.mygarden.utils

import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.users.UserProfile
import com.android.mygarden.model.users.UserProfileRepository
import com.android.mygarden.ui.profile.Avatar

/**
 * A simple in-memory fake implementation of [UserProfileRepository] for unit tests.
 *
 * This fake avoids any interaction with Firebase or the network. Test cases can pre-populate the
 * [profiles] map to control what `getUserProfile(userId)` returns.
 *
 * Usage example in tests:
 * ```
 * val fake = FakeUserProfileRepository()
 * fake.profiles["uid-123"] = UserProfile("uid-123", "alice", Avatar.A1)
 * val vm = AddFriendViewModel(userProfileRepository = fake, ...)
 * ```
 */
class FakeUserProfileRepository : UserProfileRepository {

    /** In-memory storage of user profiles keyed by userId. */
    val profiles: MutableMap<String, UserProfile> = mutableMapOf()

    /**
     * Returns the profile stored for the given [userId], or `null` if no profile has been set for
     * that id.
     */
    override suspend fun getUserProfile(userId: String): UserProfile? = profiles[userId]
}
