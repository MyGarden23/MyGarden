package com.android.mygarden.model.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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

    // Mock NetworkRequest.Builder chain
    val builder = mockk<NetworkRequest.Builder>(relaxed = true)
    val request = mockk<NetworkRequest>(relaxed = true)
    mockkConstructor(NetworkRequest.Builder::class)
    every { anyConstructed<NetworkRequest.Builder>().addCapability(any()) } returns builder
    every { anyConstructed<NetworkRequest.Builder>().build() } returns request

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
  fun `isConnected returns false when missing validated capability`() {
    val network = mockk<Network>()
    val capabilities = mockk<NetworkCapabilities>()
    every { connectivityManager.activeNetwork } returns network
    every { connectivityManager.getNetworkCapabilities(network) } returns capabilities
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns false
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

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `observe emits initial state`() = runTest {
    val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs

    val result = observer.observe().first()
    assertFalse(result)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `observe emits true when network becomes available`() = runTest {
    val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs

    val emissions = mutableListOf<Boolean>()
    val job =
        launch(UnconfinedTestDispatcher(testScheduler)) {
          observer.observe().take(2).toList(emissions)
        }

    // Wait for callback to be captured
    testScheduler.runCurrent()

    // Simulate network becoming available
    callbackSlot.captured.onAvailable(mockk())
    testScheduler.runCurrent()

    job.cancel()
    assertEquals(2, emissions.size)
    assertFalse(emissions[0]) // Initial state
    assertTrue(emissions[1]) // After onAvailable
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `observe emits false when network is lost`() = runTest {
    val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs

    val emissions = mutableListOf<Boolean>()
    val job =
        launch(UnconfinedTestDispatcher(testScheduler)) {
          observer.observe().take(3).toList(emissions)
        }

    testScheduler.runCurrent()

    // First emit true (onAvailable)
    callbackSlot.captured.onAvailable(mockk())
    testScheduler.runCurrent()

    // Then emit false (onLost)
    callbackSlot.captured.onLost(mockk())
    testScheduler.runCurrent()

    job.cancel()
    assertEquals(3, emissions.size)
    assertFalse(emissions[0]) // Initial state
    assertTrue(emissions[1]) // After onAvailable
    assertFalse(emissions[2]) // After onLost
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `observe emits false when network is unavailable`() = runTest {
    val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs

    val emissions = mutableListOf<Boolean>()
    val job =
        launch(UnconfinedTestDispatcher(testScheduler)) {
          observer.observe().take(3).toList(emissions)
        }

    testScheduler.runCurrent()

    // First emit true (onAvailable)
    callbackSlot.captured.onAvailable(mockk())
    testScheduler.runCurrent()

    // Then emit false (onUnavailable)
    callbackSlot.captured.onUnavailable()
    testScheduler.runCurrent()

    job.cancel()
    assertEquals(3, emissions.size)
    assertFalse(emissions[0]) // Initial state
    assertTrue(emissions[1]) // After onAvailable
    assertFalse(emissions[2]) // After onUnavailable
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `observe unregisters callback when flow is cancelled`() = runTest {
    val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
    every { connectivityManager.activeNetwork } returns null
    every {
      connectivityManager.registerNetworkCallback(any<NetworkRequest>(), capture(callbackSlot))
    } just Runs
    every {
      connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>())
    } just Runs

    val job =
        launch(UnconfinedTestDispatcher(testScheduler)) {
          observer.observe().collect { /* consume */}
        }

    testScheduler.runCurrent()
    job.cancel()
    testScheduler.runCurrent()

    verify { connectivityManager.unregisterNetworkCallback(callbackSlot.captured) }
  }
}
