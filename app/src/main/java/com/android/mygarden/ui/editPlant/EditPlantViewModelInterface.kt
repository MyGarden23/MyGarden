package com.android.mygarden.ui.editPlant

import java.sql.Timestamp
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface representing the UI state of an edit plant screen. Add this for the convenience of
 * testing.
 */
interface EditPlantViewModelInterface {
  val uiState: StateFlow<EditPlantUIState>

  /** Clears the error message in the UI state. */
  fun clearErrorMsg()

  /**
   * Sets an error message in the UI state.
   *
   * @param e The error message to be displayed.
   */
  fun setErrorMsg(e: String)

  /**
   * Loads a plant by its ID and updates the UI state accordingly.
   *
   * @param ownedPlantId The ID of the plant to load.
   */
  fun loadPlant(ownedPlantId: String)

  /**
   * Sets the last watered date in the UI state.
   *
   * @param timestamp The last watered date to be set.
   */
  fun setLastWatered(timestamp: Timestamp)

  /**
   * Sets the description in the UI state.
   *
   * @param newDescription The new description to be set.
   */
  fun setDescription(newDescription: String)

  /**
   * Deletes a plant by its ID.
   *
   * @param ownedPlantId The ID of the plant to delete.
   */
  fun deletePlant(ownedPlantId: String)

  /**
   * Edits a plant by its ID.
   *
   * @param ownedPlantId The ID of the plant to edit.
   */
  fun editPlant(ownedPlantId: String)
}
