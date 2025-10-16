package com.android.mygarden.utils

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import java.io.BufferedReader
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

  private val DEVICE_HOST: String by lazy {
    // 1) explicit override
    System.getenv("FIREBASE_EMULATOR_DEVICE_HOST")
        // 2) on CI, default to 127.0.0.1 (adb reverse)
        ?: if ((System.getenv("CI") ?: "").equals("true", ignoreCase = true)) "127.0.0.1"
        // 3) local dev default
        else "10.0.2.2"
  }

  const val AUTH_PORT = 9099
  private var configured = false

  val auth: FirebaseAuth
    get() {
      ensureConfigured()
      return FirebaseAuth.getInstance()
    }

  fun ensureConfigured() {
    if (configured) return
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)
    Log.d("FirebaseEmulator", "Using Auth emulator at $DEVICE_HOST:$AUTH_PORT")
    FirebaseAuth.getInstance().useEmulator(DEVICE_HOST, AUTH_PORT)
    configured = true
  }

  val isRunning: Boolean
    get() =
        try {
          Socket().use {
            it.connect(InetSocketAddress(DEVICE_HOST, AUTH_PORT), 500)
            true
          }
        } catch (_: Exception) {
          false
        }

  fun clearAuthEmulator(projectId: String = currentProjectId()) {
    httpDelete(URL("http://$DEVICE_HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts"))
  }

  fun createGoogleUser(fakeIdToken: String) {
    val url =
        URL(
            "http://$DEVICE_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=fake-api-key")
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

  private fun currentProjectId(): String {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)
    return FirebaseApp.getInstance().options.projectId
        ?: error("Firebase projectId missing — ensure google-services.json is present in CI.")
  }

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
      val err = c.errorStream?.bufferedReader()?.use(BufferedReader::readText)
      error("POST $url failed: $code ${c.responseMessage}. Body: $err")
    }
    c.disconnect()
  }
}
