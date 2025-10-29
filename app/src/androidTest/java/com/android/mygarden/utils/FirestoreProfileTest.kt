package com.android.mygarden.utils

import com.android.mygarden.model.profile.ProfileRepository
import com.android.mygarden.model.profile.ProfileRepositoryFirestore
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertNotNull

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
        auth = FirebaseEmulator.auth

        // Fake sign-in (suspend)
        auth.signOut()
        val idToken = FakeJwtGenerator.createFakeGoogleIdToken(email = "test@mygarden.com")
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(cred).await()   // ✅ dans runTest
        val uid = result.user?.uid
        assertNotNull(uid)

        // Clean user doc (suspend)
        db.collection("users").document(uid!!).delete().await() // ✅ dans runTest

        // Inject repo
        ProfileRepositoryProvider.repository = ProfileRepositoryFirestore(db, auth)
        repo = ProfileRepositoryProvider.repository
    }

    @After
    fun tearDown() {
        auth.signOut()
    }
}