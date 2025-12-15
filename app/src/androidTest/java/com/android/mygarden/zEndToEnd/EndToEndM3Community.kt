package com.android.mygarden.zEndToEnd

import android.Manifest
import android.util.Log
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
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
import com.android.mygarden.model.achievements.AchievementsRepositoryProvider
import com.android.mygarden.model.gardenactivity.ActivityRepositoryProvider
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import com.android.mygarden.ui.achievements.AchievementsScreenTestTags
import com.android.mygarden.ui.addFriend.AddFriendTestTags
import com.android.mygarden.ui.authentication.SignInScreenTestTags
import com.android.mygarden.ui.camera.CameraScreenTestTags
import com.android.mygarden.ui.editPlant.EditPlantScreenTestTags
import com.android.mygarden.ui.feed.FeedScreenTestTags
import com.android.mygarden.ui.friendList.FriendListScreenTestTags
import com.android.mygarden.ui.friendsRequests.FriendsRequestsScreenTestTags
import com.android.mygarden.ui.garden.GardenAchievementsParentScreenTestTags
import com.android.mygarden.ui.garden.GardenScreenTestTags
import com.android.mygarden.ui.garden.GardenTab
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.plantinfos.PlantInfoScreenTestTags
import com.android.mygarden.ui.profile.ProfileScreenTestTags
import com.android.mygarden.utils.FirebaseUtils
import com.android.mygarden.utils.RequiresCamera
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val MAIN_USER_ALICE_PSEUDO = "main_alice_pseudo"
const val FRIEND_USER_BOB_PSEUDO = "friend_bob_pseudo"
const val BOB_PLANT_NAME = "Bob's Rose"
const val BOB_PLANT_LATIN_NAME = "Roseus"
const val BOB_PLANT_DESCRIPTION = "Bob's favorite plant"

/**
 * End-to-end test for the Community feature (M3).
 *
 * Test flow:
 * 1. Go to the feed screen
 * 2. Add a friend (Bob sends friend request to Alice)
 * 3. Accept a friend request (Alice accepts Bob's request)
 * 4. View the list of friends
 * 5. Go to a friend's garden
 * 6. Go to a friend's achievements
 * 7. Go to a friend's plant info
 * 8. Go back (navigate back through screens)
 * 9. Return to the feed
 * 10. Check if Bob's activity is displayed in Alice's feed
 */
@RequiresCamera
@RunWith(AndroidJUnit4::class)
class EndToEndM3Community {
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

  private val firebaseUtils: FirebaseUtils = FirebaseUtils()
  private lateinit var mainUserAliceUid: String

  private val friendFirebaseUtils: FirebaseUtils = FirebaseUtils()
  private lateinit var friendUserBobUid: String

  private lateinit var scenario: ActivityScenario<MainActivity>

