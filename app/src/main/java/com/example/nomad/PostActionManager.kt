// PostActionManager.kt (New File)
package com.example.nomad

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class PostActionManager(private val context: Context) {
    private val TAG = "PostActionManager"
    private val BASE_URL = "http://192.168.100.15/nomad_api" // Use your correct IP

    suspend fun deleteObservation(observationId: Int, userId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/delete_observation.php")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("observation_id", observationId)
                    put("user_id", userId) // Used for security check on server
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val jsonResponse = JSONObject(response)
                return@withContext jsonResponse.getBoolean("success")

            } catch (e: Exception) {
                Log.e(TAG, "Delete failed: ${e.message}", e)
                return@withContext false
            }
        }
    }

    // --- 2. TOGGLE LIKE FUNCTION (New implementation) ---
    suspend fun toggleLike(observationId: Int, userId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/toggle_like.php") // Ensure this PHP file exists
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("observation_id", observationId)
                    put("user_id", userId)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val jsonResponse = JSONObject(response)
                // The server will send back 'liked' or 'unliked' status for success
                return@withContext jsonResponse.getBoolean("success")

            } catch (e: Exception) {
                Log.e(TAG, "Toggle like failed: ${e.message}", e)
                return@withContext false
            }
        }
    }
    suspend fun updateObservation(observationId: Int, userId: Int, categoryId: Int, title: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/update_observation.php")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("observation_id", observationId)
                    put("user_id", userId) // Security check
                    put("category_id", categoryId)
                    put("title", title)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                // Read response and check success flag
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val jsonResponse = JSONObject(response)
                return@withContext jsonResponse.getBoolean("success")

            } catch (e: Exception) {
                Log.e(TAG, "Update failed: ${e.message}", e)
                return@withContext false
            }
        }
    }
}