package com.example.nomad

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class UploadManager(private val context: Context) {
    private val TAG = "UploadManager"

    // NOTE: Use the correct BASE_URL for your setup (e.g., http://10.0.2.2/nomad_api for emulator)
    private val BASE_URL = "http://192.168.100.15/nomad_api"
    private val BOUNDARY = "*****" + System.currentTimeMillis() + "*****"
    private val LINE_FEED = "\r\n"

    sealed class UploadResult {
        data class Success(val message: String) : UploadResult()
        data class Error(val message: String) : UploadResult()
    }

    suspend fun uploadDiscovery(
        userId: Int,
        categoryId: Int,
        caption: String,
        imageUri: Uri
    ): UploadResult {
        return withContext(Dispatchers.IO) {
            try {
                val uploadUrl = URL("$BASE_URL/upload_discovery.php")
                val connection = uploadUrl.openConnection() as HttpURLConnection

                // Setup connection properties for POST and Multi-part data
                connection.requestMethod = "POST"
                connection.useCaches = false
                connection.doOutput = true
                connection.doInput = true
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")
                connection.connectTimeout = 30000 // 30 seconds
                connection.readTimeout = 30000

                val outputStream: OutputStream = connection.outputStream
                val writer = BufferedWriter(OutputStreamWriter(outputStream, "UTF-8"))

                // 1. Add text data (User ID, Category ID, Caption)
                addFormField(writer, "user_id", userId.toString())
                addFormField(writer, "category_id", categoryId.toString())
                addFormField(writer, "title", caption) // Using caption as the title

                // 2. Add image file data
                val imageFileName = "discovery_image.jpg"
                addFilePart(writer, outputStream, context, "image_file", imageFileName, imageUri)

                // 3. Close the request body
                writer.write(LINE_FEED)
                writer.write("--$BOUNDARY--$LINE_FEED")
                writer.flush()
                // writer.close() // Close streams after reading response
                // outputStream.close()

                // Read the response
                val responseCode = connection.responseCode
                Log.d(TAG, "Upload response code: $responseCode")

                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                connection.disconnect()

                // Parse response
                val jsonResponse = JSONObject(response)
                val success = jsonResponse.getBoolean("success")
                val message = jsonResponse.getString("message")

                return@withContext if (success) {
                    UploadResult.Success(message)
                } else {
                    UploadResult.Error(message)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Upload failed: ${e.message}", e)
                return@withContext UploadResult.Error("Network/IO Error: ${e.message}")
            }
        }
    }

    private fun addFormField(writer: BufferedWriter, fieldName: String, value: String) {
        writer.write("--$BOUNDARY$LINE_FEED")
        writer.write("Content-Disposition: form-data; name=\"$fieldName\"$LINE_FEED")
        writer.write("Content-Type: text/plain; charset=UTF-8$LINE_FEED")
        writer.write(LINE_FEED)
        writer.write(value)
        writer.write(LINE_FEED)
        writer.flush()
    }

    private fun addFilePart(
        writer: BufferedWriter,
        outputStream: OutputStream,
        context: Context,
        fieldName: String,
        fileName: String,
        fileUri: Uri
    ) {
        writer.write("--$BOUNDARY$LINE_FEED")
        writer.write("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"$LINE_FEED")
        // Use a generic content type if the resolver fails
        val contentType = context.contentResolver.getType(fileUri) ?: "application/octet-stream"
        writer.write("Content-Type: $contentType$LINE_FEED")
        writer.write("Content-Transfer-Encoding: binary$LINE_FEED")
        writer.write(LINE_FEED)
        writer.flush()

        // Read the file content and write it to the output stream
        val inputStream = context.contentResolver.openInputStream(fileUri)
        inputStream?.use { input ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
        }
        writer.write(LINE_FEED)
        writer.flush()
    }
}