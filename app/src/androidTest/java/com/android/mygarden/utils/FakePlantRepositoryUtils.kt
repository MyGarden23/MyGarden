package com.android.mygarden.utils

import com.android.mygarden.model.plant.Plant
import com.android.mygarden.model.plant.PlantsRepository
import com.android.mygarden.model.plant.PlantsRepositoryFirestore
import com.android.mygarden.model.plant.PlantsRepositoryLocal
import com.android.mygarden.model.plant.PlantsRepositoryProvider
import io.mockk.coEvery
import io.mockk.spyk

/**
 * Enumeration of available plant repository types for testing.
 *
 * This enum is used to specify which repository implementation should be instantiated and spied
 * upon in [FakePlantRepositoryUtils].
 */
enum class PlantRepositoryType {
  /**
   * Local repository implementation that stores plant data locally on the device.
   *
   * @see PlantsRepositoryLocal
   */
  PlantRepoLocal,

  /**
   * Firestore repository implementation that stores plant data in Firebase Firestore.
   *
   * @see PlantsRepositoryFirestore
   */
  PlantRepoFirestore
}

/**
 * Utility class for creating and configuring fake plant repositories in Android instrumented tests.
 *
 * This class provides a spy wrapper around either [PlantsRepositoryLocal] or
 * [PlantsRepositoryFirestore] and helper methods to mock API responses for testing purposes. It
 * allows tests to control the behavior of repository methods without making actual network calls.
 *
 * ## Usage Example
 *
 * ```kotlin
 * // Create a fake repository for local storage testing
 * val fakeRepoUtil = FakePlantRepositoryUtils(PlantRepositoryType.PlantRepoLocal)
 *
 * // Change the repo used in the provider to use he mockRepo
 * fakeRepoUtil.setUpMockRepo
 *
 * // Mock PlantNet API response
 * fakeRepoUtil.mockPlantNetAPI(customJsonResponse)
 *
 * // Use the repository in tests
 * val result = PlantRepositoryProvider.repository.plantNetAPICall(image, organ)
 * ```
 *
 * @param repoType The type of repository to create and spy upon, specified using
 *   [PlantRepositoryType]
 * @property repository A MockK spy of the selected repository implementation
 *   ([PlantsRepositoryLocal] or [PlantsRepositoryFirestore]) that can be configured to return
 *   predetermined responses for testing scenarios.
 * @throws IllegalArgumentException If an unsupported repository type is provided (should never
 *   occur with the current enum values)
 */
class FakePlantRepositoryUtils(repoType: PlantRepositoryType) {
  private val repository: PlantsRepository =
      when (repoType) {
        PlantRepositoryType.PlantRepoLocal -> spyk(PlantsRepositoryLocal())
        PlantRepositoryType.PlantRepoFirestore -> spyk(PlantsRepositoryFirestore())
        else -> throw IllegalArgumentException("Unsupported repository type")
      }

  /**
   * A valid JSON response string from the PlantNet API for testing purposes.
   *
   * This mock response represents a successful plant identification for a tomato plant (Solanum
   * lycopersicum L.). The response includes:
   * - Query details with image identifier and organ detection
   * - Predicted organ (fruit) with confidence score
   * - Best match: "Solanum lycopersicum L." (Garden tomato)
   * - Multiple species results with scores and taxonomic information
   * - Common names and external database references (GBIF, POWO)
   *
   * Used as default test data for [plantNetValidRes].
   */
  private val validResPlantNetString =
      "{\"query\":{\"project\":\"all\",\"images\":[\"d2f2f1bfaa22ff774fff7251c23a97fd\"],\"organs\":[\"auto\"],\"includeRelatedImages\":false,\"noReject\":false,\"type\":null},\"predictedOrgans\":[{\"image\":\"d2f2f1bfaa22ff774fff7251c23a97fd\",\"filename\":\"tomato_test.jpg\",\"organ\":\"fruit\",\"score\":0.65548}],\"language\":\"en\",\"preferedReferential\":\"useful\",\"bestMatch\":\"Solanum lycopersicum L.\",\"results\":[{\"score\":0.8359,\"species\":{\"scientificNameWithoutAuthor\":\"Solanum lycopersicum\",\"scientificNameAuthorship\":\"L.\",\"genus\":{\"scientificNameWithoutAuthor\":\"Solanum\",\"scientificNameAuthorship\":\"\",\"scientificName\":\"Solanum\"},\"family\":{\"scientificNameWithoutAuthor\":\"Solanaceae\",\"scientificNameAuthorship\":\"\",\"scientificName\":\"Solanaceae\"},\"commonNames\":[\"Garden tomato\",\"Cherry Tomato\",\"Tomato\"],\"scientificName\":\"Solanum lycopersicum L.\"},\"gbif\":{\"id\":\"2930137\"},\"powo\":{\"id\":\"316947-2\"}},{\"score\":0.07904,\"species\":{\"scientificNameWithoutAuthor\":\"Solanum pimpinellifolium\",\"scientificNameAuthorship\":\"L.\",\"genus\":{\"scientificNameWithoutAuthor\":\"Solanum\",\"scientificNameAuthorship\":\"\",\"scientificName\":\"Solanum\"},\"family\":{\"scientificNameWithoutAuthor\":\"Solanaceae\",\"scientificNameAuthorship\":\"\",\"scientificName\":\"Solanaceae\"},\"commonNames\":[\"Currant tomato\",\"Cherry Tomato\",\"Pimp\"],\"scientificName\":\"Solanum pimpinellifolium L.\"},\"gbif\":{\"id\":\"2931738\"},\"powo\":{\"id\":\"820511-1\"},\"iucn\":{\"id\":\"66836393\",\"category\":\"LC\"}}],\"version\":\"2025-08-08 (7.4)\",\"remainingIdentificationRequests\":498}"

