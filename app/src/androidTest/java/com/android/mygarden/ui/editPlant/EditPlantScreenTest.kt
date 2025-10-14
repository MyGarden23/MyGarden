package com.android.mygarden.ui.editPlant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [EditPlantScreen]. */
@RunWith(AndroidJUnit4::class)
class EditPlantScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  /**
   * Test helper to set the content of the Compose rule to the [EditPlantScreen].
   *
   * This function simplifies test setup by providing default values for the view model, plant ID,
   * and callback trackers. It allows tests to override specific parameters as needed while keeping
   * the setup concise.
   *
   * @param vm The [FakeEditPlantViewModel] to use for the screen. Defaults to a new instance.
   * @param ownedPlantId The ID of the plant being edited. Defaults to "owned-123".
   * @param onSavedCalled A mutable list to track if the `onSaved` callback was invoked.
   * @param onDeletedCalled A mutable list to track if the `onDeleted` callback was invoked.
   * @param goBackCalled A mutable list to track if the `goBack` callback was invoked.
   */
  private fun setContentWith(
      vm: FakeEditPlantViewModel = FakeEditPlantViewModel(),
      ownedPlantId: String = "owned-123",
      onSavedCalled: MutableList<Boolean> = mutableListOf(),
      onDeletedCalled: MutableList<Boolean> = mutableListOf(),
      goBackCalled: MutableList<Boolean> = mutableListOf(),
  ) {
    composeRule.setContent {
      EditPlantScreen(
          ownedPlantId = ownedPlantId,
          editPlantViewModel = vm,
          onSaved = { onSavedCalled += true },
          onDeleted = { onDeletedCalled += true },
          goBack = { goBackCalled += true })
    }
  }

  /**
   * Verifies that when the [EditPlantScreen] is composed, it triggers the `loadPlant` function on
   * the ViewModel with the correct `ownedPlantId`.
   */
  @Test
  fun loadPlant_calledOnComposition_withOwnedPlantId() {
    val vm = FakeEditPlantViewModel()
    setContentWith(vm = vm, ownedPlantId = "abc-xyz")
    composeRule.waitForIdle()
    assertEquals(listOf("abc-xyz"), vm.loadCalls)
  }

  /**
   * Verifies that the plant's common name and Latin name fields are read-only and disabled for user
   * input on the edit screen.
   */
  @Test
  fun nameAndLatin_areReadOnlyDisabled() {
    setContentWith()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).assertIsNotEnabled()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).assertIsNotEnabled()
  }

  /**
   * Tests that the placeholder image is displayed when the plant data does not contain an image
   * URL. It sets up the screen with default data (which has a null image) and asserts that the
   * image composable, identified by its test tag, is present and visible.
   */
  @Test
  fun image_placeholderIsShown_whenNoImage() {
    setContentWith()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_IMAGE).assertIsDisplayed()
  }

  /**
   * Verifies that the 'Save' button is initially disabled and only becomes enabled after both the
   * description and the last watered date have been provided by the user. Once enabled, it confirms
   * that clicking 'Save' triggers the ViewModel's `editPlant` method and the `onSaved` callback.
   */
  @Test
  fun save_disabledUntilDescriptionAndDateProvided_thenCallsVmAndCallback() {
    val vm =
        FakeEditPlantViewModel().apply {
          // Start blank to force disabled state
          setDescription("")
          setLastWateredNull(null)
        }
    val onSaved = mutableListOf<Boolean>()
    setContentWith(vm = vm, onSavedCalled = onSaved)

    // Initially disabled
    val saveNode = composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE)
    saveNode.assertIsNotEnabled()

    // Touch the date icon to simulate user intent (shows error if missing)
    composeRule.onNodeWithTag(EditPlantScreenTestTags.DATE_PICKER_BUTTON).performClick()

    composeRule.waitForIdle()

    // Still disabled because no date set, and description is blank
    saveNode.assertIsNotEnabled()

    // Provide valid description first
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .performClick()
        .performTextInput("OK")
    composeRule.waitForIdle()
    // Still disabled because date is missing
    saveNode.assertIsNotEnabled()

    // Now set the date via VM (what the dialog would do)
    vm.setLastWatered(Timestamp(1760384175))
    composeRule.waitForIdle()

    // Enabled once both fields are valid
    saveNode.assertIsEnabled()

    // Click save -> calls VM.editPlant and onSaved
    saveNode.performClick()
    composeRule.waitForIdle()

    assertEquals(listOf("owned-123"), vm.editCalls)
    assertTrue(onSaved.isNotEmpty())
  }

  /**
   * Tests that clicking the delete button triggers the corresponding ViewModel method `deletePlant`
   * with the correct plant ID, and also invokes the `onDeleted` callback.
   */
  @Test
  fun delete_callsVmAndCallback() {
    val vm = FakeEditPlantViewModel()
    val onDeleted = mutableListOf<Boolean>()
    setContentWith(vm = vm, onDeletedCalled = onDeleted)

    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()

    composeRule.runOnIdle {}
    composeRule.waitForIdle()

    assertEquals(listOf("owned-123"), vm.deleteCalls)
    assertTrue(onDeleted.isNotEmpty())
  }

  /** Verifies that clicking the back button in the top app bar invokes the `goBack` callback. */
  @Test
  fun backButton_invokesGoBack() {
    val goBack = mutableListOf<Boolean>()
    setContentWith(goBackCalled = goBack)

    composeRule.onNodeWithContentDescription("Back").performClick()
    assertTrue(goBack.isNotEmpty())
  }

  /**
   * Tests that the validation error for a blank description field only appears after the user has
   * interacted with (focused on) the input field. It verifies that initially, with a blank
   * description, no error is shown. After simulating a click on the description input, it asserts
   * that an error message becomes visible.
   */
  @Test
  fun description_errorAppears_onlyAfterUserFocus_whenBlank() {
    val vm =
        FakeEditPlantViewModel().apply {
          setDescription("") // keep it blank
        }
    setContentWith(vm = vm)

    // Error not shown before interaction
    composeRule.onAllNodesWithTag(EditPlantScreenTestTags.ERROR_MESSAGE).assertCountEquals(0)

    // Focus into description -> mark as touched
    composeRule.onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION).performClick()

    composeRule.waitForIdle()

    // Now an error helper text should appear
    composeRule
        .onAllNodesWithTag(EditPlantScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertCountEquals(1)
  }

  /**
   * Verifies that the validation error message for the "last watered" date is only displayed after
   * the user has interacted with the date picker icon, not on initial screen load.
   */
  @OptIn(ExperimentalTestApi::class)
  @Test
  fun lastWatered_errorAppears_onlyAfterUserInteraction_whenMissing() {
    val vm = FakeEditPlantViewModel().apply { setLastWateredNull(null) }
    setContentWith(vm = vm)

    // No error initially
    composeRule
        .onAllNodesWithTag(EditPlantScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertCountEquals(0)

    // User presses the calendar button (marks date as 'touched')
    composeRule.onNodeWithTag(EditPlantScreenTestTags.DATE_PICKER_BUTTON).performClick()
    composeRule.waitForIdle()

    // Now an error should be visible for the date field
    composeRule.waitUntilAtLeastOneExists(hasTestTag(EditPlantScreenTestTags.ERROR_MESSAGE), 5_000)

    composeRule
        .onAllNodesWithTag(EditPlantScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertCountEquals(1)
  }
}

/** Recording fake VM for UI tests (no mocking framework needed). */
private class FakeEditPlantViewModel : EditPlantViewModelInterface {
  private val _ui =
      MutableStateFlow(
          EditPlantUIState(
              name = "TestPlant",
              latinName = "Latin name",
              description = "Initial description",
              lastWatered = Timestamp(1760384175),
              image = null,
              errorMsg = null))
  override val uiState: StateFlow<EditPlantUIState> = _ui

  val loadCalls = mutableListOf<String>()
  val editCalls = mutableListOf<String>()
  val deleteCalls = mutableListOf<String>()
  val errorMsgs = mutableListOf<String>()

  override fun loadPlant(ownedPlantId: String) {
    loadCalls += ownedPlantId
  }

  override fun editPlant(ownedPlantId: String) {
    editCalls += ownedPlantId
  }

  override fun deletePlant(ownedPlantId: String) {
    deleteCalls += ownedPlantId
  }

  override fun setDescription(newDescription: String) {
    _ui.value = _ui.value.copy(description = newDescription)
  }

  override fun setLastWatered(timestamp: Timestamp) {
    _ui.value = _ui.value.copy(lastWatered = timestamp)
  }

  override fun setErrorMsg(e: String) {
    errorMsgs += e
    _ui.value = _ui.value.copy(errorMsg = e)
  }

  override fun clearErrorMsg() {
    _ui.value = _ui.value.copy(errorMsg = null)
  }

  fun setLastWateredNull(ts: Timestamp?) {
    _ui.value = _ui.value.copy(lastWatered = ts)
  }
}
