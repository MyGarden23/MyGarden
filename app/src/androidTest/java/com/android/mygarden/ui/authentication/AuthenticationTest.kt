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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.testutils.FakeCredentialManager
import com.android.mygarden.testutils.FakeJwtGenerator
import com.android.mygarden.testutils.FirebaseEmulator
import com.android.mygarden.ui.navigation.AppNavHost
import com.android.mygarden.ui.navigation.Screen
import com.android.mygarden.ui.theme.MyGardenTheme
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import org.junit.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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

  @Before
  fun setUp() {
    // Point Firebase to the local emulator (idempotent) and reset state.
    FirebaseEmulator.ensureConfigured()
    FirebaseEmulator.auth.signOut()
    // Optional: clear emulator users between tests if your helper provides it:
    // FirebaseEmulator.clearAuthEmulator()
  }

  @After
  fun tearDown() {
    FirebaseEmulator.auth.signOut()
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

    // Wait until we hit Camera OR the user is set on Firebase
    compose.waitUntil(TIMEOUT) {
      currentRoute.value == Screen.Camera.route && FirebaseEmulator.auth.currentUser != null
    }

    // Final assertions
    assertEquals(Screen.Camera.route, currentRoute.value)
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

        // 2) Launch app with FakeCredentialManager and click the UI button
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

        // Wait for Firebase to have a current user and for the route to switch
        compose.waitUntil(TIMEOUT) {
          val user = FirebaseEmulator.auth.currentUser
          (user?.email == email) && currentRoute.value == Screen.Camera.route
        }

        assertEquals(email, FirebaseEmulator.auth.currentUser!!.email)
        assertEquals(Screen.Camera.route, currentRoute.value)
      }

  // --- Small helper like in your other tests ---
  private fun waitForTextAnywhere(text: String, timeoutMs: Long = TIMEOUT) {
    compose.waitUntil(timeoutMs) {
      compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
  }
}
