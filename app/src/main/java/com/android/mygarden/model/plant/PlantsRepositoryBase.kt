package com.android.mygarden.model.plant

import android.util.Log
import com.android.mygarden.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Base class that holds the shared method bodies from the original interface. Subclasses (e.g.,
 * local or Firestore repositories) only implement persistence methods such as
 * getNewId/save/edit/delete/water/get*.
 *
 * All original comments and method bodies are preserved.
 */
abstract class PlantsRepositoryBase(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : PlantsRepository {

  // This is defined here to let View Models that use the repository Provider to have access to the
  // plantsFlow
  override val plantsFlow: StateFlow<List<OwnedPlant>> = MutableStateFlow(emptyList())

  override val plantsFlowTimeoutWhenNoSubscribers: Long
    get() = 5_000

  // This flow emits something at a fixed period of time ; to be collected by the
  // single flow plantsFlow overridden in both repositories
  override val ticks: Flow<Unit>
    get() = flow {
      while (true) {
        emit(Unit)
        delay(tickDelay)
      }
    }

  companion object {
    /**
     * Minimum confidence score threshold for plant identification results.
     *
     * Results from the PlantNet API with a score below this threshold are considered unreliable and
     * will be rejected.
     */
    const val SCORE_THRESHOLD = PlantsRepository.SCORE_THRESHOLD

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
              imageBytes.toRequestBody("image/jpeg".toMediaType(), 0, imageBytes.size))
          .addFormDataPart("organs", "auto")
          .build()
    }

    /**
     * Builds an HTTP POST request for the PlantNet API.
     *
     * @param requestBody The multipart request body containing the image data
     * @return A configured Request object ready to be executed
     */
    fun buildRequest(requestBody: okhttp3.RequestBody): Request {
      return Request.Builder().url(PLANTNET_API_URL).post(requestBody).build()
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
  override fun imageFileToByteArray(path: String?): ByteArray {
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
   * description, location, lightExposure and watering frequency. It parses the AI output and fills
   * in missing or error details as needed.
   *
   * @param basePlant The base Plant object to use for default values and structure
   * @return A Plant object containing the AI-generated information
   */
  override suspend fun generatePlantWithAI(basePlant: Plant): Plant {
    val prompt =
        """Reply ONLY with a valid JSON object as plain text, with no markdown blocks or extra explanation. 
            |The response must start with { and end with }. 
            |The formatting of the JSON MUST be a single object, NOT an array. 
            |Describe the plant by its latin name '${basePlant.latinName}'. 
            |The object should include: name, description, location, lightExposure, wateringFrequency. 
            |For 'name', use the simple/common name (e.g., 'tomato' not 'garden tomato'). 
            |'wateringFrequency' must be an integer representing the number of days between waterings. 
            |'location' must be either 'INDOOR' or 'OUTDOOR'. 
            |'lightExposure' a short recommendation for light exposure (e.g., 'Needs bright indirect light.')
            """
            .trimIndent()
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
      val location = jsonObject["location"]?.jsonPrimitive?.content?.uppercase() ?: "UNKNOWN"
      val plantLocation =
          try {
            PlantLocation.valueOf(location)
          } catch (_: IllegalArgumentException) {
            PlantLocation.UNKNOWN
          }
      val res =
          basePlant.copy(
              name = jsonObject["name"]?.jsonPrimitive?.content ?: basePlant.name,
              description =
                  jsonObject["description"]?.jsonPrimitive?.content ?: basePlant.description,
              location = plantLocation,
              lightExposure =
                  jsonObject["lightExposure"]?.jsonPrimitive?.content ?: basePlant.lightExposure,
              wateringFrequency =
                  jsonObject["wateringFrequency"]?.jsonPrimitive?.doubleOrNull?.toInt()
                      ?: basePlant.wateringFrequency,
              isRecognized = true)

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
  override suspend fun plantDescriptionCallGemini(prompt: String): String {
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
  override suspend fun identifyLatinNameWithPlantNet(path: String?): Plant {

    return withContext(dispatcher) {

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
  override suspend fun plantNetAPICall(client: OkHttpClient, request: Request): String {
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
  override suspend fun identifyPlant(path: String?): Plant {
    val plantWLatinName = identifyLatinNameWithPlantNet(path)
    if (plantWLatinName.latinName == Plant().latinName) {
      Log.d("plantNetApiLog", "PlantNet API call failed)")
      return plantWLatinName
    }
    return generatePlantWithAI(plantWLatinName)
  }

  /**
   * Generates care tips for a plant based on its latinName and health status.
   *
   * @param latinName The Latin name of the plant
   * @param healthStatus The current health status of the plant
   * @return A string containing the generated care tips
   */
  override suspend fun generateCareTips(
      latinName: String,
      healthStatus: PlantHealthStatus
  ): String {
    val prompt =
        """
        Reply ONLY with plain text advice (no markdown, no JSON).
        Give 2-3 concise, practical care tips for a $latinName plant that is currently ${healthStatus.descriptionRes}.
        Each tip should be one short sentence focused on immediate actions.
    """
            .trimIndent()

    return try {
      plantDescriptionCallGemini(prompt)
    } catch (e: Exception) {
      Log.e("plantCareTipsLog", "Error while generating care tips for $latinName", e)
      return "Unable to generate care tips at this time."
    }
  }

  /**
   * Default cleanup implementation (no-op). Subclasses should override this if they need to clean
   * up resources.
   */
  override fun cleanup() {
    // Default: do nothing. Firestore implementation will override.
  }
}
