package com.android.mygarden.ui.authentication

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.R
import com.android.mygarden.model.authentication.AuthRepositoryFirebase
import com.android.mygarden.model.profile.GardeningSkill
import com.android.mygarden.model.profile.Profile
import com.android.mygarden.model.profile.ProfileRepositoryProvider
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.profile.Avatar
import com.android.mygarden.ui.theme.MyGardenTheme
import com.android.mygarden.utils.FakeCredentialManager
import com.android.mygarden.utils.FakeJwtGenerator
import com.android.mygarden.utils.FakeProfileRepository
import com.android.mygarden.utils.FirebaseEmulator
import com.google.firebase.auth.GoogleAuthProvider
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Authentication tests for MyGarden. Mirrors the Bootcamp style: UI checks + emulator-backed auth,
 * without production code changes.
 */
@RunWith(AndroidJUnit4::class)
class AuthenticationTest {

  @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

  private val TIMEOUT = 5_000L

  // Longer timeout for CI environment where network can be slower
  private val effectiveTimeout: Long
    get() = if (System.getenv("CI") == "true") 10_000L else TIMEOUT

  @Before
  fun setUp() {
    // Point Firebase to the local emulator (idempotent) and reset state.
    FirebaseEmulator.connectAuth()

    // Verify emulator is actually running (helps with CI debugging)
    if (!FirebaseEmulator.isRunning) {
      android.util.Log.e(
          "AuthenticationTest", " Firebase emulator is not running! Tests will likely fail.")
    }

    FirebaseEmulator.auth.signOut()
    // Optional: clear emulator users between tests if your helper provides it:
    FirebaseEmulator.clearAuthEmulator()

    // Reset ProfileRepositoryProvider to avoid test pollution
    ProfileRepositoryProvider.repository.cleanup()
  }

  @After
  fun tearDown() {
    FirebaseEmulator.auth.signOut()
    // Clean up ProfileRepositoryProvider between tests
    ProfileRepositoryProvider.repository.cleanup()
  }

  @Test
  fun aaa_firebase_emulator_is_accessible() {
    // This test runs first (alphabetically) to verify emulator connectivity
    // Fail fast if emulator is not reachable
    assertTrue(
        "Firebase emulator must be running at ${FirebaseEmulator.isRunning}",
        FirebaseEmulator.isRunning)
    assertNotNull("FirebaseAuth instance should be configured", FirebaseEmulator.auth)
    android.util.Log.i("AuthenticationTest", "✓ Firebase emulator is accessible and configured")
  }

  @Test
  fun google_sign_in_is_configured() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val idRes =
        context.resources.getIdentifier("default_web_client_id", "string", context.packageName)

    // Skip on environments where the client id isn’t wired (e.g., CI secrets not present)
    assumeTrue("Google Sign-In not configured — skipping", idRes != 0)

