package com.android.mygarden.model.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Interface for observing network connectivity changes. */
interface ConnectivityObserver {
  /** Flow that emits true when connected to internet, false when disconnected. */
  fun observe(): Flow<Boolean>

  /** Checks current connectivity status synchronously. */
  fun isConnected(): Boolean
}

/**
 * Implementation of ConnectivityObserver using Android's ConnectivityManager.
 *
 * @param context The application context (use applicationContext, not activity context to avoid
 *   memory leaks)
 */
class NetworkConnectivityObserver(private val context: Context) : ConnectivityObserver {

  private val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

  /** Observes network connectivity changes and emits connection status. */
  override fun observe(): Flow<Boolean> =
      callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                  override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    trySend(true)
                  }

                  override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(false)
                  }

                  override fun onUnavailable() {
                    super.onUnavailable()
                    trySend(false)
                  }
                }

            val request =
                NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build()

            connectivityManager.registerNetworkCallback(request, callback)

            // Emit current state immediately
            trySend(isConnected())

            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
          }
          .distinctUntilChanged()

  /** Checks if the device is currently connected to the internet. */
  override fun isConnected(): Boolean {
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  }
}
