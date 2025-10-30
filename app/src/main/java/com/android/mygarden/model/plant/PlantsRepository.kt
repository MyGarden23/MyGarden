package com.android.mygarden.model.plant

import android.util.Log
import com.android.mygarden.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.flow.StateFlow
import java.sql.Timestamp
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient

/**
 * Repository interface for managing plants in the application.
 *
 * This repository handles both general plant information retrieval and the management of the user's
 * virtual garden.
 */
interface PlantsRepository {

  val plantsFlow: StateFlow<List<OwnedPlant>>
    get() = MutableStateFlow(emptyList())

  companion object {
    /**
     * Minimum confidence score threshold for plant identification results.
     *
     * Results from the PlantNet API with a score below this threshold are considered unreliable and
     * will be rejected.
     */
    const val SCORE_THRESHOLD = 0.3

    /**
     * Base URL for the PlantNet API endpoint, including the API key parameter.
     *
     * The API key is loaded from local.properties (not committed to Git) or from environment
     * variables in CI/CD.
     */
    private val PLANTNET_API_URL =
        "https://my-api.plantnet.org/v2/identify/all?api-key=${BuildConfig.PLANTNET_API_KEY}"

    /**
     * HTTP client configured for PlantNet API requests.
     *
     * Configured with extended timeouts (30 seconds) to accommodate image upload and processing.
     */
    val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

    /**
     * Builds a multipart request body for uploading an image to the PlantNet API.
     *
     * Creates a form-data request containing the image file and organ type parameter set to "auto"
     * for automatic plant part detection.
     *
     * @param imageBytes The image data as a byte array
     * @return A RequestBody configured for the PlantNet API image upload
     */
    fun buildRequestBody(imageBytes: ByteArray): okhttp3.RequestBody {
      return okhttp3.MultipartBody.Builder()
          .setType(okhttp3.MultipartBody.FORM)
          .addFormDataPart(
              "images",
              "PlantImage.jpg",
              okhttp3.RequestBody.create("image/jpeg".toMediaType(), imageBytes))
          .addFormDataPart("organs", "auto")
          .build()
    }

    /**
     * Builds an HTTP POST request for the PlantNet API.
     *
     * @param requestBody The multipart request body containing the image data
     * @return A configured Request object ready to be executed
     */
    fun buildRequest(requestBody: okhttp3.RequestBody): okhttp3.Request {
      return okhttp3.Request.Builder().url(PLANTNET_API_URL).post(requestBody).build()
    }
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
  fun imageFileToByteArray(path: String?): ByteArray {
    return try {
      java.io.File(path).readBytes()
    } catch (e: Exception) {
      byteArrayOf()
    }
  }

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
  suspend fun generatePlantWithAI(basePlant: Plant): Plant {
    val prompt =
        "Reply ONLY with a valid JSON object as plain text, with no markdown blocks or extra explanation. The response must start with { and end with }. The formatting of the JSON MUST be a single object, NOT an array. Describe the plant by its latin name '${basePlant.latinName}'. The object should include: name, latinName, description, wateringFrequency. For 'name', use the simple/common name (e.g., 'tomato' not 'garden tomato'). 'wateringFrequency' must be an integer representing the number of days between waterings."

    val outputStr =
        try {
          plantDescriptionCallGemini(prompt)
        } catch (e: Exception) {
          Log.d("plantGeneratingLog", e.toString())
          return basePlant.copy(
              description = "The AI couldn't generate a description for this plant.")
        }

    if (outputStr.isEmpty())
        return basePlant.copy(description = "Error while generating plant details")

    try {
      val jsonObject = Json.parseToJsonElement(outputStr).jsonObject
      val res =
          basePlant.copy(
              name = jsonObject["name"]?.jsonPrimitive?.content ?: basePlant.name,
              description =
                  jsonObject["description"]?.jsonPrimitive?.content ?: basePlant.description,
              wateringFrequency =
                  jsonObject["wateringFrequency"]?.jsonPrimitive?.doubleOrNull?.toInt()
                      ?: basePlant.wateringFrequency)

      return res
    } catch (e: Exception) {
      Log.d("plantGeneratingLog", e.toString())
      return basePlant.copy(description = "Error while generating plant details")
    }
  }

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
  suspend fun plantDescriptionCallGemini(prompt: String): String {
    val model =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-2.5-flash")
    val response: GenerateContentResponse = model.generateContent(prompt)
    return response.text ?: ""
  }

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
  suspend fun identifyLatinNameWithPlantNet(path: String?): Plant {

    return withContext(Dispatchers.IO) {

      // Read image file into byte array
      val imageBytes = imageFileToByteArray(path)

      // If image cannot be read, return a Plant with error description
      if (imageBytes.isEmpty()) {
        return@withContext Plant(
            image = path, description = "Could not be read the image or is empty.")
      }

      // Build multipart request for image upload and API call
      val requestBody = buildRequestBody(imageBytes)
      val request = buildRequest(requestBody)

      // handle response
      try {
        // Execute the API request
        val rawBody = plantNetAPICall(client, request)

        // Parse API response JSON
        val json = Json.parseToJsonElement(rawBody)

        // Get first result from PlantNet and its confidence score
        val firstResult = json.jsonObject["results"]?.jsonArray?.first()

        val score = firstResult?.jsonObject?.get("score")?.jsonPrimitive?.doubleOrNull ?: 0.0
        if (score < SCORE_THRESHOLD) {
          // Low confidence: let user know identification failed
          return@withContext Plant(
              image = path, description = "The AI was not able to identify the plant.")
        }

        // Extract Latin name from response JSON
        val latinName =
            firstResult
                ?.jsonObject
                ?.get("species")
                ?.jsonObject
                ?.get("scientificNameWithoutAuthor")
                ?.jsonPrimitive
                ?.content ?: "Unknown"

        return@withContext Plant(latinName = latinName, image = path)
      } catch (e: Exception) {
        // Log and handle API/network errors
        Log.e("plantNetApiLog", "Error while calling PlantNet API: ", e)
      }
      // Fallback for any errors encountered
      return@withContext Plant(
          image = path, description = "There was an error getting the plant latin name.")
    }
  }

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
  suspend fun plantNetAPICall(client: okhttp3.OkHttpClient, request: okhttp3.Request): String {
    val response = client.newCall(request).execute()
    return response.body?.string() ?: ""
  }

  /**
   * Identifies a plant from an image using recognition technology.
   *
   * This function analyzes an image and returns plant information, including name, latin name,
   * description, and care requirements.
   *
   * @param path The file path of the image of the plant to identify
   * @return A Plant object containing the identified plant's information
   */
  suspend fun identifyPlant(path: String?): Plant {
    val plantWLatinName = identifyLatinNameWithPlantNet(path)
    if (plantWLatinName.latinName == Plant().latinName) {
      Log.d("plantNetApiLog", "PlantNet API call failed)")
      return plantWLatinName
    }
    return generatePlantWithAI(plantWLatinName)
  }

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
