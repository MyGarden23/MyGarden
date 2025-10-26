package com.android.mygarden.model.plant

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Test class for PlantsRepository using mocks to avoid calling real APIs.
 *
 * This approach allows testing the logic without consuming API tokens or making network calls.
 */
class PlantsRepositoryMockTest {
  val imagePath: String =
      "C:/Users/matte/Desktop/MyGarden/app/src/test/resources/plantimage/tomato_test.jpg"
  val resultPlantNetAPICall: String =
      "{\"query\":{\"project\":\"all\",\"images\":[\"d2f2f1bfaa22ff774fff7251c23a97fd\"],\"organs\":[\"auto\"],\"includeRelatedImages\":false,\"noReject\":false,\"type\":null},\"predictedOrgans\":[{\"image\":\"d2f2f1bfaa22ff774fff7251c23a97fd\",\"filename\":\"tomato_test.jpg\",\"organ\":\"fruit\",\"score\":0.65548}],\"language\":\"en\",\"preferedReferential\":\"useful\",\"bestMatch\":\"Solanum lycopersicum L.\",\"results\":[{\"score\":0.8359,\"species\":{\"scientificNameWithoutAuthor\":\"Solanum lycopersicum\",\"scientificNameAuthorship\":\"L.\",\"genus\":{\"scientificNameWithoutAuthor\":\"Solanum\",\"scientificNameAuthorship\":\"\",\"scientificName\":\"Solanum\"},\"family\":{\"scientificNameWithoutAuthor\":\"Solanaceae\",\"scientificNameAuthorship\":\"\",\"scientificName\":\"Solanaceae\"},\"commonNames\":[\"Garden tomato\",\"Cherry Tomato\",\"Tomato\"],\"scientificName\":\"Solanum lycopersicum L.\"},\"gbif\":{\"id\":\"2930137\"},\"powo\":{\"id\":\"316947-2\"}},{\"score\":0.07904,\"species\":{\"scientificNameWithoutAuthor\":\"Solanum pimpinellifolium\",\"scientificNameAuthorship\":\"L.\",\"genus\":{\"scientificNameWithoutAuthor\":\"Solanum\",\"scientificNameAuthorship\":\"\",\"scientificName\":\"Solanum\"},\"family\":{\"scientificNameWithoutAuthor\":\"Solanaceae\",\"scientificNameAuthorship\":\"\",\"scientificName\":\"Solanaceae\"},\"commonNames\":[\"Currant tomato\",\"Cherry Tomato\",\"Pimp\"],\"scientificName\":\"Solanum pimpinellifolium L.\"},\"gbif\":{\"id\":\"2931738\"},\"powo\":{\"id\":\"820511-1\"},\"iucn\":{\"id\":\"66836393\",\"category\":\"LC\"}}],\"version\":\"2025-08-08 (7.4)\",\"remainingIdentificationRequests\":498}"

  val resultPlantGeminiCall: String =
      "{\"name\": \"Tomato\", \"latinName\": \"Solanum lycopersicum\", \"description\": \"A widely cultivated flowering plant in the nightshade family, known for its edible fruit which is botanically a berry but culinarily used as a vegetable. It grows as a sprawling, vining plant and requires warm temperatures and ample sunlight.\", \"wateringFrequency\": 2}"

  val lowScorePlantNetAPICall: String =
      "{\"results\":[{\"score\":0.15,\"species\":{\"scientificNameWithoutAuthor\":\"Unknown Plant\"}}]}"

  val invalidJsonGeminiCall: String = "This is not a valid JSON response"

  val incompleteGeminiCall: String = "{\"name\": \"Rose\", \"description\": \"A beautiful flower\"}"

  private lateinit var spyRepository: PlantsRepository

  @Before
  fun setup() {
    // Create a spy of PlantsRepositoryLocal to use real methods by default
    spyRepository = spy(PlantsRepositoryLocal())
  }

  // Helper function to create a test plant
  private fun createTestPlant(
      name: String = "Test Plant",
      latinName: String = "Testus Plantus",
      description: String = "Test description",
      wateringFrequency: Int = 7
  ): Plant {
    return Plant(
        name = name,
        image = null,
        latinName = latinName,
        description = description,
        healthStatus = PlantHealthStatus.HEALTHY,
        healthStatusDescription = PlantHealthStatus.HEALTHY.description,
        wateringFrequency = wateringFrequency)
  }

  // ==================== Tests for identifyLatinNameWithPlantNet ====================

