package com.android.sample.model.authentication

import android.os.Bundle
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Small helper interface that isolates Google/Firebase credential logic. Makes it easier to test
 * AuthRepository without calling static SDK methods directly.
 */
interface GoogleSignInHelper {

  /**
   * Converts the raw credential data from Credential Manager into a GoogleIdTokenCredential.
   *
   * @param bundle The data bundle returned after Google sign-in.
   */
  fun extractIdTokenCredential(bundle: Bundle): GoogleIdTokenCredential

  /**
   * Turns a Google ID token into a Firebase AuthCredential for authentication.
   *
   * @param idToken The ID token obtained from the Google credential.
   */
  fun toFirebaseCredential(idToken: String): AuthCredential
}

/** Default production implementation that directly calls the Google and Firebase SDK methods. */
class DefaultGoogleSignInHelper : GoogleSignInHelper {

  override fun extractIdTokenCredential(bundle: Bundle) = GoogleIdTokenCredential.createFrom(bundle)

  override fun toFirebaseCredential(idToken: String) =
      GoogleAuthProvider.getCredential(idToken, null)
}