  @Before
  fun setUp() = runTest {
    Log.d("EndToEndM3Community", "setUpEntry")
    firebaseUtils.initialize()
    friendFirebaseUtils.initialize()
    Log.d("EndToEndM3Community", "Initialized and signed out")

    // Launch the activity
    scenario = ActivityScenario.launch(MainActivity::class.java)
    Log.d("EndToEndM3Community", "Activity launched")

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
  fun test_end_to_end_m3_community() =
      runTest(timeout = 200_000.milliseconds) {
        // === SIGN IN AS MAIN USER ===
        composeTestRule
            .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
            .assertIsDisplayed()
            .performClick()

        runBlocking {
          firebaseUtils.signIn()
          firebaseUtils.waitForAuthReady()
        }
        mainUserAliceUid = firebaseUtils.auth.uid!!

        // === CREATE MAIN USER PROFILE ===
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD)
            .performTextInput("Alice")
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD)
            .performTextInput("Smith")
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD)
            .performTextInput(MAIN_USER_ALICE_PSEUDO)
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
            .performTextInput("Switzerland")
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).isDisplayed()
        }

        // === LOGOUT MAIN USER ===
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

        // === SIGN IN AS FRIEND USER ===
        composeTestRule
            .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
            .performClick()

        runBlocking {
          friendFirebaseUtils.signIn()
          friendFirebaseUtils.waitForAuthReady()
        }

        friendUserBobUid = firebaseUtils.auth.uid!!

        // === CREATE FRIEND USER PROFILE ===
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(ProfileScreenTestTags.SCREEN).isDisplayed()
        }
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.FIRST_NAME_FIELD)
            .performTextInput("Bob")
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.LAST_NAME_FIELD)
            .performTextInput("Jones")
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.PSEUDO_FIELD)
            .performTextInput(FRIEND_USER_BOB_PSEUDO)
        composeTestRule
            .onNodeWithTag(ProfileScreenTestTags.COUNTRY_FIELD)
            .performTextInput("Canada")
        composeTestRule.onNodeWithTag(ProfileScreenTestTags.SAVE_BUTTON).performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).isDisplayed()
        }

        // === STEP 1: GO TO THE FEED SCREEN ===
        composeTestRule.onNodeWithTag(NavigationTestTags.FEED_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).isDisplayed()
        }

        // === STEP 2: ADD A FRIEND (Bob sends friend request to Alice) ===
        composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_FRIEND_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.ADD_FRIEND_SCREEN).isDisplayed()
        }

        // Search for the friend
        composeTestRule
            .onNodeWithTag(AddFriendTestTags.SEARCH_TEXT)
            .performTextInput(MAIN_USER_ALICE_PSEUDO)
        composeTestRule.onNodeWithTag(AddFriendTestTags.SEARCH_BUTTON).performClick()

        // Wait for search results and send friend request
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(
                  AddFriendTestTags.getTestTagForButtonOnFriendCard(MAIN_USER_ALICE_PSEUDO))
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(
                AddFriendTestTags.getTestTagForButtonOnFriendCard(MAIN_USER_ALICE_PSEUDO))
            .performClick()

        // Go back to feed first
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).isDisplayed()
        }

        // === ADD A PLANT TO BOB'S GARDEN ===
        composeTestRule.onNodeWithTag(NavigationTestTags.CAMERA_BUTTON).performClick()

        // Wait for camera ready
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).isDisplayed()
        }
        composeTestRule.onNodeWithTag(CameraScreenTestTags.TAKE_PICTURE_BUTTON).performClick()

        // === PLANT INFO SCREEN ===
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
        }
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).isDisplayed()
        }
        composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.NEXT_BUTTON).performClick()

        // === EDIT PLANT SCREEN ===
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.EDIT_PLANT_SCREEN).isDisplayed()
        }

        // Edit plant details (fill in the name)
        composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).performTextClearance()
        composeTestRule
            .onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME)
            .performTextInput(BOB_PLANT_NAME)
        composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).performTextClearance()
        composeTestRule
            .onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN)
            .performTextInput(BOB_PLANT_LATIN_NAME)
        composeTestRule
            .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
            .performTextClearance()
        composeTestRule
            .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
            .performTextInput(BOB_PLANT_DESCRIPTION)
        composeTestRule.onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE).performTextClearance()
        composeTestRule
            .onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE)
            .performTextInput("Full sun")

        // Scroll to save button (it's at the bottom of the screen)
        composeTestRule
            .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
            .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_SAVE))

        // Click Save to navigate to Garden
        composeTestRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).performClick()

        // Wait for navigation to garden
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
              .isDisplayed()
        }

        // === LOGOUT FRIEND USER (BOB) ===
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).isDisplayed()
        }
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
              .isDisplayed()
        }

        // === SIGN IN BACK AS MAIN USER (ALICE) ===
        firebaseUtils.signIn()
        firebaseUtils.waitForAuthReady()

        composeTestRule
            .onNodeWithTag(SignInScreenTestTags.SIGN_IN_SCREEN_GOOGLE_BUTTON)
            .performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).isDisplayed()
        }

        // ENSURE IT IS THE RIGHT USER
        composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.GARDEN_BUTTON).isDisplayed()
          composeTestRule.onNodeWithTag(GardenAchievementsParentScreenTestTags.PSEUDO).isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(GardenAchievementsParentScreenTestTags.PSEUDO)
            .assertTextEquals(MAIN_USER_ALICE_PSEUDO)

        composeTestRule.onNodeWithTag(NavigationTestTags.FEED_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).isDisplayed()
        }

        // === STEP 3: ACCEPT A FRIEND REQUEST (Alice accepts Bob's request) ===
        composeTestRule.onNodeWithTag(FeedScreenTestTags.FRIENDS_REQUESTS_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FRIENDS_REQUESTS_SCREEN).isDisplayed()
        }

        // Wait for request to be loaded and accept it
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(
                  FriendsRequestsScreenTestTags.getRequestAcceptButtonFromUser(
                      FRIEND_USER_BOB_PSEUDO))
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(
                FriendsRequestsScreenTestTags.getRequestAcceptButtonFromUser(
                    FRIEND_USER_BOB_PSEUDO))
            .performClick()

        // Go back to feed
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).isDisplayed()
        }

        // === STEP 4: VIEW THE LIST OF FRIENDS ===
        composeTestRule.onNodeWithTag(FeedScreenTestTags.FRIEND_LIST_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(FriendListScreenTestTags.SCREEN).isDisplayed()
        }

        // Wait for friend list to load
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(FriendListScreenTestTags.FRIEND_COLUMN).isDisplayed()
          composeTestRule
              .onNodeWithTag(FriendListScreenTestTags.getDelButtonForFriend(friendUserBobUid))
              .isDisplayed()
        }

        // === STEP 5: GO TO A FRIEND'S GARDEN ===
        composeTestRule.onNodeWithTag(FriendListScreenTestTags.REQUEST_CARD).performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(FriendListScreenTestTags.REQUEST_CARD).isDisplayed()
          composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_SIGN_OUT_BUTTON).isNotDisplayed()
          composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).isDisplayed()
        }

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(GardenAchievementsParentScreenTestTags.PSEUDO)
            .assertTextEquals(FRIEND_USER_BOB_PSEUDO)

        // === STEP 6: GO TO A FRIEND'S ACHIEVEMENTS ===
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

        // Verify achievements screen is displayed
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.INTERNAL_ACHIEVEMENTS_SCREEN)
              .isDisplayed()
        }

        composeTestRule
            .onNodeWithTag(
                AchievementsScreenTestTags.getTestTagForAchievementCard(
                    AchievementType.FRIENDS_NUMBER))
            .isDisplayed()

        composeTestRule
            .onNodeWithTag(
                AchievementsScreenTestTags.getTestTagForAchievementCard(
                    AchievementType.FRIENDS_NUMBER))
            .performClick()

        composeTestRule
            .onNodeWithTag(
                AchievementsScreenTestTags.getTestTagForPopup(AchievementType.FRIENDS_NUMBER))
            .isDisplayed()

        // Switch back to Garden tab to view plants
        composeTestRule
            .onNodeWithTag(
                GardenAchievementsParentScreenTestTags.getTestTagForTab(GardenTab.GARDEN))
            .performClick()

        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.INTERNAL_GARDEN_SCREEN).isDisplayed()
        }

        // === STEP 7: GO TO A FRIEND'S PLANT INFO ===
        val bobOwnedPlantList =
            PlantsRepositoryProvider.repository.getAllOwnedPlantsByUserId(friendUserBobUid)
        assertEquals(1, bobOwnedPlantList.size)
        val ownedPlantBob = bobOwnedPlantList.first()

        composeTestRule
            .onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlantBob))
            .isDisplayed()

        composeTestRule
            .onNodeWithTag(GardenScreenTestTags.getTestTagForOwnedPlant(ownedPlantBob))
            .performClick()

        // Verify plant info screen is displayed
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.SCREEN).isDisplayed()
          composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.EDIT_BUTTON).isNotDisplayed()
        }
        composeTestRule
            .onNodeWithTag(PlantInfoScreenTestTags.PLANT_NAME)
            .assertTextEquals(BOB_PLANT_NAME)
        composeTestRule
            .onNodeWithTag(PlantInfoScreenTestTags.PLANT_LATIN_NAME)
            .assertTextEquals(BOB_PLANT_LATIN_NAME)
        composeTestRule
            .onNodeWithTag(PlantInfoScreenTestTags.DESCRIPTION_TEXT)
            .assertTextEquals(BOB_PLANT_DESCRIPTION)

        // === STEP 8: GO BACK (from plant info to garden, then to friend list, then to feed) ===
        composeTestRule.onNodeWithTag(PlantInfoScreenTestTags.BACK_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule
              .onNodeWithTag(NavigationTestTags.GARDEN_ACHIEVEMENTS_PARENT_SCREEN)
              .isDisplayed()
        }

        // Go back from friend's garden to friend list
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(FriendListScreenTestTags.SCREEN).isDisplayed()
        }

        // === STEP 9: RETURN TO THE FEED ===
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
        composeTestRule.waitUntil(TIMEOUT) {
          composeTestRule.onNodeWithTag(NavigationTestTags.FEED_SCREEN).isDisplayed()
        }

        // === STEP 10: CHECK IF BOB'S ACTIVITY IS IN ALICE'S FEED ===
        val bobNewPlantActivity =
            ActivityRepositoryProvider.repository.getActivitiesForUser(friendUserBobUid).first()
        assertEquals(1, bobNewPlantActivity.size)

        // Verify Bob's "added plant" activity is displayed in Alice's feed
        composeTestRule
            .onNodeWithTag(FeedScreenTestTags.getTestTagForActivity(bobNewPlantActivity.first()))
            .isDisplayed()

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

        // Verify Alice's friend achievement was incremented
        val aliceAchievementProgress = runBlocking {
          AchievementsRepositoryProvider.repository.getUserAchievementProgress(
              mainUserAliceUid, AchievementType.FRIENDS_NUMBER)
        }
        assertEquals(
            "Alice's friend achievement should be 1 after accepting Bob",
            1,
            aliceAchievementProgress?.currentValue ?: 0)

        composeTestRule
            .onNodeWithTag(
                AchievementsScreenTestTags.getTestTagForCardLevel(AchievementType.FRIENDS_NUMBER),
                useUnmergedTree = true)
            .assertTextEquals("Level 2/10")

        // Test complete!
        Log.d("EndToEndM3Community", "End-to-end test for Community feature completed successfully")
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
