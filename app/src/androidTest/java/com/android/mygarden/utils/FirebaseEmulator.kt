package com.android.mygarden.utils

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.MemoryCacheSettings
import com.google.firebase.firestore.MemoryEagerGcSettings
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import org.json.JSONObject

/**
 * Firebase Emulator helper for androidTest:
 * - Configures Auth + Firestore + Storage emulators (idempotent)
 * - Provides REST helpers to wipe Auth/Firestore/Storage between tests
 */
object FirebaseEmulator {

  // ---------- Host & ports ----------
  private fun pickEmulatorHost(): String {
    System.getenv("FIREBASE_EMULATOR_DEVICE_HOST")?.let {
      return it
    }
    val isCI = System.getenv("CI")?.equals("true", ignoreCase = true) ?: false
    if (isCI) return "10.0.2.2"
    // probe 10.0.2.2 then 127.0.0.1
    for (host in listOf("10.0.2.2", "127.0.0.1")) {
      if (canReach(host, AUTH_PORT, 500)) return host
    }
    return "10.0.2.2"
  }

  private fun canReach(host: String, port: Int, timeoutMs: Int) =
      try {
        Socket().use {
          it.connect(InetSocketAddress(host, port), timeoutMs)
          true
        }
      } catch (_: Exception) {
        false
      }

  private val HOST: String by lazy { pickEmulatorHost() }
  const val EMULATORS_PORT = 4400
  const val AUTH_PORT = 9099
  const val FIRESTORE_PORT = 8080
  const val STORAGE_PORT = 9199
  const val FUNCTIONS_PORT = 5001

  // ---------- Firebase app / project ----------
  private val projectId: String by lazy {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)
    FirebaseApp.getInstance().options.projectId
        ?: error("Firebase projectId missing — ensure google-services.json is present.")
  }

  private val storageBucket: String by lazy {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)
    FirebaseApp.getInstance().options.storageBucket
        ?: error("Firebase storageBucket missing — ensure google-services.json is present.")
  }

  // ---------- Emulator hub probe ----------
  val isRunning: Boolean
    get() =
        try {
          val url = URL("http://$HOST:$EMULATORS_PORT/emulators")
          val c = (url.openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
          val ok = c.responseCode in 200..299
          c.disconnect()
          ok
        } catch (_: Exception) {
          false
        }

  // ---------- Auth wiring ----------
  @Volatile private var authConfigured = false

  fun connectAuth() {
    if (authConfigured) return
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)
    FirebaseAuth.getInstance().useEmulator(HOST, AUTH_PORT)
    authConfigured = true
    Log.i("FirebaseEmulator", "Auth -> emulator at $HOST:$AUTH_PORT")
  }

  val auth: FirebaseAuth
    get() {
      connectAuth()
      return FirebaseAuth.getInstance()
    }

  // ---------- Firestore wiring ----------
  @Volatile private var firestoreConfigured = false

  fun connectFirestore(): FirebaseFirestore {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)

    val db = FirebaseFirestore.getInstance()

    if (!firestoreConfigured) {
      db.useEmulator(HOST, FIRESTORE_PORT)

      // Deterministic tests: in-memory cache (no disk), eager GC
      db.firestoreSettings =
          FirebaseFirestoreSettings.Builder()
              .setLocalCacheSettings(
                  MemoryCacheSettings.newBuilder()
                      .setGcSettings(MemoryEagerGcSettings.newBuilder().build())
                      .build())
              .build()

      firestoreConfigured = true
      Log.i(
          "FirebaseEmulator", "Firestore -> emulator at $HOST:$FIRESTORE_PORT, project=$projectId")
    }
    return db
  }

  // ---------- Storage wiring ----------
  @Volatile private var storageConfigured = false

  fun connectStorage(): FirebaseStorage {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)

    val storage = FirebaseStorage.getInstance()

    if (!storageConfigured) {
      storage.useEmulator(HOST, STORAGE_PORT)
      storageConfigured = true
      Log.i("FirebaseEmulator", "Storage -> emulator at $HOST:$STORAGE_PORT, bucket=$storageBucket")
    }
    return storage
  }

  fun connectFunctions(): FirebaseFunctions {
    val ctx: Context = ApplicationProvider.getApplicationContext()
    if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)

    val functions = FirebaseFunctions.getInstance()
    functions.useEmulator(HOST, FUNCTIONS_PORT)
    return functions
  }

  // ---------- REST endpoints for wipes ----------
  private val authAccountsEndpoint: String
    get() = "http://$HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts"

  private val firestoreDocsEndpoint: String
    get() =
        "http://$HOST:$FIRESTORE_PORT/emulator/v1/projects/$projectId/databases/(default)/documents"

  private val storageEndpoint: String
    get() = "http://$HOST:$STORAGE_PORT/v0/b/$storageBucket/o"

  fun clearAuthEmulator() {
    httpDelete(URL(authAccountsEndpoint))
  }

  /** Danger: deletes ALL docs in the Firestore emulator (default DB). */
  fun clearFirestoreEmulator() {
    httpDelete(URL(firestoreDocsEndpoint))
  }

  /** Danger: deletes ALL files in the Storage emulator. */
  fun clearStorageEmulator() {
    try {
      httpDelete(URL(storageEndpoint))
    } catch (e: Exception) {
      Log.w("FirebaseEmulator", "Failed to clear Storage emulator: ${e.message}")
    }
  }

  // ---------- Seed / update via Auth REST ----------
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

  // ---------- tiny HTTP helpers ----------
  private fun httpDelete(url: URL) {
    val c =
        (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "DELETE"
          setRequestProperty("Content-Type", "application/json; charset=UTF-8")
          doInput = true
        }
    val code = c.responseCode
    if (code !in 200..299) {
      val msg =
          try {
            c.errorStream?.let { BufferedReader(InputStreamReader(it)).readText() } ?: ""
          } catch (_: Exception) {
            ""
          }
      error("DELETE $url failed: $code ${c.responseMessage}. $msg")
    }
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
