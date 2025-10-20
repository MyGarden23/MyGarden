package com.android.mygarden.model.plant

import android.media.Image
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerativeBackend
import java.sql.Timestamp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository interface for managing plants in the application.
 *
 * This repository handles both general plant information retrieval and the management of the user's
 * virtual garden.
 */
interface PlantsRepository {

  companion object {
    const val SCORE_THRESHOLD = 0.5
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
  fun imageFileToByteArray(path: String): ByteArray {
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
   * @param plantName The name of the plant to generate information for
   * @param basePlant The base Plant object to use for default values and structure
   * @return A Plant object containing the AI-generated information
   */
  suspend fun generatePlantWithAI(plantName: String, basePlant: Plant): Plant {
    val model =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel("gemini-2.5-flash")
    val prompt =
        "Reply ONLY with a valid JSON object as plain text, with no markdown blocks or extra explanation. The response must start with { and end with }. Describe the plant '$plantName'. The object should include: name, latinName, description, wateringFrequency. 'wateringFrequency' must be an integer representing the number of days between waterings."
    val response: GenerateContentResponse = model.generateContent(prompt)

    // Robustly parse AI output
    val jsonString =
        try {
          val outputStr = response.text ?: "{}"
          if (outputStr.trim().startsWith("{")) {
            // Direct JSON
            outputStr
          } else if (outputStr.trim().startsWith("[")) {
            // Parse array
            val arr = JSONArray(outputStr)
            val obj = arr.getJSONObject(0)
            val msgObj = obj.getJSONObject("message")
            val contentArr = msgObj.getJSONArray("content")
            val textBlock = contentArr.getJSONObject(0).getString("text")
            textBlock.replace("```json", "").replace("```", "").trim()
          } else {
            // Try to just remove markdown explicitly
            outputStr.replace("```json", "").replace("```", "").trim()
          }
        } catch (e: Exception) {
          Log.d("plantGeneratingLog", e.toString())
          "{}"
        }

    val newPlant =
        try {
          val jsonObj = JSONObject(jsonString)
          basePlant.copy(
              name = jsonObj.optString("name", plantName),
              latinName = jsonObj.optString("latinName", ""),
              description = jsonObj.optString("description", ""),
              wateringFrequency = jsonObj.optInt("wateringFrequency", basePlant.wateringFrequency))
        } catch (e: Exception) {
          Log.d("plantGeneratingLog", e.toString())
          basePlant.copy(latinName = "Error while generating plant")
        }
    return newPlant
  }

  /**
   * Identifies a plant from an image using recognition technology.
   *
   * This function analyzes an image and returns plant information, including name, latin name,
   * description, and care requirements.
   *
   * @param image The image of the plant to identify
   * @return A Plant object containing the identified plant's information
   */
  suspend fun identifyPlant(image: Image): Plant

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
  suspend fun identifyLatinNameWithPlantNet(path: String): Plant {
    // Read image file into byte array
    val imageBytes = imageFileToByteArray(path)

    // Initialize API client and API key
    val client = okhttp3.OkHttpClient()
    val apiKey = "2b10vR85MRLoZFey45nBR41LRO"
    val url = "https://my-api.plantnet.org/v2/identify/all?api-key=$apiKey"

    // If image cannot be read, return a Plant with error description
    if (imageBytes.isEmpty()) {
      println("plantNetApiLog: Image file at $path could not be read or is empty.")
      return Plant(image = path, description = "Could not be read the image or is empty.")
    }

    // Build multipart request for image upload and API call
    val requestBody =
        okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart(
                "images",
                java.io.File(path).name,
                okhttp3.RequestBody.create("image/jpeg".toMediaType(), imageBytes))
            .addFormDataPart("organs", "auto")
            .build()

    val request = okhttp3.Request.Builder().url(url).post(requestBody).build()

    // handle response
    try {
      // Execute the API request
      val response = client.newCall(request).execute()
      val rawBody = response.body?.string() ?: ""

      // Parse API response JSON
      val json = Json.parseToJsonElement(rawBody)

      // Get first result from PlantNet and its confidence score
      val firstResult = json.jsonObject["results"]?.jsonArray?.first()

      val score = firstResult?.jsonObject?.get("score")?.jsonPrimitive?.doubleOrNull ?: 0.0
      if (score < SCORE_THRESHOLD) {
        // Low confidence: let user know identification failed
        return Plant(image = path, description = "The AI was not able to identify the plant.")
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

      return Plant(latinName = latinName, image = path)
    } catch (e: Exception) {
      // Log and handle API/network errors
      println("plantNetApiLog: Error during PlantNet API call: $e")
    }
    // Fallback for any errors encountered
    return Plant(image = path, description = "There was an error getting the plant informations.")
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