  @Test
  fun identifyLatinNameWithPlantNet_returnsPlantWithLatinName_whenSuccessful() = runTest {
    // Arrange: Mock only the API call to avoid real network requests
    doReturn(resultPlantNetAPICall).whenever(spyRepository).plantNetAPICall(any(), any())

    // Act: Call the function - it will use real methods except for the API call
    val result = spyRepository.identifyLatinNameWithPlantNet(imagePath)

    // Assert: Verify the results
    assertNotNull(result)
    assertEquals("Solanum lycopersicum", result.latinName)
    assertEquals(imagePath, result.image)

    // Verify that the API call was made once
    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
  }

  @Test
  fun identifyLatinNameWithPlantNet_returnsErrorDescription_whenScoreBelowThreshold() = runTest {
    // Arrange: Mock API to return low confidence score
    doReturn(lowScorePlantNetAPICall).whenever(spyRepository).plantNetAPICall(any(), any())

    // Act
    val result = spyRepository.identifyLatinNameWithPlantNet(imagePath)

    // Assert: Should return error message when confidence is low
    assertNotNull(result)
    assertEquals(imagePath, result.image)
    assertEquals("The AI was not able to identify the plant.", result.description)

    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
  }

  @Test
  fun identifyLatinNameWithPlantNet_returnsErrorDescription_whenImagePathIsNull() = runTest {
    // Act: Call with null image path
    val result = spyRepository.identifyLatinNameWithPlantNet(null)

    // Assert: Should return error message
    assertNotNull(result)
    assertNull(result.image)
    assertEquals("Could not be read the image or is empty.", result.description)

    // Verify that the API call was never made
    verify(spyRepository, never()).plantNetAPICall(any(), any())
  }

  @Test
  fun identifyLatinNameWithPlantNet_returnsErrorDescription_whenImageFileDoesNotExist() = runTest {
    // Arrange: Use a non-existent file path
    val nonExistentPath = "C:/path/to/nonexistent/image.jpg"

    // Act
    val result = spyRepository.identifyLatinNameWithPlantNet(nonExistentPath)

    // Assert: Should return error message when file doesn't exist
    assertNotNull(result)
    assertEquals(nonExistentPath, result.image)
    assertEquals("Could not be read the image or is empty.", result.description)

    // Verify that the API call was never made
    verify(spyRepository, never()).plantNetAPICall(any(), any())
  }

  @Test
  fun identifyLatinNameWithPlantNet_returnsErrorDescription_whenAPIThrowsException() = runTest {
    // Arrange: Mock API to throw an exception
    doThrow(RuntimeException("Network error")).whenever(spyRepository).plantNetAPICall(any(), any())

    // Act
    val result = spyRepository.identifyLatinNameWithPlantNet(imagePath)

    // Assert: Should handle exception gracefully
    assertNotNull(result)
    assertEquals(imagePath, result.image)
    assertEquals("There was an error getting the plant latin name.", result.description)

    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
  }

  @Test
  fun identifyLatinNameWithPlantNet_returnsErrorDescription_whenAPIReturnsEmptyResults() = runTest {
    // Arrange: Mock API to return empty results
    val emptyResultsResponse = "{\"results\":[]}"
    doReturn(emptyResultsResponse).whenever(spyRepository).plantNetAPICall(any(), any())

    // Act
    val result = spyRepository.identifyLatinNameWithPlantNet(imagePath)

    // Assert: Should handle empty results gracefully
    assertNotNull(result)
    assertEquals(imagePath, result.image)
    assertEquals("There was an error getting the plant latin name.", result.description)

    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
  }

  @Test
  fun identifyLatinNameWithPlantNet_returnsUnknown_whenLatinNameMissingInResponse() = runTest {
    // Arrange: Mock API to return response without scientificNameWithoutAuthor
    val responseWithoutLatinName =
        "{\"results\":[{\"score\":0.85,\"species\":{\"commonNames\":[\"Unknown\"]}}]}"
    doReturn(responseWithoutLatinName).whenever(spyRepository).plantNetAPICall(any(), any())

    // Act
    val result = spyRepository.identifyLatinNameWithPlantNet(imagePath)

    // Assert: Should return "Unknown" when latin name is missing
    assertNotNull(result)
    assertEquals("Unknown", result.latinName)
    assertEquals(imagePath, result.image)

    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
  }

