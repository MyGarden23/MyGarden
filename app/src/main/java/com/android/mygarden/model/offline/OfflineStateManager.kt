package com.android.mygarden.model.offline

import android.content.Context
import com.android.mygarden.model.connectivity.ConnectivityObserver
import com.android.mygarden.model.connectivity.NetworkConnectivityObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that manages the offline state of the application.
 *
 * Provides a single source of truth for network connectivity status and helps coordinate offline
 * behavior across the app.
 */
object OfflineStateManager {

  private var connectivityObserver: ConnectivityObserver? = null

  private val _isOnline = MutableStateFlow(true)

  /** Flow that emits the current online/offline state. true = online, false = offline */
  val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

  /**
   * Initializes the offline state manager with the application context. Should be called once in
   * MainActivity.onCreate().
   */
  fun initialize(context: Context) {
    if (connectivityObserver == null) {
      connectivityObserver = NetworkConnectivityObserver(context.applicationContext)
    }
  }

  /** Returns the connectivity observer for collecting connectivity changes. */
  fun getConnectivityObserver(): ConnectivityObserver {
    return connectivityObserver
        ?: throw IllegalStateException(
            "OfflineStateManager not initialized. Call initialize() first.")
  }

  /**
   * Updates the online state based on connectivity changes.
   *
   * This function should ONLY be called by the connectivity observer flow collector (typically in
   * MainActivity) and not arbitrarily by other parts of the app. The network state is the single
   * source of truth determined by the Android system's ConnectivityManager callbacks.
   *
   * @param isOnline true if connected to internet, false if disconnected
   */
  fun setOnlineState(isOnline: Boolean) {
    _isOnline.value = isOnline
  }
}
