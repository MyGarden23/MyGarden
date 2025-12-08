package com.android.mygarden.model.plant

import android.util.Log
import com.android.mygarden.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineDispatcher
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
abstract class PlantsRepositoryBase(private val dispatcher: CoroutineDispatcher = Dispatchers.IO) :
    PlantsRepository {

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

    // HTTP client timeout values
    private const val HTTP_TIMEOUT_SECONDS = 30L

    // API request constants
    private const val FORM_DATA_PART_IMAGES = "images"
    private const val FORM_DATA_PART_ORGANS = "organs"
    private const val IMAGE_FILENAME = "PlantImage.jpg"
    private const val IMAGE_MEDIA_TYPE = "image/jpeg"
    private const val ORGAN_TYPE_AUTO = "auto"

    // JSON field names
    private const val JSON_FIELD_LOCATION = "location"
    private const val JSON_FIELD_NAME = "name"
    private const val JSON_FIELD_DESCRIPTION = "description"
    private const val JSON_FIELD_LIGHT_EXPOSURE = "lightExposure"
    private const val JSON_FIELD_WATERING_FREQUENCY = "wateringFrequency"
    private const val JSON_FIELD_RESULTS = "results"
    private const val JSON_FIELD_SCORE = "score"
    private const val JSON_FIELD_SPECIES = "species"
    private const val JSON_FIELD_SCIENTIFIC_NAME = "scientificNameWithoutAuthor"

    // Default/fallback values
    private const val DEFAULT_LOCATION = "UNKNOWN"
    private const val DEFAULT_SCORE = 0.0

    // Error messages
    private const val ERROR_IMAGE_EMPTY = "Could not be read the image or is empty."
    private const val ERROR_AI_NO_IDENTIFICATION = "The AI was not able to identify the plant."
    private const val ERROR_LATIN_NAME_FAILED = "There was an error getting the plant latin name."
    private const val ERROR_AI_NO_DESCRIPTION =
        "The AI couldn't generate a description for this plant."
    private const val ERROR_GENERATING_DETAILS = "Error while generating plant details"
    private const val ERROR_CARE_TIPS_FALLBACK = "Unable to generate care tips at this time."

    // Log tags
    private const val LOG_TAG_PLANT_GENERATING = "plantGeneratingLog"
    private const val LOG_TAG_PLANTNET_API = "plantNetApiLog"
    private const val LOG_TAG_CARE_TIPS = "plantCareTipsLog"

    // AI model name
    private const val GEMINI_MODEL_NAME = "gemini-2.5-flash"

    /**
     * HTTP client configured for PlantNet API requests.
     *
     * Configured with extended timeouts (30 seconds) to accommodate image upload and processing.
     */
    val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
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
              FORM_DATA_PART_IMAGES,
              IMAGE_FILENAME,
              imageBytes.toRequestBody(IMAGE_MEDIA_TYPE.toMediaType(), 0, imageBytes.size))
          .addFormDataPart(FORM_DATA_PART_ORGANS, ORGAN_TYPE_AUTO)
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
          Log.d(LOG_TAG_PLANT_GENERATING, e.toString())
          return basePlant.copy(description = ERROR_AI_NO_DESCRIPTION)
        }

    if (outputStr.isEmpty()) return basePlant.copy(description = ERROR_GENERATING_DETAILS)

    try {
      val jsonObject = Json.parseToJsonElement(outputStr).jsonObject
      val location =
          jsonObject[JSON_FIELD_LOCATION]?.jsonPrimitive?.content?.uppercase() ?: DEFAULT_LOCATION
      val plantLocation =
          try {
            PlantLocation.valueOf(location)
          } catch (_: IllegalArgumentException) {
            PlantLocation.UNKNOWN
          }
      val res =
          basePlant.copy(
              name = jsonObject[JSON_FIELD_NAME]?.jsonPrimitive?.content ?: basePlant.name,
              description =
                  jsonObject[JSON_FIELD_DESCRIPTION]?.jsonPrimitive?.content
                      ?: basePlant.description,
              location = plantLocation,
              lightExposure =
                  jsonObject[JSON_FIELD_LIGHT_EXPOSURE]?.jsonPrimitive?.content
                      ?: basePlant.lightExposure,
              wateringFrequency =
                  jsonObject[JSON_FIELD_WATERING_FREQUENCY]?.jsonPrimitive?.doubleOrNull?.toInt()
                      ?: basePlant.wateringFrequency,
              isRecognized = true)

      return res
    } catch (e: Exception) {
      Log.d(LOG_TAG_PLANT_GENERATING, e.toString())
      return basePlant.copy(description = ERROR_GENERATING_DETAILS)
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
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(GEMINI_MODEL_NAME)
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
        return@withContext Plant(image = path, description = ERROR_IMAGE_EMPTY)
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
        val firstResult = json.jsonObject[JSON_FIELD_RESULTS]?.jsonArray?.first()

        val score =
            firstResult?.jsonObject?.get(JSON_FIELD_SCORE)?.jsonPrimitive?.doubleOrNull
                ?: DEFAULT_SCORE
        if (score < SCORE_THRESHOLD) {
          // Low confidence: let user know identification failed
          return@withContext Plant(image = path, description = ERROR_AI_NO_IDENTIFICATION)
        }

        // Extract Latin name from response JSON
        val latinName =
            firstResult
                ?.jsonObject
                ?.get(JSON_FIELD_SPECIES)
                ?.jsonObject
                ?.get(JSON_FIELD_SCIENTIFIC_NAME)
                ?.jsonPrimitive
                ?.content ?: "Unknown"

        return@withContext Plant(latinName = latinName, image = path)
      } catch (e: Exception) {
        // Log and handle API/network errors
        Log.e(LOG_TAG_PLANTNET_API, "Error while calling PlantNet API: ", e)
      }
      // Fallback for any errors encountered
      return@withContext Plant(image = path, description = ERROR_LATIN_NAME_FAILED)
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
      Log.d(LOG_TAG_PLANTNET_API, "PlantNet API call failed)")
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
      Log.e(LOG_TAG_CARE_TIPS, "Error while generating care tips for $latinName", e)
      return ERROR_CARE_TIPS_FALLBACK
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
