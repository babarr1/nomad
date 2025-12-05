package com.example.nomad

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Handles profile picture upload functionality
 */
class ProfilePictureManager(private val context: Context) {
    private val BASE_URL = "http://192.168.18.51/nomad_api" // Match your AuthManager URL
    private val TAG = "ProfilePictureManager"

    /**
     * Upload profile picture to server
     */
    suspend fun uploadProfilePicture(userId: Int, imageUri: Uri): UploadResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Uploading profile picture for user: $userId")

                // Get file from URI
                val file = getFileFromUri(imageUri)
                if (file == null || !file.exists()) {
                    return@withContext UploadResult.Error("File not found")
                }

                val url = URL("$BASE_URL/upload_profile_picture.php")
                val connection = url.openConnection() as HttpURLConnection

                val boundary = "===" + System.currentTimeMillis() + "==="
                val lineEnd = "\r\n"
                val twoHyphens = "--"

                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.doInput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.connectTimeout = 30000
                connection.readTimeout = 30000

                val outputStream = connection.outputStream
                val writer = outputStream.bufferedWriter()

                // Add user_id parameter
                writer.write(twoHyphens + boundary + lineEnd)
                writer.write("Content-Disposition: form-data; name=\"user_id\"$lineEnd")
                writer.write(lineEnd)
                writer.write(userId.toString())
                writer.write(lineEnd)
                writer.flush()

                // Add file
                writer.write(twoHyphens + boundary + lineEnd)
                writer.write("Content-Disposition: form-data; name=\"profile_picture\"; filename=\"${file.name}\"$lineEnd")
                writer.write("Content-Type: image/*$lineEnd")
                writer.write(lineEnd)
                writer.flush()

                val fileInputStream = FileInputStream(file)
                val buffer = ByteArray(4096)
                var bytesRead: Int

                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                fileInputStream.close()
                outputStream.flush()

                writer.write(lineEnd)
                writer.write(twoHyphens + boundary + twoHyphens + lineEnd)
                writer.flush()
                writer.close()
                outputStream.close()

                // Read response
                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")

                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "Response: $response")
                connection.disconnect()

                // Parse response
                val jsonResponse = JSONObject(response)
                val success = jsonResponse.getBoolean("success")
                val message = jsonResponse.getString("message")

                if (success) {
                    val data = jsonResponse.getJSONObject("data")
                    val profilePictureUrl = data.getString("profile_picture_url")
                    UploadResult.Success(profilePictureUrl)
                } else {
                    UploadResult.Error(message)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload error: ${e.message}", e)
                UploadResult.Error("Upload failed: ${e.message}")
            }
        }
    }

    /**
     * Get file from content URI
     */
    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            tempFile.deleteOnExit()

            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error getting file from URI: ${e.message}", e)
            null
        }
    }

    sealed class UploadResult {
        data class Success(val imageUrl: String) : UploadResult()
        data class Error(val message: String) : UploadResult()
    }
}