package com.android.mygarden.model.plant

import java.sql.Timestamp
import java.util.concurrent.TimeUnit

/** Shared test helper functions for plant-related tests. */

/** Helper function to create a timestamp X days ago from now */
fun daysAgo(days: Double): Timestamp {
  val millisAgo = (days * TimeUnit.DAYS.toMillis(1)).toLong()
  return Timestamp(System.currentTimeMillis() - millisAgo)
}
