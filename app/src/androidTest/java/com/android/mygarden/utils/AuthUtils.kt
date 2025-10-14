package com.android.mygarden.testutils

import android.content.Context
import android.util.Base64
import androidx.core.os.bundleOf
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.json.JSONObject

/**
 * Generates fake Google ID tokens and mock CredentialManagers for Android instrumented tests.
 *
 * These helpers allow you to test sign-in flows without hitting Google Play Services.
 */
object FakeJwtGenerator {
    private var counter = 0

    private fun base64UrlEncode(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Creates a fake JWT-shaped Google ID token that can be used with the Firebase Auth Emulator.
     *
     * @param name Display name to embed in the token payload.
     * @param email Email to embed in the token payload.
     */
    fun createFakeGoogleIdToken(name: String = "Test User", email: String = "test@example.com"): String {
        val header = JSONObject(mapOf("alg" to "none"))
        val payload =
            JSONObject(
                mapOf(
                    "sub" to (counter++).toString(),
                    "email" to email,
                    "name" to name,
                    "picture" to "http://example.com/avatar.png",
                    "iss" to "https://accounts.google.com",
                    "aud" to "fake"
                )
            )

        val headerEncoded = base64UrlEncode(header.toString().toByteArray())
        val payloadEncoded = base64UrlEncode(payload.toString().toByteArray())
        val signature = "sig" // emulator doesn't check signature

        return "$headerEncoded.$payloadEncoded.$signature"
    }
}

/**
 * A test-only mock CredentialManager that always returns a fake Google ID token.
 *
 * Use this in your instrumented tests to bypass the real CredentialManager flow.
 *
 * Example:
 * ```kotlin
 * val fakeToken = FakeJwtGenerator.createFakeGoogleIdToken(email = "tester@example.com")
 * val fakeCredManager = FakeCredentialManager.create(fakeToken, context)
 * composeTestRule.setContent {
 *   SignInScreen(credentialManager = fakeCredManager, onSignedIn = { ... })
 * }
 * ```
 */
class FakeCredentialManager private constructor(private val context: Context) :
    CredentialManager by CredentialManager.create(context) {

    companion object {
        /**
         * Creates a mock CredentialManager that always returns a CustomCredential containing
         * the given fakeUserIdToken when getCredential() is called.
         */
        fun create(fakeUserIdToken: String, context: Context): CredentialManager {
            // Mock static creation of GoogleIdTokenCredential
            mockkObject(GoogleIdTokenCredential)
            val googleIdTokenCredential = mockk<GoogleIdTokenCredential>()
            every { googleIdTokenCredential.idToken } returns fakeUserIdToken
            every { GoogleIdTokenCredential.createFrom(any()) } returns googleIdTokenCredential

            // Build a fake CustomCredential (same type as real Google sign-in)
            val fakeCustomCredential =
                CustomCredential(
                    type = TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                    data = bundleOf("id_token" to fakeUserIdToken)
                )

            // Mock the response returned by CredentialManager.getCredential()
            val mockResponse = mockk<GetCredentialResponse>()
            every { mockResponse.credential } returns fakeCustomCredential

            // Mock CredentialManager to return that response
            val fakeCredentialManager = mockk<FakeCredentialManager>(relaxed = true)
            coEvery {
                fakeCredentialManager.getCredential(any<Context>(), any<GetCredentialRequest>())
            } returns mockResponse

            return fakeCredentialManager
        }
    }
}