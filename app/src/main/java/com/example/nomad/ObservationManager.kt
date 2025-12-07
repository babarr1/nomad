package com.example.nomad

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manager to fetch observation data for any category (Fauna, Verdant, Strata, Phenomena).
 * Replaces the need for FaunaManager, StrataManager, etc.
 */
class ObservationManager(private val context: Context) {
    private val TAG = "ObservationManager"

    // NOTE: Use the correct BASE_URL for your setup
    private val BASE_URL = "http://192.168.100.10/nomad_api"

    /**
     * Data class to match the observation table columns (same as FaunaItem, but generalized)
     */
    data class ObservationItem(
        // Core Identification
        val id: Int, // Maps to observation_id
        val userId: Int,
        val categoryId: Int,

        // Core Content
        val title: String,
        val description: String,
        val imageUrl: String,

        // Location Data (MUST be included as they are in the JSON)
        // They are nullable because they can be NULL in the database.
        val latitude: String? = null,
        val longitude: String? = null,

        // Timestamp
        val dateObserved: String? = null // Maps to created_at
    )

    /**
     * Fetches a list of observation items based on the provided category ID.
     * 1 = Fauna, 2 = Verdant, 3 = Strata, 4 = Phenomena
     */
    suspend fun fetchCategoryItems(categoryId: Int): List<ObservationItem> {
        return withContext(Dispatchers.IO) {
            try {
                // The endpoint is now dynamic based on category ID
                val url = URL("$BASE_URL/get_category_items.php?category_id=$categoryId")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode

                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                connection.disconnect()
                Log.d(TAG, "Response for category $categoryId: $response")

                val jsonResponse = JSONObject(response)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val itemsArray = jsonResponse.getJSONArray("items")

                    val itemList = mutableListOf<ObservationItem>()
                    for (i in 0 until itemsArray.length()) {
                        val itemObj = itemsArray.getJSONObject(i)
                        itemList.add(
                            ObservationItem(
                                id = itemObj.getInt("observation_id"),
                                userId = itemObj.getInt("user_id"),
                                categoryId = itemObj.getInt("category_id"),
                                title = itemObj.getString("title"),
                                description = itemObj.getString("description"),
                                imageUrl = itemObj.getString("image_url"),
                                latitude = itemObj.optString("latitude", null),
                                longitude = itemObj.optString("longitude", null),
                                dateObserved = itemObj.optString("created_at", null)
                            )
                        )
                    }
                    itemList
                } else {
                    emptyList()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Fetch error for category $categoryId: ${e.message}", e)
                emptyList()
            }
        }
    }
    suspend fun fetchSingleObservation(observationId: Int): ObservationItem? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/get_single_observation.php?observation_id=$observationId")
                val connection = url.openConnection() as HttpURLConnection

                // ... (setup connection properties: GET, timeouts) ...
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val jsonResponse = JSONObject(response)
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val itemObj = jsonResponse.getJSONObject("data") // Note: The PHP returns 'data' as an object

                    return@withContext ObservationItem(
                        id = itemObj.getInt("observation_id"),
                        userId = itemObj.getInt("user_id"),
                        categoryId = itemObj.getInt("category_id"),
                        title = itemObj.getString("title"),
                        description = itemObj.getString("description"),
                        imageUrl = itemObj.getString("image_url"),
                        latitude = itemObj.optString("latitude", null),
                        longitude = itemObj.optString("longitude", null),
                        dateObserved = itemObj.optString("created_at", null)
                    )
                } else {
                    null
                }

            } catch (e: Exception) {
                Log.e(TAG, "Fetch single post error: ${e.message}", e)
                null
            }
        }
    }
}