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

@RunWith(AndroidJUnit4::class)
class EditPlantScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  // --- helpers ---
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

  @Test
  fun loadPlant_calledOnComposition_withOwnedPlantId() {
    val vm = FakeEditPlantViewModel()
    setContentWith(vm = vm, ownedPlantId = "abc-xyz")
    composeRule.waitForIdle()
    assertEquals(listOf("abc-xyz"), vm.loadCalls)
  }

  @Test
  fun nameAndLatin_areReadOnlyDisabled() {
    setContentWith()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).assertIsNotEnabled()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).assertIsNotEnabled()
  }

  @Test
  fun image_placeholderIsShown_whenNoImage() {
    setContentWith()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_IMAGE).assertIsDisplayed()
  }

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

  @Test
  fun backButton_invokesGoBack() {
    val goBack = mutableListOf<Boolean>()
    setContentWith(goBackCalled = goBack)

    composeRule.onNodeWithContentDescription("Back").performClick()
    assertTrue(goBack.isNotEmpty())
  }

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

  @Test
  fun lastWatered_errorAppears_onlyAfterUserInteraction_whenMissing() {
    val vm = FakeEditPlantViewModel().apply { setLastWateredNull(null) }
    setContentWith(vm = vm)

    // No error initially
    composeRule.onAllNodesWithTag(EditPlantScreenTestTags.ERROR_MESSAGE).assertCountEquals(0)

    // User presses the calendar button (marks date as 'touched')
    composeRule.onNodeWithTag(EditPlantScreenTestTags.DATE_PICKER_BUTTON).performClick()
    composeRule.waitForIdle()

    // Now an error should be visible for the date field
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
