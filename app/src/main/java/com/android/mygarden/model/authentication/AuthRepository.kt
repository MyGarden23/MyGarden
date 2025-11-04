package com.android.mygarden.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

// Defines a common interface for handling authentication logic.
// This helps separate the Firebase implementation from the rest of the app logic.
interface AuthRepository {

  /**
   * Represents the result of a sign-in operation.
   *
   * @property user The FirebaseUser object representing the signed-in user, or null if no user is
   *   signed in.
   * @property isNewUser A Boolean flag indicating whether the user is a new user or not.
   */
  data class SignInResult(val user: FirebaseUser?, val isNewUser: Boolean)

  // Signs in a user with a Google credential (from Credential Manager).
  // Returns a Result wrapping the signed-in SignInResult on success,
  // or an exception on failure.
  suspend fun signInWithGoogle(credential: Credential): Result<SignInResult>

  // Signs the user out (e.g. clears Firebase session).
  // Returns a Result<Unit> to indicate success or failure.
  fun signOut(): Result<Unit>
}
