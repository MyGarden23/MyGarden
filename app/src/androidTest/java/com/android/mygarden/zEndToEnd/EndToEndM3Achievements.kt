package com.android.mygarden.zEndToEnd

import android.Manifest
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.android.mygarden.MainActivity
import com.android.mygarden.model.achievements.AchievementType
import com.android.mygarden.ui.achievements.AchievementsScreenTestTags
import com.android.mygarden.ui.addFriend.AddFriendTestTags
import com.android.mygarden.ui.authentication.SignInScreenTestTags
import com.android.mygarden.ui.camera.CameraScreenTestTags
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.feed.FeedScreenTestTags
import com.android.mygarden.ui.friendsRequests.FriendsRequestsScreenTestTags
import com.android.mygarden.ui.garden.GardenAchievementsParentScreenTestTags
import com.android.mygarden.ui.garden.GardenTab
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.profile.ProfileScreenTestTags
import com.android.mygarden.utils.FirebaseUtils
import com.android.mygarden.utils.RequiresCamera
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val BOB_USER_PSEUDO = "bob_achievements_test"
const val ALICE_USER_PSEUDO = "alice_achievements_test"

/**
 * End-to-end test for Achievements (M3).
 *
 * Test flow:
 * 1. Sign in as Bob and create his profile
 * 2. Add 3 plants to Bob's garden
 * 3. Go to Bob's achievements and verify plants achievement (Level 3/10)
 * 4. Sign out Bob and sign in as Alice
 * 5. Create Alice's profile
 * 6. Alice sends a friend request to Bob
 * 7. Sign out Alice and sign in as Bob
 * 8. Bob accepts Alice’s friend request
 * 9. Verify Bob’s friends achievement (Level 2/10)
 */
@RequiresCamera
@RunWith(AndroidJUnit4::class)
class EndToEndM3Achievements {
  companion object {
    init {
      // Set system property BEFORE the compose rule is created
      System.setProperty("mygarden.e2e", "true")
    }
  }

