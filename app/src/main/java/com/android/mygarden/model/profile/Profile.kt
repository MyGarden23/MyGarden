package com.android.mygarden.model.profile

/**
 * Represents a user's profile within the garden management system.
 *
 * Linked to the Google account used for sign-in via Firebase Auth (field: [uid]). This allows
 * scoping user data (e.g., plants) to a specific authenticated user.
 *
 * @param uid Firebase Auth unique identifier of the user (same as FirebaseUser.uid)
 * @param firstName The user's first name
 * @param lastName The user's last name
 * @param gardeningSkill The user's gardening expertise (e.g., Beginner, Intermediate, Expert)
 * @param favoritePlant The user's favorite plant
 * @param country The user's country of residence
 * @param email The user's email address from Google Sign-In (optional)
 * @param photoUrl The user's avatar URL from Google profile (optional)
 * @param createdAt Epoch millis when the profile was created (server/client set)
 * @param updatedAt Epoch millis when the profile was last updated
 */
data class Profile(
    val firstName: String = "",
    val lastName: String = "",
    val gardeningSkill: GardeningSkill = GardeningSkill.BEGINNER,
    val favoritePlant: String = "",
    val country: String = "",
    val hasSignedIn: Boolean = false,
    val avatar: String? = "A1"
)
/**
 * Represents the different levels of gardening experience and skill.
 *
 * This enum is used in user profiles to indicate their gardening expertise level, which helps the
 * application provide appropriate content, recommendations, and difficulty levels for gardening
 * activities.
 *
 * The skill levels are ordered from least to most experienced, allowing for easy comparison and
 * progression tracking.
 */
enum class GardeningSkill {
  NOVICE,
  BEGINNER,
  INTERMEDIATE,
  ADVANCED,
  EXPERT,
}
