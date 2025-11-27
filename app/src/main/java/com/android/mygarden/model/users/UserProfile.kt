package com.android.mygarden.model.users

import com.android.mygarden.ui.profile.Avatar

/**
 * Represents the public profile information of a user.
 *
 * @property id The unique Firestore user ID (document ID under `users/{id}`).
 * @property pseudo The display pseudo associated with the user.
 * @property avatar The avatar enum selected by the user. .
 */
data class UserProfile(val id: String, val pseudo: String, val avatar: Avatar)