  @get:Rule val composeTestRule = createEmptyComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.CAMERA)

  @get:Rule
  val permissionNotifsRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

  private val TIMEOUT = 10_000L

  private val bobFirebaseUtils: FirebaseUtils = FirebaseUtils()
  private lateinit var bobUserUid: String

  private val aliceFirebaseUtils: FirebaseUtils = FirebaseUtils()
  private lateinit var aliceUserUid: String

  private lateinit var scenario: ActivityScenario<MainActivity>

  @Before
  fun setUp() = runTest {
    Log.d("EndToEndM3Achievements", "setUp entry")
    bobFirebaseUtils.initialize()
    aliceFirebaseUtils.initialize()
    Log.d("EndToEndM3Achievements", "Initialized and signed out")

    // Launch the activity
    scenario = ActivityScenario.launch(MainActivity::class.java)
    Log.d("EndToEndM3Achievements", "Activity launched")

    composeTestRule.waitForIdle()
    waitForAppToLoad()
  }

  @After
  fun tearDown() {
    // Clean up the activity scenario
    if (::scenario.isInitialized) {
      scenario.close()
    }
    // Clean up the system property to avoid affecting other tests
    System.clearProperty("mygarden.e2e")
  }

  @Test
  fun test_end_to_end_m3_achievements() =
      runTest(timeout = 200_000.milliseconds) {
        // === STEP 1: SIGN IN AS BOB AND CREATE PROFILE ===
        Log.d("EndToEndM3Achievements", "Step 1: Sign in as Bob")
        composeTestRule
            .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
            .assertIsDisplayed()
            .performClick()

        runBlocking {
          bobFirebaseUtils.signIn()
          bobFirebaseUtils.waitForAuthReady()
        }
        bobUserUid = bobFirebaseUtils.auth.uid!!

        // Create Bob's profile
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD)
            .performTextInput("Bob")
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD)
            .performTextInput("Jones")
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD)
            .performTextInput(BOB_USER_PSEUDO)
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
            .performTextInput("Switzerland")
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).isDisplayed()
        }

        // === STEP 2: ADD 3 PLANTS TO BOB'S GARDEN ===
        Log.d("EndToEndM3Achievements", "Step 2: Adding 3 plants to Bob's garden")
        for (i in 1..3) {
          addPlant("Bob's Plant $i", "Plantus $i", "Description for plant $i")

          // Wait for garden to load after adding plant
          composeTestRule.waitUntil(TIMEOUT) {
            composeTestRule
                .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
                .isDisplayed()
          }

          Log.d("EndToEndM3Achievements", "Added plant $i/3")
        }

        // === STEP 3: GO TO BOB'S ACHIEVEMENTS AND VERIFY PLANTS ACHIEVEMENT ===
        Log.d("EndToEndM3Achievements", "Step 3: Checking Bob's plants achievement")

        // Navigate to achievements tab
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(
                  GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.ACHIEVEMENTS))
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(
                GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.ACHIEVEMENTS))
            .performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.INTERNAL_ACHIEVEMENTS_SCREEN)
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(
                AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.PLANTS_NUMBER),
                useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(
                AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.PLANTS_NUMBER),
                useUnmergedTree = true)
            .assertTextEquals("Level 3/10")

        // === STEP 4: SIGN OUT BOB AND SIGN IN AS ALICE ===
        Log.d("EndToEndM3Achievements", "Step 4: Sign out Bob and sign in as Alice\n")

        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
              .isDisplayed()
        }

        // === STEP 5-6: SIGN IN AS ALICE, CREATE PROFILE AND ADD BOB ===
        Log.d("EndToEndM3Achievements", "Step 5: Sign in as Alice")
        composeTestRule
            .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
            .performClick()

        runBlocking {
          aliceFirebaseUtils.signIn()
          aliceFirebaseUtils.waitForAuthReady()
        }
        aliceUserUid = aliceFirebaseUtils.auth.uid!!

        // Create Alice's profile
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).isDisplayed()
        }
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD)
            .performTextInput("Alice")
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD)
            .performTextInput("Smith")
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD)
            .performTextInput(ALICE_USER_PSEUDO)
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
            .performTextInput("France")
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).isDisplayed()
        }

        composeTestRule.onNodeWithTag(NavigationTestTags.FEED_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).isDisplayed()
        }

        composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.ADD_FRIEND_SCREEN).isDisplayed()
        }

        // Search for Bob
        composeTestRule
            .onNodeWithTag(AddFriendTestTags.SEARCH_TEXT)
            .performTextInput(BOB_USER_PSEUDO)
        composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).performClick()

        // Wait for search results and send friend request
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(BOB_USER_PSEUDO))
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(AddFriendTestTags.getTestTagForButtonOnFriendCard(BOB_USER_PSEUDO))
            .performClick()

        // === STEP 7: SIGN OUT ALICE AND SIGN BACK IN AS BOB TO ACCEPT REQUEST ===
        Log.d("EndToEndM3Achievements", "Signing out Alice to let Bob accept friend request")
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()

        composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
              .isDisplayed()
        }

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).isDisplayed()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
              .isDisplayed()
        }

        // Reconnect with Bob's account
        bobFirebaseUtils.signIn()
        bobFirebaseUtils.waitForAuthReady()

        composeTestRule
            .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
            .performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).isDisplayed()
        }

        // Verify we're signed in as Bob
        composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
              .isDisplayed()
          composeTestRule.onNodeWithTag(GardenAchievementsParentScreenTestTags.PSEUDO).isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(GardenAchievementsParentScreenTestTags.PSEUDO)
            .assertTextEquals(BOB_USER_PSEUDO)

        // Go to feed
        composeTestRule.onNodeWithTag(NavigationTestTags.FEED_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).isDisplayed()
        }

        // Accept Alice's friend request
        composeTestRule.onNodeWithTag(FeedScreenTestTags.FRIENDS_REQUESTS_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FRIENDS_REQUESTS_SCREEN).isDisplayed()
        }

        // Wait for request to be loaded and accept it
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(
                  FriendsRequestsScreenTestTags.getRequestAcceptButtonFromUser(ALICE_USER_PSEUDO))
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(
                FriendsRequestsScreenTestTags.getRequestAcceptButtonFromUser(ALICE_USER_PSEUDO))
            .performClick()

        // Wait for the friend request to be fully processed and achievements to be updated
        composeTestRule.waitForIdle()
        Thread.sleep(
            2000) // Give Firebase time to process the friend addition and achievement update

        Log.d("EndToEndM3Achievements", "Bob accepted Alice's friend request")

        // Go back to feed
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).isDisplayed()
        }

        // === STEP 8: VERIFY Bob'S FRIENDS ACHIEVEMENT (LEVEL 2: 1 FRIEND) ===
        Log.d("EndToEndM3Achievements", "Step 8: Verifying Bob's friends achievement")

        // Navigate to Bob's achievements
        composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(
                GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.ACHIEVEMENTS))
            .performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.INTERNAL_ACHIEVEMENTS_SCREEN)
              .isDisplayed()
        }

        // Verify the UI displays the correct level
        composeTestRule
            .onNodeWithTag(
                AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.FRIENDS_NUMBER),
                useUnmergedTree = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(
                AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.FRIENDS_NUMBER),
                useUnmergedTree = true)
            .assertTextEquals("Level 2/10")

        // Test complete!
        Log.d("EndToEndM3Achievements", "End-to-end test for Achievements completed successfully")
      }

  /** Helper function to add a plant to the current user's garden */
  private fun addPlant(name: String, latinName: String, description: String) {
    // Go to camera
    composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).performClick()

    // Wait for camera ready
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()

    // Plant info screen
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
    }
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).isDisplayed()
    }
    composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

    // Edit plant screen
    composeTestRule.waitUntil(TIMEOUT) {
      composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
    }

    // Edit plant details
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).performTextClearance()
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).performTextInput(name)
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).performTextClearance()
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).performTextInput(latinName)
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .performTextInput(description)
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE).performTextClearance()
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE)
        .performTextInput("Full sun")

    // Scroll to save button
    composeTestRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))

    // Click Save
    composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()

    composeTestRule.waitForIdle()
    Thread.sleep(1000) // Give the app time to fully process the plant addition
  }

  /** Waits for the app to fully load by checking for key UI elements */
  private fun waitForAppToLoad() {
    composeTestRule.waitUntil(TIMEOUT) {
      try {
        composeTestRule.onRoot().fetchSemanticsNode()
        true
      } catch (_: Throwable) {
        false
      }
    }
  }
}
