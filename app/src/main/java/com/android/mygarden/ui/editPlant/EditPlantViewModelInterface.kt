package com.android.mygarden.ui.editPlant

import com.android.mygarden.model.plant.PlantLocation
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
   * @param resId The resource ID of the error message to be set.
   */
  fun setErrorMsg(resId: Int)

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
   * Sets the name in the UI state. This function should be called only if the Plant is not
   * recognized by the API.
   *
   * @param newName The new name to be set.
   */
  fun setName(newName: String)

  /**
   * Sets the latin name in the UI state. This function should be called only if the Plant is not
   * recognized by the API.
   *
   * @param newLatinName The new latin name to be set.
   */
  fun setLatinName(newLatinName: String)

  /**
   * Sets the description in the UI state.
   *
   * @param newDescription The new description to be set.
   */
  fun setDescription(newDescription: String)

  /**
   * Sets location of the plant in the UI state. This function should be called only if the Plant is
   * not recognized by the API.
   *
   * @param newLocation The new location of the plant.
   */
  fun setLocation(newLocation: PlantLocation)

  /**
   * Sets light exposure of the plant in the UI state. This function should be called only if the
   * Plant is not recognized by the API.
   *
   * @param newExposure The new location of the plant.
   */
  fun setLightExposure(newExposure: String)

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
