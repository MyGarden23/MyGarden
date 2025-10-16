package com.android.mygarden.model.profile

/**
 * Represents the different levels of gardening experience and skill.
 *
 * This enum is used in user profiles to indicate their gardening expertise level,
 * which helps the application provide appropriate content, recommendations, and
 * difficulty levels for gardening activities.
 *
 * The skill levels are ordered from least to most experienced, allowing for
 * easy comparison and progression tracking.
 */
enum class GardeningSkill {
  NOVICE,
  BEGINNER,
  INTERMEDIATE,
  ADVANCED,
  EXPERT,
}
