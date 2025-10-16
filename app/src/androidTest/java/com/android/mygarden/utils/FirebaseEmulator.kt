package com.android.mygarden.utils

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import org.json.JSONObject

/**
 * Minimal Firebase Auth Emulator helper for androidTest.
 * - No production code changes
 * - No extra dependencies (uses HttpURLConnection)
 * - Focused on Auth (Firestore omitted)
 */
object FirebaseEmulator {
  // ---- Config discovery ----

  // Prefer CI override; else try 127.0.0.1 (if CI set up adb reverse), then 10.0.2.2 (local)
  private fun pickEmulatorHost(): String {
    System.getenv("FIREBASE_EMULATOR_DEVICE_HOST")?.let { forced ->
      Log.d("FirebaseEmulator", "Using forced emulator host from env: $forced")
      return forced
    }
    for (host in listOf("127.0.0.1", "10.0.2.2")) {
      if (canReach(host, AUTH_PORT, 350)) {
        Log.d("FirebaseEmulator", "Chose emulator host by probe: $host")
        return host
      }
    }
    Log.d("FirebaseEmulator", "No host reachable quickly, defaulting to 10.0.2.2")
    return "10.0.2.2"
  }

  private fun canReach(host: String, port: Int, timeoutMs: Int): Boolean =
      try {
        Socket().use {
          it.connect(InetSocketAddress(host, port), timeoutMs)
          true
        }
      } catch (_: Exception) {
        false
      }

  private val HOST: String by lazy { pickEmulatorHost() }
  const val AUTH_PORT = 9099
  const val EMULATORS_PORT = 4400

  private var configured = false

  /** Firebase project ID from google-services.json */
  private val projectId: String by lazy {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)
    FirebaseApp.getInstance().options.projectId
        ?: error("Firebase projectId missing — ensure google-services.json is present.")
  }

  /** REST endpoints (auth emulator) */
  private val authAccountsEndpoint: String by lazy {
    "http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts"
  }

  /** Lightweight hub probe (like Bootcamp) */
  val isRunning: Boolean
    get() =
        try {
          val url = URL("http://$HOST:$EMULATORS_PORT/emulators")
          val c = (url.openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
          val code = c.responseCode
          c.disconnect()
          code in 200..299
        } catch (_: Exception) {
          false
        }

  /** Ensure Firebase initialized and pointed to Auth emulator (idempotent). */
  fun ensureConfigured() {
    if (configured) return
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)
    // We point to emulator regardless of hub probe; your tests control when emulator is up.
    Log.d("FirebaseEmulator", "Using Auth emulator at $HOST:$AUTH_PORT")
    FirebaseAuth.getInstance().useEmulator(HOST, AUTH_PORT)
    configured = true
  }

  /** Short alias to get Auth after configuration. */
  val auth: FirebaseAuth
    get() {
      ensureConfigured()
      return FirebaseAuth.getInstance()
    }

  /** DELETE all accounts in the Auth emulator. */
  fun clearAuthEmulator() {
    httpDelete(URL(authAccountsEndpoint))
  }

  /**
   * Create/sign-in a Google user in the Auth emulator with a fake JWT (no Play Services).
   *
   * @param fakeIdToken header.payload.sig (emulator ignores signature)
   */
  fun createGoogleUser(fakeIdToken: String) {
    val url =
        URL(
            "http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=fake-api-key")
    val postBody = "id_token=$fakeIdToken&providerId=google.com"
    val payload =
        JSONObject()
            .put("postBody", postBody)
            .put("requestUri", "http://localhost")
            .put("returnIdpCredential", true)
            .put("returnSecureToken", true)
            .toString()
    httpPostJson(url, payload)
  }

  /** Optional: change email for a signed-in user (emulator REST). */
  fun changeEmail(idToken: String, newEmail: String) {
    val url =
        URL(
            "http://$HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:update?key=fake-api-key")
    val payload =
        JSONObject()
            .put("idToken", idToken)
            .put("email", newEmail)
            .put("returnSecureToken", true)
            .toString()
    httpPostJson(url, payload)
  }

  // --- tiny HTTP helpers ---

  private fun httpDelete(url: URL) {
    val c =
        (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "DELETE"
          setRequestProperty("Content-Type", "application/json; charset=UTF-8")
          doInput = true
        }
    val code = c.responseCode
    if (code !in 200..299) error("DELETE $url failed: $code ${c.responseMessage}")
    c.disconnect()
  }

  private fun httpPostJson(url: URL, json: String) {
    val c =
        (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "POST"
          setRequestProperty("Content-Type", "application/json; charset=UTF-8")
          doOutput = true
          doInput = true
        }
    OutputStreamWriter(c.outputStream, Charsets.UTF_8).use { it.write(json) }
    val code = c.responseCode
    if (code !in 200..299) {
      val err =
          try {
            BufferedReader(InputStreamReader(c.errorStream)).readText()
          } catch (_: Exception) {
            ""
          }
      error("POST $url failed: $code ${c.responseMessage}. Body: $err")
    }
    c.disconnect()
  }
}
