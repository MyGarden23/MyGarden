package com.android.sample.model.plant

import android.media.Image
import java.sql.Timestamp


/**
 * Repository interface for managing plants in the application.
 *
 * This repository handles both general plant information retrieval
 * and the management of the user's virtual garden.
 */
interface PlantsRepository {

    /**
     * Identifies a plant from an image using recognition technology.
     *
     * This function analyzes an image and returns plant information,
     * including name, latin name, description, and care requirements.
     *
     * @param image The image of the plant to identify
     * @return A Plant object containing the identified plant's information
     */
    suspend fun identifyPlant(image: Image): Plant

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
     * This function converts a general Plant object into an OwnedPlant,
     * which includes tracking information like watering dates and fertilization schedule.
     *
     * @param plant The plant to add to the garden
     * @param id The unique identifier for the plant
     * @param lastWatered The timestamp of the last watering event
     * @return The newly created OwnedPlant with initialized tracking data
     */
    suspend fun saveToGarden(plant: Plant, id:String, lastWatered: Timestamp): OwnedPlant
}