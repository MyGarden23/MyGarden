package com.android.mygarden

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.android.mygarden.ui.navigation.NavigationTestTags
import org.junit.Rule
import org.junit.Test

/**
 * Minimal, non-E2E coverage for MainActivity.
 * - Verifies the activity composes (root semantics tree available)
 * - Best-effort probe for bottom bar (if app lands on a top-level screen)
 * - No emulator, no network, no brittle text assumptions
 */
class MainActivitySmokeTest {

  @get:Rule val rule = createAndroidComposeRule<MainActivity>()

  private val TIMEOUT = 10_000L

  @Test
  fun activity_launches_and_composes() {
    // If Compose is up, the root semantics node exists.
    waitForRoot()

    // Best-effort probes (won’t fail if absent):
    // 1) Bottom bar present? show it’s displayed
    if (nodeExistsWithTag(NavigationTestTags.BOTTOM_BAR)) {
      rule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    }

    // 2) If you DO render a sign-in button with text, this won’t fail if it’s not there.
    // Replace "Sign in" with your real text or remove this block if you prefer.
    try {
      val nodes = rule.onAllNodesWithText("Sign in").fetchSemanticsNodes()
      if (nodes.isNotEmpty()) {
        // Good enough to know sign-in UI rendered
      }
    } catch (_: Throwable) {
      // ignore if not present
    }
  }

  @Test
  fun recreate_activity_still_composes() {
    // Recreate the activity to hit a different lifecycle path
    rule.activityRule.scenario.recreate()

    // Root should still compose without crashes
    waitForRoot()

    // Best-effort: if bottom bar renders, assert it’s displayed
    if (nodeExistsWithTag(NavigationTestTags.BOTTOM_BAR)) {
      rule.onNodeWithTag(NavigationTestTags.BOTTOM_BAR).assertIsDisplayed()
    }
  }

  // ---------- helpers ----------

  /** Waits until the Compose root semantics node is available. */
  private fun waitForRoot() {
    rule.waitUntil(TIMEOUT) {
      try {
        rule.onRoot().fetchSemanticsNode()
        true
      } catch (_: Throwable) {
        false
      }
    }
  }

  /** Safe probe for a node by tag without failing the test. */
  private fun nodeExistsWithTag(tag: String): Boolean =
      try {
        rule.onNodeWithTag(tag).fetchSemanticsNode()
        true
      } catch (_: Throwable) {
        false
      }
}
