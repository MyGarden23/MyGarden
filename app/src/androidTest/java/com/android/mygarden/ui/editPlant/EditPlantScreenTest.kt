package com.android.mygarden.ui.editPlant

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.mygarden.model.plant.PlantLocation
import com.android.mygarden.model.plant.testTag
import com.android.mygarden.ui.navigation.NavigationTestTags
import com.android.mygarden.ui.navigation.Screen
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
  @Test
  fun loadPlant_calledOnComposition_withOwnedPlantId() {
    val vm = FakeEditPlantViewModel()
    setContentWith(vm = vm, ownedPlantId = "abc-xyz")
    composeRule.waitForIdle()
    assertEquals(listOf("abc-xyz"), vm.loadCalls)
  }

  /** Verifies that every text fields, image and buttons are displayed when composing the screen. */
  @Test
  fun allComponents_areDisplayed() {
    setContentWith()

    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_IMAGE).assertIsDisplayed()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).assertIsDisplayed()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).assertIsDisplayed()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION).assertIsDisplayed()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LOCATION_TEXTFIELD).assertIsDisplayed()
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE).assertIsDisplayed()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.INPUT_LAST_WATERED).assertIsDisplayed()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_SAVE).assertIsDisplayed()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).assertIsDisplayed()
  }

  /**
   * Verifies that the plant's common name and Latin name fields are enabled for user input on the
   * edit screen when the image is not recognized.
   */
  @Test
  fun name_latinName_location_areNotReadOnlyDisabledWhenImageNotRecognized() {
    setContentWith()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).assertIsEnabled()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).assertIsEnabled()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LOCATION_TEXTFIELD).assertIsEnabled()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE).assertIsEnabled()
  }
  /**
   * Verifies that the plant's common name and Latin name fields are read-only and disabled for user
   * input on the edit screen when the image is recognized.
   */
  @Test
  fun nameAndLatin_areNotEditable_whenPlantIsRecognized() {
    val vm = FakeEditPlantViewModel().apply { setIsRecognized() }
    setContentWith(vm = vm)

    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME).assertIsNotEnabled()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN).assertIsNotEnabled()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LOCATION_TEXTFIELD).assertIsNotEnabled()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LIGHT_EXPOSURE).assertIsNotEnabled()
  }

  /** Verifies that all the possible locations are shown when clicking the drop down menu. */
  @Test
  fun allLocations_areVisible_whenMenuDropDown() {
    setContentWith()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LOCATION_TEXTFIELD).performClick()
    composeRule.waitForIdle()
    for (loc in PlantLocation.entries) {
      composeRule.onNodeWithTag(loc.testTag).assertIsDisplayed()
    }
  }

  /** Verifies that the location drop down menu hides when one is selected. */
  @Test
  fun clickingOnALocation_hide_dropDownMenu() {
    setContentWith()
    composeRule.onNodeWithTag(EditPlantScreenTestTags.LOCATION_TEXTFIELD).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(PlantLocation.INDOOR.testTag).performClick()
    composeRule.waitForIdle()
    for (loc in PlantLocation.entries) {
      composeRule.onNodeWithTag(loc.testTag).assertIsNotDisplayed()
    }
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
   * Verifies that the 'Save' button is initially disabled and only becomes enabled after all the
   * fields : description, name, latin name and the last watered date have been provided by the
   * user. Once enabled, it confirms that clicking 'Save' triggers the ViewModel's `editPlant`
   * method and the `onSaved` callback.
   */
  @Test
  fun save_disabledUntilDescriptionAndDateProvided_thenCallsVmAndCallback() {
    val vm =
        FakeEditPlantViewModel().apply {
          // Start blank to force disabled state
          setDescription("")
          setName("")
          setLatinName("")
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

    // Still disabled because no date set, and description, name, latin name are blank
    saveNode.assertIsNotEnabled()

    // Provide valid description first
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION)
        .performClick()
        .performTextInput("OK")
    composeRule.waitForIdle()
    // Still disabled because date is missing and name, latin name are blank
    saveNode.assertIsNotEnabled()

    // Now set the date via VM (what the dialog would do)
    vm.setLastWatered(Timestamp(1760384175))
    composeRule.waitForIdle()

    // Provide valid name
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_NAME)
        .performClick()
        .performTextInput("OK")
    composeRule.waitForIdle()
    // Still disabled because latin name is blank
    saveNode.assertIsNotEnabled()

    // Provide valid latin name
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.PLANT_LATIN)
        .performClick()
        .performTextInput("OK")
    composeRule.waitForIdle()

    // Enabled once all fields are valid
    saveNode.assertIsEnabled()

    // Click save -> calls VM.editPlant and onSaved
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
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
  }

  /** Verifies that clicking the back button in the top app bar invokes the `goBack` callback. */
  @Test
  fun backButton_invokesGoBack() {
    val goBack = mutableListOf<Boolean>()
    setContentWith(goBackCalled = goBack)

    composeRule.onNodeWithTag(NavigationTestTags.TOP_BAR_NAV_BACK_BUTTON).performClick()
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
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.ERROR_MESSAGE_DESCRIPTION)
        .assertDoesNotExist()

    // Focus into description -> mark as touched
    composeRule.onNodeWithTag(EditPlantScreenTestTags.INPUT_PLANT_DESCRIPTION).performClick()

    composeRule.waitForIdle()

    // Now an error helper text should appear
    composeRule.onNodeWithTag(EditPlantScreenTestTags.ERROR_MESSAGE_DESCRIPTION).isDisplayed()
  }

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
    composeRule.onNodeWithTag(EditPlantScreenTestTags.ERROR_MESSAGE_LIGHT_EXPOSURE).isDisplayed()
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

    composeRule.onNodeWithTag(EditPlantScreenTestTags.ERROR_MESSAGE_DATE).assertDoesNotExist()

    // User presses the calendar button (marks date as 'touched')
    composeRule.onNodeWithTag(EditPlantScreenTestTags.DATE_PICKER_BUTTON).performClick()
    composeRule.waitForIdle()

    // Now an error should be visible for the date field
    composeRule.onNodeWithTag(EditPlantScreenTestTags.ERROR_MESSAGE_DATE).isDisplayed()
  }

  // Under this comment: plant deletion popup tests

  /**
   * Asserts that the popup is displayed when the delete button is pressed in the EditPlant screen
   */
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
  }

  /**
   * Asserts that the popup is not displayed anymore when the delete button is pressed, then going
   * back in the EditPlant screen by keeping the plant in the garden.
   */
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
  }

  /**
   * Asserts that the popup is not displayed anymore when the delete button is pressed, then going
   * back in the Garden screen by deleting the plant.
   */
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
  }

  /** Test if the delete button does not exist if we come from the Plant Info Screen */
  @Test
  fun deleteButton_isHidden_whenFromPlantInfo() {
    setContentWith(fromRoute = Screen.PlantInfo.route)
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).assertDoesNotExist()
  }

  /** Test if the delete button exists if we come from the Garden Screen */
  @Test
  fun deleteButton_isVisible_whenFromGarden() {
    setContentWith(fromRoute = Screen.Garden.route)
    composeRule
        .onNodeWithTag(EditPlantScreenTestTags.SCROLLABLE_COLUMN)
        .performScrollToNode(hasTestTag(EditPlantScreenTestTags.PLANT_DELETE))
    composeRule.onNodeWithTag(EditPlantScreenTestTags.PLANT_DELETE).assertIsDisplayed()
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
