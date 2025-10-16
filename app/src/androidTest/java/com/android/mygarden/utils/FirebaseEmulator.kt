package com.android.mygarden.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * Minimal Firebase Auth Emulator helper for androidTest.
 * - No production code changes
 * - No extra dependencies (uses HttpURLConnection)
 * - Focused on Auth (Firestore omitted)
 */
object FirebaseEmulator {

    // Host seen from *inside* the Android emulator:
    // - Local dev: 10.0.2.2
    // - CI with adb reverse: 127.0.0.1
    private val DEVICE_HOST: String =
        System.getenv("FIREBASE_EMULATOR_DEVICE_HOST") ?: "10.0.2.2"

    const val AUTH_PORT = 9099
    // Optional: emulators hub
    const val EMULATORS_PORT = 4400

    private var configured = false

    /** Get FirebaseAuth bound to the emulator (lazy-configured). */
    val auth: FirebaseAuth
        get() {
            ensureConfigured()
            return FirebaseAuth.getInstance()
        }

    /** Initialize Firebase and point Auth to emulator (idempotent). */
    fun ensureConfigured() {
        if (configured) return
        val ctx: Context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(ctx).isEmpty()) {
            FirebaseApp.initializeApp(ctx)
        }
        FirebaseAuth.getInstance().useEmulator(DEVICE_HOST, AUTH_PORT)
        configured = true
    }

    /** Check if the Auth emulator is reachable (fast TCP check). */
    val isRunning: Boolean
        get() = try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(DEVICE_HOST, AUTH_PORT), /* timeoutMs = */ 500)
                true
            }
        } catch (_: Exception) {
            false
        }

    /** DELETE all accounts in the Auth emulator (good cleanup between tests). */
    fun clearAuthEmulator(projectId: String = currentProjectId()) {
        val url = URL("http://$DEVICE_HOST:$AUTH_PORT/emulator/v1/projects/$projectId/accounts")
        httpDelete(url)
    }

    /**
     * Create/sign-in a Google user in the Auth emulator with a fake JWT (no Play Services).
     *
     * @param fakeIdToken e.g., from FakeJwtGenerator (header.payload.sig)
     */
    fun createGoogleUser(fakeIdToken: String) {
        val url = URL(
            "http://$DEVICE_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=fake-api-key"
        )
        val postBody = "id_token=$fakeIdToken&providerId=google.com"

        val payload = JSONObject()
            .put("postBody", postBody)
            .put("requestUri", "http://localhost")
            .put("returnIdpCredential", true)
            .put("returnSecureToken", true)
            .toString()

        httpPostJson(url, payload)
    }

    /** (Optional) Change email of a signed-in user using emulator REST API. */
    fun changeEmail(idToken: String, newEmail: String) {
        val url = URL(
            "http://$DEVICE_HOST:$AUTH_PORT/identitytoolkit.googleapis.com/v1/accounts:update?key=fake-api-key"
        )
        val payload = JSONObject()
            .put("idToken", idToken)
            .put("email", newEmail)
            .put("returnSecureToken", true)
            .toString()

        httpPostJson(url, payload)
    }

    // -------- Internals --------

    private fun currentProjectId(): String {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        if (FirebaseApp.getApps(ctx).isEmpty()) FirebaseApp.initializeApp(ctx)
        return FirebaseApp.getInstance().options.projectId
            ?: error("Firebase projectId missing — ensure google-services.json is present in CI.")
    }

    private fun httpDelete(url: URL) {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doInput = true
        }
        val code = conn.responseCode
        if (code !in 200..299) error("DELETE $url failed: $code ${conn.responseMessage}")
        conn.disconnect()
    }

    private fun httpPostJson(url: URL, json: String) {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            doOutput = true
            doInput = true
        }
        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(json) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use(BufferedReader::readText)
            error("POST $url failed: $code ${conn.responseMessage}. Body: $err")
        }
        conn.disconnect()
    }
}