    val clientId = context.getString(idRes)
    assertTrue(
        "Invalid Google client ID format: $clientId", clientId.endsWith(".googleusercontent.com"))
  }

  @Test
  fun loggedOut_shows_signIn_screen() {
    compose.setContent {
      MyGardenTheme {
        val nav = rememberNavController()
        AppNavHost(navController = nav, startDestination = Screen.Auth.route)
      }
    }
    compose.onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun can_sign_in_with_google_via_fake_credential_manager() {
    // Prepare a fake Google ID token and a fake CredentialManager that returns it
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val fakeToken = FakeJwtGenerator.createFakeGoogleIdToken(email = "test@example.com")
    val fakeCredMgr = FakeCredentialManager.create(fakeToken, ctx)

    // Set up FakeProfileRepository with no profile (simulating a new user)
    ProfileRepositoryProvider.repository = FakeProfileRepository(profile = null)

    // Track the current route so we can assert navigation to Camera
    lateinit var currentRoute: MutableState<String?>

    compose.setContent {
      MyGardenTheme {
        val nav = rememberNavController()
        val backEntry by nav.currentBackStackEntryAsState()
        currentRoute = remember { mutableStateOf<String?>(null) }
        LaunchedEffect(backEntry) { currentRoute.value = backEntry?.destination?.route }

        AppNavHost(
            navController = nav,
            startDestination = Screen.Auth.route,
            credentialManagerProvider = { fakeCredMgr } // inject fake manager just like Bootcamp
            )
      }
    }

    // Tap the sign-in button
    compose
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Wait until we hit NewProfile (for new users) and the user is set on Firebase
    compose.waitUntil(effectiveTimeout) {
      currentRoute.value == Screen.NewProfile.route && FirebaseEmulator.auth.currentUser != null
    }

    // Final assertions
    assertEquals(Screen.NewProfile.route, currentRoute.value)
    assertNotNull(FirebaseEmulator.auth.currentUser)
  }

  @Test
  fun can_sign_in_with_existing_account() =
      kotlinx.coroutines.runBlocking {
        // 1) Seed an existing user directly in the emulator
        val email = "existing@test.com"
        val idToken =
            FakeJwtGenerator.createFakeGoogleIdToken(name = "Existing User", email = email)

        // Sign into emulator directly to create the account, then sign out
        val cred = GoogleAuthProvider.getCredential(idToken, null)
        val created = FirebaseEmulator.auth.signInWithCredential(cred).await().user
        assertNotNull(created)
        FirebaseEmulator.auth.signOut()

        // 2) Set up FakeProfileRepository with an existing profile (simulating an existing user)
        val existingProfile =
            Profile(
                firstName = "Existing",
                lastName = "User",
                pseudo = "existing_user",
                avatar = Avatar.A1,
                gardeningSkill = GardeningSkill.BEGINNER,
                hasSignedIn = true)
        ProfileRepositoryProvider.repository = FakeProfileRepository(profile = existingProfile)

        // 3) Launch app with FakeCredentialManager and click the UI button
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val fakeCredMgr = FakeCredentialManager.create(idToken, ctx)

        lateinit var currentRoute: MutableState<String?>
        compose.setContent {
          MyGardenTheme {
            val nav = rememberNavController()
            val backEntry by nav.currentBackStackEntryAsState()
            currentRoute = remember { mutableStateOf<String?>(null) }
            LaunchedEffect(backEntry) { currentRoute.value = backEntry?.destination?.route }

            AppNavHost(
                navController = nav,
                startDestination = Screen.Auth.route,
                credentialManagerProvider = { fakeCredMgr })
          }
        }

        compose
            .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
            .assertIsDisplayed()
            .performClick()

        // Wait for Firebase to have a current user and for the route to switch to Camera (for
        // existing users)
        compose.waitUntil(effectiveTimeout) {
          val user = FirebaseEmulator.auth.currentUser
          (user?.email == email) && currentRoute.value == Screen.Camera.route
        }

        assertEquals(email, FirebaseEmulator.auth.currentUser!!.email)
        assertEquals(Screen.Camera.route, currentRoute.value)
      }

  // --- Small helper like in your other tests ---
  private fun waitForTextAnywhere(text: String, timeoutMs: Long = effectiveTimeout) {
    compose.waitUntil(timeoutMs) {
      compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
  }

  @Test
  fun signIn_with_invalid_token_stays_on_auth_and_has_no_user() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    // Use an obviously invalid token to make sign-in fail
    val invalidToken = "not-a-valid-jwt-token"
    val fakeCredMgr = FakeCredentialManager.create(invalidToken, ctx)

    // Track which screen (route) we’re currently on
    lateinit var currentRoute: MutableState<String?>

    compose.setContent {
      MyGardenTheme {
        val nav = rememberNavController()
        val backEntry by nav.currentBackStackEntryAsState()
        currentRoute = remember { mutableStateOf<String?>(null) }
        // Keep updating the current route when navigation changes
        LaunchedEffect(backEntry) { currentRoute.value = backEntry?.destination?.route }

        // Start the app at the Auth screen with the fake credential manager
        AppNavHost(
            navController = nav,
            startDestination = Screen.Auth.route,
            credentialManagerProvider = { fakeCredMgr })
      }
    }

    // Click the Google sign-in button
    compose
        .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Wait for navigation (or not) — we expect to stay on Auth
    compose.waitUntil(effectiveTimeout) {
      currentRoute.value == Screen.Auth.route ||
          currentRoute.value == Screen.NewProfile.route ||
          currentRoute.value == Screen.Camera.route
    }

    // We should still be on the Auth screen and have no logged-in user
    assertEquals(
        "Should remain on Auth after failed sign-in", Screen.Auth.route, currentRoute.value)
    Assert.assertNull("No user should be signed in on failure", FirebaseEmulator.auth.currentUser)
  }

  // Repo that purposely throws a GetCredentialCancellationException
  private class CancellationRepo : com.android.mygarden.model.authentication.AuthRepository {
    override suspend fun signInWithGoogle(
        credential: androidx.credentials.Credential
    ): Result<com.android.mygarden.model.authentication.AuthRepository.SignInResult> {
      throw GetCredentialCancellationException("User cancelled sign-in")
    }

    override fun signOut(): Result<Unit> = Result.success(Unit)
  }

  @Test
  fun signIn_handles_GetCredentialCancellationException_gracefully() =
      kotlinx.coroutines.runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // Token doesn’t matter, repo will throw before using it
        val token = FakeJwtGenerator.createFakeGoogleIdToken(email = "cancel@test.com")
        val fakeMgr = FakeCredentialManager.create(token, ctx)

        val vm = SignInViewModel(repository = CancellationRepo())
        vm.signIn(ctx, fakeMgr)

        // Wait for the ViewModel to update its state
        compose.waitUntil(effectiveTimeout) { vm.uiState.value.errorMsg != null }

        val s = vm.uiState.value
        assertEquals(compose.activity.getString(R.string.error_sign_in_cancelled), s.errorMsg)
        assertTrue(s.signedOut)
        assertNull(s.user)
      }

  // Repo that throws NoCredentialException
  private class NoCredentialRepo : com.android.mygarden.model.authentication.AuthRepository {
    override suspend fun signInWithGoogle(
        credential: androidx.credentials.Credential
    ): Result<com.android.mygarden.model.authentication.AuthRepository.SignInResult> {
      throw NoCredentialException("No Google account found")
    }

    override fun signOut(): Result<Unit> = Result.success(Unit)
  }

  @Test
  fun signIn_handles_NoCredentialException_gracefully() =
      kotlinx.coroutines.runBlocking {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val token = FakeJwtGenerator.createFakeGoogleIdToken(email = "nocred@test.com")
        val fakeMgr = FakeCredentialManager.create(token, ctx)
        val vm = SignInViewModel(repository = NoCredentialRepo())

        vm.signIn(ctx, fakeMgr)
        compose.waitUntil(effectiveTimeout) { vm.uiState.value.errorMsg != null }

        val s = vm.uiState.value
        assertEquals(compose.activity.getString(R.string.error_no_google_account), s.errorMsg)
        assertTrue(s.signedOut)
        assertNull(s.user)
      }

  // Repo that throws a subclass of GetCredentialException
  private class ProviderErrorRepo : com.android.mygarden.model.authentication.AuthRepository {
    override suspend fun signInWithGoogle(
        credential: androidx.credentials.Credential
    ): Result<com.android.mygarden.model.authentication.AuthRepository.SignInResult> {
      throw androidx.credentials.exceptions.GetCredentialProviderConfigurationException(
          "Provider misconfigured")
    }

    override fun signOut(): Result<Unit> = Result.success(Unit)
  }

  @Test
  fun signIn_handles_GetCredentialException_gracefully() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val token = FakeJwtGenerator.createFakeGoogleIdToken(email = "provider-error@test.com")
    val fakeMgr = FakeCredentialManager.create(token, ctx)
    val vm = SignInViewModel(repository = ProviderErrorRepo())

    vm.signIn(ctx, fakeMgr)
    compose.waitUntil(effectiveTimeout) { vm.uiState.value.errorMsg != null }

    val s = vm.uiState.value
    assertEquals(compose.activity.getString(R.string.error_failed_credentials), s.errorMsg)
    assertTrue(s.signedOut)
    assertNull(s.user)
  }

  @Test
  fun signIn_handles_Exception_gracefully() = runBlocking {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val cm = mockk<CredentialManager>()
    // Simulate a random unexpected crash path
    coEvery { cm.getCredential(any(), any<GetCredentialRequest>()) } throws
        GetCredentialProviderConfigurationException("Provider misconfigured")

    val vm = SignInViewModel()
    vm.signIn(ctx, cm)

    compose.waitUntil(effectiveTimeout) { vm.uiState.value.errorMsg != null }

    val s = vm.uiState.value
    assertEquals(compose.activity.getString(R.string.error_unexpected), s.errorMsg)
    assertTrue(s.signedOut)
    assertNull(s.user)
  }

  @Test
  fun signOut_clears_current_user_and_returns_success() = runBlocking {
    val auth = FirebaseEmulator.auth
    FirebaseEmulator.clearAuthEmulator()

    // Sign in a fake user on the emulator
    val email = "signout@test.com"
    val idToken = FakeJwtGenerator.createFakeGoogleIdToken(email = email)
    val cred = GoogleAuthProvider.getCredential(idToken, null)
    val user = auth.signInWithCredential(cred).await().user
    assertNotNull("User should be signed in before signOut()", user)

    val repo = AuthRepositoryFirebase()

    // Sign out and check results
    val result = repo.signOut()
    assertTrue(result.isSuccess)
    assertNull("User should be null after signOut()", auth.currentUser)
  }
}
