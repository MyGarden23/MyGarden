package com.android.mygarden.utils

import com.android.mygarden.model.profile.ProfileRepositoryFirestore
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertNotNull

class FirebaseUtils {
  lateinit var db: FirebaseFirestore
  lateinit var auth: FirebaseAuth
  protected lateinit var cred: AuthCredential
  lateinit var storage: FirebaseStorage

  suspend fun initialize() {
    // Emulators
    FirebaseEmulator.connectAuth()
    FirebaseEmulator.clearAuthEmulator()
    db = FirebaseEmulator.connectFirestore()
    FirebaseEmulator.clearFirestoreEmulator()
    storage = FirebaseEmulator.connectStorage()
    FirebaseEmulator.clearStorageEmulator()
    auth = FirebaseEmulator.auth

    // Fake sign-in (suspend)
    auth.signOut()
    val uniqueEmail = "test.profile+${System.currentTimeMillis()}@example.com"
    val idToken = FakeJwtGenerator.createFakeGoogleIdToken(email = uniqueEmail)
    cred = GoogleAuthProvider.getCredential(idToken, null)
    val result = auth.signInWithCredential(cred).await()
    val uid = result.user?.uid
    assertNotNull(uid)

    // Clean user doc (suspend)
    db.collection("users").document(uid!!).delete().await()

    signOut()
  }

  fun injectProfileRepository() {
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(db, auth)
  }

  suspend fun signIn() {
    val result = auth.signInWithCredential(cred).await()
    val uid = result.user?.uid
    assertNotNull(uid)
  }

  /**
   * Wait for Firebase authentication to be fully ready and propagated. This is especially important
   * after re-login in E2E tests to ensure Firestore operations won't fail with authentication
   * errors.
   *
   * @param maxRetries Maximum number of retry attempts (default: 10)
   * @param delayMs Delay between retries in milliseconds (default: 500)
   */
  suspend fun waitForAuthReady(maxRetries: Int = 10, delayMs: Long = 500) {
    var retries = 0
    while (retries < maxRetries) {
      try {
        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser != null) {
          // Try to access Firestore to ensure auth is fully propagated
          db.collection("users").document(currentUser.uid).get().await()
          // If we get here, auth is ready
          return
        }
      } catch (e: Exception) {
        // Auth not ready yet, will retry
      }
      retries++
      delay(delayMs)
    }
    throw IllegalStateException("Firebase auth not ready after $maxRetries retries")
  }

  fun signOut() {
    auth.signOut()
  }
}