  @Test
  fun identifyLatinNameWithPlantNet_checksScoreThreshold_correctly() = runTest {
    // Arrange: Test with score exactly at threshold (0.3)
    val thresholdResponse =
        "{\"results\":[{\"score\":0.3,\"species\":{\"scientificNameWithoutAuthor\":\"Threshold Plant\"}}]}"
    doReturn(thresholdResponse).whenever(spyRepository).plantNetAPICall(any(), any())

    // Act
    val result = spyRepository.identifyLatinNameWithPlantNet(imagePath)

    // Assert: Score of 0.3 should be accepted (not below threshold)
    assertNotNull(result)
    assertEquals("Threshold Plant", result.latinName)
    assertNotEquals("The AI was not able to identify the plant.", result.description)

    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
  }

  @Test
  fun identifyLatinNameWithPlantNet_preservesImagePath_inAllScenarios() = runTest {
    // Arrange
    doReturn(resultPlantNetAPICall).whenever(spyRepository).plantNetAPICall(any(), any())

    // Act
    val result = spyRepository.identifyLatinNameWithPlantNet(imagePath)

    // Assert: Image path should always be preserved
    assertEquals(imagePath, result.image)

    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
  }

  // ==================== Tests for generatePlantWithAI ====================

  @Test
  fun generatePlantWithAI_returnsPlantWithAllFields_whenSuccessful() = runTest {
    // Arrange: Mock Gemini API call
    doReturn(resultPlantGeminiCall).whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Solanum lycopersicum", image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Verify all fields are correctly populated
    assertNotNull(result)
    assertEquals("Tomato", result.name)
    assertEquals("Solanum lycopersicum", result.latinName)
    assertTrue(result.description.contains("nightshade family"))
    assertEquals(2, result.wateringFrequency)

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_returnsErrorDescription_whenGeminiReturnsEmptyString() = runTest {
    // Arrange: Mock Gemini to return empty string
    doReturn("").whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Test Plant", image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should return error description
    assertNotNull(result)
    assertEquals("Error while generating plant details", result.description)

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_returnsErrorDescription_whenGeminiReturnsInvalidJSON() = runTest {
    // Arrange: Mock Gemini to return invalid JSON
    doReturn(invalidJsonGeminiCall).whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Rosa", name = "Rose", image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should handle invalid JSON gracefully
    assertNotNull(result)
    assertEquals("Error while generating plant details", result.description)

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_usesDefaultValues_whenFieldsMissingInResponse() = runTest {
    // Arrange: Mock Gemini to return incomplete JSON
    doReturn(incompleteGeminiCall).whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant =
        Plant(
            latinName = "Rosa rubiginosa",
            name = "Sweet Briar Rose",
            description = "Default description",
            wateringFrequency = 5,
            image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should use values from incomplete response and fallback to defaults
    assertNotNull(result)
    assertEquals("Rose", result.name) // From incomplete JSON
    assertEquals("A beautiful flower", result.description) // From incomplete JSON
    assertEquals("Rosa rubiginosa", result.latinName) // From basePlant (not in response)
    assertEquals(5, result.wateringFrequency) // From basePlant (not in response)

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_preservesBasePlantData_whenNotInResponse() = runTest {
    // Arrange
    val geminiResponseWithoutName = "{\"description\": \"A test plant\", \"wateringFrequency\": 3}"
    doReturn(geminiResponseWithoutName).whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant =
        Plant(
            latinName = "Plantus testus",
            name = "Test Plant",
            description = "Original description",
            wateringFrequency = 10,
            image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should preserve base plant data when not in response
    assertNotNull(result)
    assertEquals("Test Plant", result.name) // Preserved from basePlant
    assertEquals("Plantus testus", result.latinName) // Preserved from basePlant
    assertEquals("A test plant", result.description) // From Gemini
    assertEquals(3, result.wateringFrequency) // From Gemini

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_handlesNullWateringFrequency_correctly() = runTest {
    // Arrange: Response with null wateringFrequency
    val responseWithNullWatering =
        "{\"name\": \"Cactus\", \"latinName\": \"Cactaceae\", \"description\": \"Desert plant\", \"wateringFrequency\": null}"
    doReturn(responseWithNullWatering).whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Cactaceae", wateringFrequency = 14, image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should use base plant's watering frequency when null
    assertNotNull(result)
    assertEquals(14, result.wateringFrequency) // From basePlant

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_convertsDoubleToInt_forWateringFrequency() = runTest {
    // Arrange: Response with double wateringFrequency
    val responseWithDoubleWatering =
        "{\"name\": \"Orchid\", \"latinName\": \"Orchidaceae\", \"description\": \"Elegant flower\", \"wateringFrequency\": 4.7}"
    doReturn(responseWithDoubleWatering).whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Orchidaceae", image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should convert 4.7 to 4
    assertNotNull(result)
    assertEquals(4, result.wateringFrequency)

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_createsCorrectPrompt_withLatinName() = runTest {
    // Arrange
    doReturn(resultPlantGeminiCall).whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Solanum lycopersicum", image = imagePath)

    // Act
    spyRepository.generatePlantWithAI(basePlant)

    // Assert: Verify the prompt contains the latin name
    verify(spyRepository, times(1))
        .plantDescriptionCallGemini(
            argThat {
              contains("Solanum lycopersicum") &&
                  contains("JSON") &&
                  contains("name") &&
                  contains("latinName") &&
                  contains("description") &&
                  contains("wateringFrequency")
            })
  }

  @Test
  fun generatePlantWithAI_returnsErrorDescription_whenExceptionThrown() = runTest {
    // Arrange: Mock Gemini to throw exception
    doThrow(RuntimeException("AI Service error"))
        .whenever(spyRepository)
        .plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Test", image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should handle exception gracefully
    assertNotNull(result)
    assertEquals("The AI couldn't generate a description for this plant.", result.description)

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_handlesStringWateringFrequency_gracefully() = runTest {
    // Arrange: Response with string instead of number for wateringFrequency
    val responseWithStringWatering =
        "{\"name\": \"Fern\", \"latinName\": \"Pteridium\", \"description\": \"Green plant\", \"wateringFrequency\": \"every week\"}"
    doReturn(responseWithStringWatering).whenever(spyRepository).plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Pteridium", wateringFrequency = 7, image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should use base plant's value when conversion fails
    assertNotNull(result)
    assertEquals(7, result.wateringFrequency) // From basePlant

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_preservesImagePath_fromBasePlant() = runTest {
    // Arrange
    doReturn(resultPlantGeminiCall).whenever(spyRepository).plantDescriptionCallGemini(any())

    val testImagePath = "C:/test/image/path.jpg"
    val basePlant = Plant(latinName = "Test", image = testImagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Image path should be preserved
    assertEquals(testImagePath, result.image)

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun generatePlantWithAI_handlesComplexDescription_correctly() = runTest {
    // Arrange: Response with long, complex description
    val complexDescription =
        "A perennial herb native to Europe and Asia. It has a strong, aromatic scent and is widely used in cooking and traditional medicine. The plant grows up to 60 cm tall with purple flowers."
    val responseWithComplexDescription =
        "{\"name\": \"Lavender\", \"latinName\": \"Lavandula\", \"description\": \"$complexDescription\", \"wateringFrequency\": 7}"
    doReturn(responseWithComplexDescription)
        .whenever(spyRepository)
        .plantDescriptionCallGemini(any())

    val basePlant = Plant(latinName = "Lavandula", image = imagePath)

    // Act
    val result = spyRepository.generatePlantWithAI(basePlant)

    // Assert: Should handle complex descriptions correctly
    assertNotNull(result)
    assertEquals(complexDescription, result.description)
    assertEquals("Lavender", result.name)

    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun identifyPlant_returnsFullPlantData_whenSuccessful() = runTest {
    // Arrange: Mock both PlantNet and Gemini API calls
    doReturn(resultPlantNetAPICall).whenever(spyRepository).plantNetAPICall(any(), any())
    doReturn(resultPlantGeminiCall).whenever(spyRepository).plantDescriptionCallGemini(any())

    // Act
    val result = spyRepository.identifyPlant(imagePath)

    // Assert: Should return a complete plant with all information
    assertNotNull(result)
    assertEquals("Tomato", result.name)
    assertEquals("Solanum lycopersicum", result.latinName)
    assertTrue(result.description.contains("nightshade family"))
    assertEquals(2, result.wateringFrequency)
    assertEquals(imagePath, result.image)

    // Verify both API calls were made
    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
    verify(spyRepository, times(1)).plantDescriptionCallGemini(any())
  }

  @Test
  fun identifyPlant_returnsPartialPlant_whenPlantNetFails() = runTest {
    // Arrange: Mock PlantNet to fail with low score
    doReturn(lowScorePlantNetAPICall).whenever(spyRepository).plantNetAPICall(any(), any())

    // Act
    val result = spyRepository.identifyPlant(imagePath)

    // Assert: Should return error description and not call Gemini
    assertNotNull(result)
    assertEquals("The AI was not able to identify the plant.", result.description)

    // Verify PlantNet was called but Gemini was not
    verify(spyRepository, times(1)).plantNetAPICall(any(), any())
    verify(spyRepository, never()).plantDescriptionCallGemini(any())
  }
}
