package com.android.mygarden.ui.editPlant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.ui.navigation.Screen
import java.sql.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
   * @param fromRoute The route from which the screen was launched. Defaults to "garden".
   * @param onSavedCalled A mutable list to track if the `onSaved` callback was invoked.
   * @param onDeletedCalled A mutable list to track if the `onDeleted` callback was invoked.
   * @param goBackCalled A mutable list to track if the `goBack` callback was invoked.
   */
  private fun setContentWith(
      vm: FakeEditPlantViewModel = FakeEditPlantViewModel(),
      ownedPlantId: String = "owned-123",
      fromRoute: String? = Screen.Garden.route,
      onSavedCalled: MutableList<Boolean> = mutableListOf(),
      onDeletedCalled: MutableList<Boolean> = mutableListOf(),
      goBackCalled: MutableList<Boolean> = mutableListOf(),
  ) {

    composeRule.setContent {
      // This is to manually modify the stack of the navHost so that I can choose the "from" Screen
      val args = android.os.Bundle().apply { putString("from", fromRoute) }

      val onDeletedCallback =
          if (args.getString("from") != Screen.PlantInfo.route) {
            { onDeletedCalled += true }
          } else {
            null
          }

      EditPlantScreen(
          ownedPlantId = ownedPlantId,
          editPlantViewModel = vm,
          onSaved = { onSavedCalled += true },
          onDeleted = onDeletedCallback,
          goBack = { goBackCalled += true })
    }
  }

  /**
   * Verifies that when the [EditPlantScreen] is composed, it triggers the `loadPlant` function on
   * the ViewModel with the correct `ownedPlantId`.
   */
  /*
  @Test
  fun loadPlant_calledOnComposition_withOwnedPlantId() {
    val vm = FakeEditPlantViewModel()
    setContentWith(vm = vm, ownedPlantId = "abc-xyz")
    composeRule.waitForIdle()
    assertEquals(listOf("abc-xyz"), vm.loadCalls)
  }*/

  /**
   * Tests that clicking the delete button triggers the corresponding ViewModel method `deletePlant`
   * with the correct plant ID, and also invokes the `onDeleted` callback.
   */
  /*
  @Test
  fun delete_callsVmAndCallback() {
    val vm = FakeEditPlantViewModel()
    val onDeleted = mutableListOf<Boolean>()
    setContentWith(vm = vm, onDeletedCalled = onDeleted)

    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()

    // Assert that the deletion popup displays and confirm deleting the plant
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.POPUP).assertIsDisplayed()
    composeRule
        .onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeRule.runOnIdle {}
    composeRule.waitForIdle()

    assertEquals(listOf("owned-123"), vm.deleteCalls)
    assertTrue(onDeleted.isNotEmpty())
  }*/

  /**
   * Tests that the validation error for a blank light exposure field only appears after the user
   * has interacted with (focused on) the input field and not before.
   */
  @Test
  fun lightExposure_errorAppears_onlyAfterUserFocus_whenBlank() {
    setContentWith()

    // Error not shown before interaction
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.ERROR_MESSAGE_LIGHT_EXPOSURE)
        .assertDoesNotExist()

    // Focus into light exposure -> mark as touched
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE).performClick()
    composeRule.waitForIdle()

    // Now an error helper text should appear as the input is by default empty
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.ERROR_MESSAGE_LIGHT_EXPOSURE)
        .assertIsDisplayed()
  }

  // Under this comment: plant deletion popup tests

  /**
   * Asserts that the popup is displayed when the delete button is pressed in the EditPlant screen
   */
  /*
  @Test
  fun deletionPopup_isDisplayed_whenPressDelete() {
    setContentWith()

    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()

    // Assert all nodes are correctly displayed
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.POPUP).assertIsDisplayed()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.QUESTION).assertIsDisplayed()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.DESCRIPTION).assertIsDisplayed()

    // For buttons also assert they are clickable
    composeRule
        .onNodeWithTag(DeletePlantPopupTestTags.CANCEL_BUTTON)
        .assertIsDisplayed()
        .assertIsEnabled()
    composeRule
        .onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON)
        .assertIsDisplayed()
        .assertIsEnabled()
  }*/

  /**
   * Asserts that the popup is not displayed anymore when the delete button is pressed, then going
   * back in the EditPlant screen by keeping the plant in the garden.
   */
  /*
  @Test
  fun deletionPopup_isNoMoreDisplayed_whenGoingBackToEdit() {
    setContentWith()

    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()
    // Keep plant in garden
    composeRule
        .onNodeWithTag(DeletePlantPopupTestTags.CANCEL_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeRule.waitForIdle()

    // Assert all nodes are correctly no more displayed
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.POPUP).assertDoesNotExist()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.QUESTION).assertDoesNotExist()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.DESCRIPTION).assertDoesNotExist()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.CANCEL_BUTTON).assertDoesNotExist()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON).assertDoesNotExist()
  }*/

  /**
   * Asserts that the popup is not displayed anymore when the delete button is pressed, then going
   * back in the Garden screen by deleting the plant.
   */
  /*
  @Test
  fun deletionPopup_isNoMoreDisplayed_whenGoingBackToGarden() {
    setContentWith()

    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).performClick()
    // Delete plant
    composeRule
        .onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeRule.waitForIdle()

    // Assert all nodes are correctly no more displayed
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.POPUP).assertDoesNotExist()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.QUESTION).assertDoesNotExist()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.DESCRIPTION).assertDoesNotExist()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.CANCEL_BUTTON).assertDoesNotExist()
    composeRule.onNodeWithTag(DeletePlantPopupTestTags.CONFIRM_BUTTON).assertDoesNotExist()
  }*/

  /** Test if the delete button exists if we come from the Garden Screen */
  /*@Test
  fun deleteButton_isVisible_whenFromGarden() {
    setContentWith(fromRoute = Screen.Garden.route)
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).assertIsDisplayed()
  }*/
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
              errorMsg = null,
              isRecognized = false))
  override val uiState: StateFlow<EditPlantUIState> = _ui

  val loadCalls = mutableListOf<String>()
  val editCalls = mutableListOf<String>()
  val deleteCalls = mutableListOf<String>()
  val errorMsgs = mutableListOf<Int>()

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

  override fun setName(newName: String) {
    _ui.value = _ui.value.copy(name = newName)
  }

  override fun setLatinName(newLatinName: String) {
    _ui.value = _ui.value.copy(latinName = newLatinName)
  }

  override fun setLocation(newLocation: PlantLocation) {
    _ui.value = _ui.value.copy(location = newLocation)
  }

  override fun setLightExposure(newExposure: String) {
    _ui.value = _ui.value.copy(lightExposure = newExposure)
  }

  override fun setErrorMsg(resId: Int) {
    errorMsgs += resId
    _ui.value = _ui.value.copy(errorMsg = resId)
  }

  override fun clearErrorMsg() {
    _ui.value = _ui.value.copy(errorMsg = null)
  }

  fun setLastWateredNull(ts: Timestamp?) {
    _ui.value = _ui.value.copy(lastWatered = ts)
  }

  fun setIsRecognized() {
    _ui.value = _ui.value.copy(isRecognized = true)
  }
}
