package com.android.mygarden.model.plant

import java.sql.Timestamp
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing plants in the application.
 *
 * This repository handles both general plant information retrieval and the management of the user's
 * virtual garden.
 */
interface PlantsRepository {

  // Fixed period of time at which we get the plants from the repository to recompute the health
  // status
  val tickDelay: Duration

  // This is defined here to let View Models that use the repository Provider to have access to the
  // plantsFlow
  val plantsFlow: StateFlow<List<OwnedPlant>>

  val plantsFlowTimeoutWhenNoSubscribers: Long

  // This flow emits something at a fixed period of time ; to be collected by the
  // single flow plantsFlow overridden in both repositories
  val ticks: Flow<Unit>

  companion object {
    /**
     * Minimum confidence score threshold for plant identification results.
     *
     * Results from the PlantNet API with a score below this threshold are considered unreliable and
     * will be rejected.
     */
    const val SCORE_THRESHOLD = 0.3
  }

  /**
   * Helper function to read a file into a ByteArray from its path on disk.
   *
   * Reads the image given its file path and returns its byte contents. This is needed for uploading
   * as part of PlantNet or other Plant recognition API calls. If the file cannot be read or doesn't
   * exist, returns an empty ByteArray.
   *
   * @param path The absolute file path of the image to read
   * @return The file's contents as a ByteArray, or empty ByteArray if file not found
   */
  fun imageFileToByteArray(path: String?): ByteArray

  /**
   * Generates plant information using AI based on the provided plant name and a base Plant object.
   *
   * This function interacts with an AI model to generate details about a plant, including its name,
   * Latin name, description, and watering frequency. It parses the AI output and fills in missing
   * or error details as needed.
   *
   * @param basePlant The base Plant object to use for default values and structure
   * @return A Plant object containing the AI-generated information
   */
  suspend fun generatePlantWithAI(basePlant: Plant): Plant

  /**
   * Calls the Gemini AI model to generate plant description content.
   *
   * This function sends a prompt to Google's Gemini 2.5 Flash model via Firebase AI and returns the
   * generated text response. The response is expected to be a JSON object containing plant
   * information.
   *
   * @param prompt The prompt text to send to the AI model
   * @return The AI-generated text response, or an empty string if generation fails
   */
  suspend fun plantDescriptionCallGemini(prompt: String): String

  /**
   * Identifies a plant's Latin name using the PlantNet API from a provided image file path.
   *
   * This method uploads an image to the PlantNet plant identification API and attempts to retrieve
   * the scientific (Latin) name of the plant depicted. If the image file cannot be read or the
   * identification confidence is low, an appropriate message is set in the returned Plant object.
   *
   * @param path Absolute file path to the image to be analyzed.
   * @return A Plant object with the identified Latin name (if found), or a message about
   *   errors/failure.
   */
  suspend fun identifyLatinNameWithPlantNet(path: String?): Plant

  /**
   * Executes a network call to the PlantNet API.
   *
   * This function performs the actual HTTP request to the PlantNet service and returns the raw
   * response body as a string. Used internally by plant identification methods.
   *
   * @param client The OkHttpClient to use for the request
   * @param request The configured HTTP request to execute
   * @return The response body as a string, or an empty string if the request fails
   */
  suspend fun plantNetAPICall(client: okhttp3.OkHttpClient, request: okhttp3.Request): String

  /**
   * Identifies a plant from an image using recognition technology.
   *
   * This function analyzes an image and returns plant information, including name, latin name,
   * description, and care requirements.
   *
   * @param path The file path of the image of the plant to identify
   * @return A Plant object containing the identified plant's information
   */
  suspend fun identifyPlant(path: String?): Plant

  /**
   * Generates a new unique identifier for a plant.
   *
   * @return A unique string identifier
   */
  fun getNewId(): String

  // Utils for the garden (not exhaustive)
  // deletePlant, getPlant, editPlant, ...
  /**
   * Saves a plant to the user's virtual garden.
   *
   * This function converts a general Plant object into an OwnedPlant, which includes tracking
   * information like watering dates and fertilization schedule.
   *
   * @param plant The plant to add to the garden
   * @param id The unique identifier for the plant
   * @param lastWatered The timestamp of the last watering event
   * @return The newly created OwnedPlant with initialized tracking data
   */
  suspend fun saveToGarden(plant: Plant, id: String, lastWatered: Timestamp): OwnedPlant

  /**
   * Retrieves all ownedplants currently in the user's virtual garden.
   *
   * @return A list of all OwnedPlant objects in the garden
   */
  suspend fun getAllOwnedPlants(): List<OwnedPlant>

  /**
   * Retrieves a specific owned plant from the user's garden by its unique identifier.
   *
   * @param id The unique identifier of the plant to retrieve
   * @return The OwnedPlant object with the specified id
   */
  suspend fun getOwnedPlant(id: String): OwnedPlant

  /**
   * Removes a owned plant from the user's virtual garden.
   *
   * @param id The unique identifier of the owned plant to remove
   */
  suspend fun deleteFromGarden(id: String)

  /**
   * Updates an existing owned plant in the user's virtual garden.
   *
   * @param id The unique identifier of the plant to update
   * @param newOwnedPlant The new ownedPlant
   */
  suspend fun editOwnedPlant(id: String, newOwnedPlant: OwnedPlant)

  /**
   * Records that a plant has been watered right now.
   *
   * This method is used when the user performs the real-time action of watering their plant. It
   * updates the lastWatered timestamp to the current time.
   *
   * Note: This differs from editOwnedPlant() which is used for initial configuration (e.g., setting
   * the lastWatered date when first adding a plant to the garden). Use waterPlant() for the "Just
   * Watered" action, and editOwnedPlant() for general plant data modifications.
   *
   * @param id The unique identifier of the plant that was watered
   * @param wateringTime The timestamp when the plant was watered (defaults to now)
   */
  suspend fun waterPlant(
      id: String,
      wateringTime: Timestamp = Timestamp(System.currentTimeMillis())
  )
}