  /**
   * A pre-configured valid PlantNet API response for testing.
   *
   * This property contains a mocked response using [validResPlantNetString] and can be used
   * directly in tests that need a successful PlantNet API call result.
   */
  val plantNetValidRes = mockPlantNetAPI(validResPlantNetString)

  /**
   * Sets up the mock repository as the global repository instance.
   *
   * This method configures [PlantsRepositoryProvider] to use the spied repository instance,
   * allowing tests to control repository behavior throughout the application. This is particularly
   * useful for integration tests where multiple components need to interact with the same mocked
   * repository instance.
   *
   * **Important:** This method should be called in the test setup (e.g., `@Before` method) to
   * ensure the mock repository is properly configured before test execution. Consider resetting the
   * provider after tests to avoid state leakage between test cases.
   *
   * ## Usage Example
   *
   * ```kotlin
   * @Before
   * fun setup() {
   *     fakeRepo = FakePlantRepositoryUtils(PlantRepositoryType.PlantRepoLocal)
   *     fakeRepo.setUpMockRepo()
   * }
   * ```
   *
   * @see PlantsRepositoryProvider
   */
  fun setUpMockRepo() {
    PlantsRepositoryProvider.repository = repository
  }

  /**
   * Mocks the identifyPlant API call to return a predetermined Plant response.
   *
   * This method configures the repository spy to return the specified Plant when
   * [PlantsRepository.identifyPlant] is called. The path parameter passed to identifyPlant will be
   * automatically captured and used in the returned Plant's image field.
   *
   * @param plant The Plant object to return from the mocked API call. The image field will be
   *   overridden with the actual path parameter from the call.
   * @see PlantsRepository.identifyPlant
   */
  fun mockIdentifyPlant(plant: Plant) {
    coEvery { repository.identifyPlant(any()) } answers
        {
          val path = firstArg<String?>()
          plant.copy(image = path)
        }
  }

  /**
   * Mocks the Gemini AI plant description API call to return a predetermined JSON response.
   *
   * This method configures the repository spy to return the specified JSON string when
   * [PlantsRepository.plantDescriptionCallGemini] is called with any parameter. This allows tests
   * to verify behavior without making actual API calls to the Gemini service.
   *
   * @param resJSON The JSON response string to return from the mocked API call
   * @return The result of the test coroutine execution
   * @see PlantsRepository.plantDescriptionCallGemini
   */
  fun mockPlantDescriptionCallGemini(resJSON: String) {
    coEvery { repository.plantDescriptionCallGemini(any()) } returns resJSON
  }

  /**
   * Mocks the PlantNet API call to return a predetermined JSON response.
   *
   * This method configures the repository spy to return the specified JSON string when
   * [PlantsRepository.plantNetAPICall] is called with any parameters. This enables testing of plant
   * identification features without requiring network connectivity or consuming API quota.
   *
   * @param resJSON The JSON response string to return from the mocked API call
   * @return The result of the test coroutine execution
   * @see PlantsRepository.plantNetAPICall
   */
  fun mockPlantNetAPI(resJSON: String) {
    coEvery { repository.plantNetAPICall(any(), any()) } returns resJSON
  }
}
