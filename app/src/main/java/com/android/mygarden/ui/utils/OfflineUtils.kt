package com.android.mygarden.ui.utils

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.android.mygarden.R

/**
 * Handles click actions with offline detection.
 *
 * If online, executes the action. If offline, shows a toast message.
 *
 * @param isOnline Whether the device is currently online
 * @param context Android context for showing toast
 * @param offlineMessageResId String resource ID for the message to display when offline
 * @param onlineAction Action to execute when online
 */
fun handleOfflineClick(
    isOnline: Boolean,
    context: Context,
    @StringRes offlineMessageResId: Int,
    onlineAction: () -> Unit
) {
  if (!isOnline) {
    Toast.makeText(context, context.getString(offlineMessageResId), Toast.LENGTH_SHORT).show()
  } else {
    onlineAction()
  }
}

/** Common offline message resource IDs for consistency across the app. */
object OfflineMessages {
  @StringRes val CANNOT_WATER_PLANTS = R.string.offline_cannot_water_plants
  @StringRes val CANNOT_ADD_PLANTS = R.string.offline_cannot_add_plants
  @StringRes val CANNOT_SAVE_PLANT = R.string.offline_cannot_save_plant
  @StringRes val CANNOT_DELETE_PLANT = R.string.offline_cannot_delete_plant
  @StringRes val CANNOT_EDIT_PROFILE = R.string.offline_cannot_edit_profile
  @StringRes val CANNOT_SAVE_PROFILE = R.string.offline_cannot_save_profile
  @StringRes val CANNOT_LOAD_TIPS = R.string.offline_cannot_load_tips
}
