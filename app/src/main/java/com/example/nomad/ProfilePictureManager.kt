package com.example.nomad

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles profile picture and background image upload functionality,
 * and profile text data updates.
 */
class ProfilePictureManager(private val context: Context) {
    // NOTE: This IP should match your AuthManager and other manager files (e.g., 10.0.2.2 for emulator)
    private val BASE_URL = "http://192.168.100.10/nomad_api"
    private val TAG = "ProfilePictureManager"
    private val BOUNDARY = "---" + System.currentTimeMillis() + "---"
    private val LINE_FEED = "\r\n"
    private val TWO_HYPHENS = "--"

    // --- Core Upload Logic (Reused for Profile and Background) ---

    /**
     * Executes a multipart form data upload for an image file.
     * @param endpoint The PHP file to target (e.g., upload_profile_picture.php)
     * @param userId The ID of the user performing the upload.
     * @param imageUri The local URI of the image to upload.
     * @param fileFieldName The name of the POST field for the file (e.g., "profile_picture" or "background_picture").
     */
    private suspend fun executeImageUpload(
        endpoint: String,
        userId: Int,
        imageUri: Uri,
        fileFieldName: String
    ): UploadResult {
        return withContext(Dispatchers.IO) {
            var tempFile: File? = null
            var connection: HttpURLConnection? = null

            try {
                // 1. Create a temporary file from the content URI for uploading
                val inputStream = context.contentResolver.openInputStream(imageUri)
                tempFile = createTempFile(inputStream)

                if (tempFile == null || !tempFile.exists()) {
                    return@withContext UploadResult.Error("File not found or access denied")
                }

                val url = URL("$BASE_URL/$endpoint")
                connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.doInput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val outputStream = connection.outputStream
                val writer = outputStream.bufferedWriter()

                // 2. Add user_id parameter
                addFormField(writer, "user_id", userId.toString())

                // 3. Add file part
                addFilePart(writer, outputStream, tempFile, fileFieldName)

                // 4. Close the request body
                writer.write(TWO_HYPHENS + BOUNDARY + TWO_HYPHENS + LINE_FEED)
                writer.flush()
                writer.close()
                outputStream.close()

                // 5. Read response
                val responseCode = connection.responseCode
                Log.d(TAG, "$endpoint Response code: $responseCode")

                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                // 6. Parse response
                val jsonResponse = JSONObject(response)
                val success = jsonResponse.getBoolean("success")
                val message = jsonResponse.getString("message")

                if (success) {
                    val data = jsonResponse.getJSONObject("data")
                    // The server must return a key named "profile_picture_url" or "background_picture_url"
                    val imageUrl = data.getString(fileFieldName + "_url")
                    UploadResult.Success(imageUrl)
                } else {
                    UploadResult.Error(message)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload error in $endpoint: ${e.message}", e)
                UploadResult.Error("Upload failed: ${e.message}")
            } finally {
                tempFile?.delete() // Ensure temporary file is deleted
                connection?.disconnect()
            }
        }
    }

    // --- Helper Functions for Multipart ---

    private fun addFormField(writer: BufferedWriter, fieldName: String, value: String) {
        writer.write(TWO_HYPHENS + BOUNDARY + LINE_FEED)
        writer.write("Content-Disposition: form-data; name=\"$fieldName\"$LINE_FEED")
        writer.write(LINE_FEED)
        writer.write(value)
        writer.write(LINE_FEED)
        writer.flush()
    }

    private fun addFilePart(writer: BufferedWriter, outputStream: OutputStream, file: File, fieldName: String) {
        writer.write(TWO_HYPHENS + BOUNDARY + LINE_FEED)
        writer.write("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${file.name}\"$LINE_FEED")
        writer.write("Content-Type: image/jpeg$LINE_FEED") // Assume JPEG for simplicity
        writer.write(LINE_FEED)
        writer.flush()

        FileInputStream(file).use { fileInputStream ->
            fileInputStream.copyTo(outputStream)
        }
        outputStream.flush()
        writer.write(LINE_FEED)
        writer.flush()
    }

    // Helper to create a temporary file from the input stream
    private fun createTempFile(inputStream: InputStream?): File? {
        return try {
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            tempFile.deleteOnExit()

            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file: ${e.message}", e)
            null
        }
    }

    // --- PUBLIC INTERFACE FUNCTIONS ---

    /**
     * Upload profile picture to server (Targets upload_profile_picture.php)
     */
    suspend fun uploadProfilePicture(userId: Int, imageUri: Uri): UploadResult {
        return executeImageUpload(
            endpoint = "upload_profile_picture.php",
            userId = userId,
            imageUri = imageUri,
            fileFieldName = "profile_picture" // Matches the PHP expected POST field
        )
    }

    /**
     * Upload background picture to server (Targets upload_background.php)
     */
    suspend fun uploadBackgroundPicture(userId: Int, imageUri: Uri): UploadResult {
        return executeImageUpload(
            endpoint = "upload_background.php",
            userId = userId,
            imageUri = imageUri,
            fileFieldName = "background_picture" // Matches the PHP expected POST field
        )
    }

    /**
     * Update non-image profile text data (Bio, Full Name, etc.).
     */
    suspend fun updateProfileText(userId: Int, bio: String?, fullName: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/update_profile_bio.php") // NEW ENDPOINT
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("user_id", userId)
                    put("bio", bio ?: "")
                    put("full_name", fullName ?: "")
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
                Log.e(TAG, "Update text failed: ${e.message}", e)
                return@withContext false
            }
        }
    }

    // --- Result Class ---

    sealed class UploadResult {
        data class Success(val imageUrl: String) : UploadResult()
        data class Error(val message: String) : UploadResult()
    }
}