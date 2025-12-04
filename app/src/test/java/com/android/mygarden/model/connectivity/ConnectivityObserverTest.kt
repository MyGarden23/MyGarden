package com.android.mygarden.model.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ConnectivityObserverTest {

  private lateinit var context: Context
  private lateinit var connectivityManager: ConnectivityManager
  private lateinit var observer: NetworkConnectivityObserver

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    connectivityManager = mockk(relaxed = true)
    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    observer = NetworkConnectivityObserver(context)
  }

  @Test
  fun `isConnected returns false when no active network`() {
    every { connectivityManager.activeNetwork } returns null
    assertFalse(observer.isConnected())
  }

  @Test
  fun `isConnected returns false when network capabilities are null`() {
    val network = mockk<Network>()
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns null
    assertFalse(observer.isConnected())
  }

  @Test
  fun `isConnected returns false when missing internet capability`() {
    val network = mockk<Network>()
    val capabilities = mockk<NetworkCapabilities>()
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
    assertFalse(observer.isConnected())
  }

  @Test
  fun `isConnected returns true when network has required capabilities`() {
    val network = mockk<Network>()
    val capabilities = mockk<NetworkCapabilities>()
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns true
    assertTrue(observer.isConnected())
  }
}
