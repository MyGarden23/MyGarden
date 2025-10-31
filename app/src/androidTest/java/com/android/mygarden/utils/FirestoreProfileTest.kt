package com.android.mygarden.utils

import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryFirestore
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before

abstract class FirestoreProfileTest {

  protected lateinit var db: FirebaseFirestore
  protected lateinit var auth: FirebaseAuth
  protected lateinit var repo: ProfileRepository

  @Before
  open fun setUp() = runTest {
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
    val cred = GoogleAuthProvider.getCredential(idToken, null)
    val result = auth.signInWithCredential(cred).await()
    val uid = result.user?.uid
    assertNotNull(uid)

    // Clean user doc (suspend)
    db.collection("users").document(uid!!).delete().await()

    // Inject repo
    ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(db, auth)
    repo = ProfileRepositoryProvider.repository
  }

  @After
  fun tearDown() {
    auth.signOut()
  }
}
