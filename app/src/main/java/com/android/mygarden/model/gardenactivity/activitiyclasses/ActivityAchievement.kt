package com.android.mygarden.model.gardenactivity.activitiyclasses

import com.android.mygarden.model.gardenactivity.ActivityType
import com.google.firebase.Timestamp

data class ActivityAchievement(
    override val userId: String,
    override val pseudo: String,
    override val timestamp: Timestamp
) : GardenActivity() {
  override val type = ActivityType.ACHIEVEMENT
}
