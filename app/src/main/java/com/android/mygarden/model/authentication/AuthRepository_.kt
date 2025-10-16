package com.android.mygarden.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

// Defines a common interface for handling authentication logic.
// This helps separate the Firebase implementation from the rest of the app logic.
interface AuthRepository_ {

  // Signs in a user with a Google credential (from Credential Manager).
  // Returns a Result wrapping the signed-in FirebaseUser on success,
  // or an exception on failure.
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  // Signs the user out (e.g. clears Firebase session).
  // Returns a Result<Unit> to indicate success or failure.
  fun signOut(): Result<Unit>
}
