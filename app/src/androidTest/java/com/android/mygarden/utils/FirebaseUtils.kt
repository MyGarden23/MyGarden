package com.android.mygarden.utils

import com.android.mygarden.model.profile.ProfileRepositoryFirestore
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.junit.Assert.assertNotNull

class FirebaseUtils {
  private lateinit var db: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth
  protected lateinit var cred: AuthCredential

  suspend fun initialize() {
    // Emulators
    FirebaseEmulator.connectAuth()
    FirebaseEmulator.clearAuthEmulator()
    db = FirebaseEmulator.connectFirestore()
    FirebaseEmulator.clearFirestoreEmulator()
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

  fun signOut() {
    auth.signOut()
  }
}
