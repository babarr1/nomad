package com.example.nomad

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AuthManager handles user authentication and session management
 */
class AuthManager(private val context: Context) {
    private val prefs: SharedPreferences
    private val TAG = "AuthManager"

    init {
        prefs = context.getSharedPreferences("NomadPrefs", Context.MODE_PRIVATE)
        Log.d(TAG, "AuthManager initialized")
    }

    // Change this to your computer's local IP address when testing on physical device
    // For emulator, use 10.0.2.2
    // For physical device on same network, use your PC's IP (e.g., 192.168.1.100)
    private val BASE_URL = "http://192.168.100.10/nomad_api" // Change for physical device

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_EMAIL = "email"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_PROFILE_PICTURE_URL = "profile_picture_url"
        private const val KEY_BACKGROUND_PICTURE_URL = "background_picture_url"
        private const val KEY_BIO = "bio"
    }

    /**
     * Check if user is logged in
     * This is a simple check that doesn't make network calls
     */
    fun isLoggedIn(): Boolean {
        return try {
            val loggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
            Log.d(TAG, "isLoggedIn check: $loggedIn")
            loggedIn
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status: ${e.message}", e)
            false
        }
    }

    /**
     * Store user session after successful login
     */
    fun login(userId: Int, username: String, email: String, fullName: String?, sessionToken: String, profilePictureUrl: String? = null, backgroundPictureUrl: String? = null) {
        try {
            prefs.edit().apply {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putInt(KEY_USER_ID, userId)
                putString(KEY_USERNAME, username)
                putString(KEY_EMAIL, email)
                putString(KEY_FULL_NAME, fullName)
                putString(KEY_SESSION_TOKEN, sessionToken)
                putString(KEY_PROFILE_PICTURE_URL, profilePictureUrl)
                putString(KEY_BACKGROUND_PICTURE_URL, backgroundPictureUrl)
                Log.d(TAG, "AuthManager: Stored background URL: $backgroundPictureUrl")
                Log.d(TAG, "User session stored: $username")
                Log.d(TAG, "AuthManager: Stored user ID: $userId")
                apply()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error storing login session: ${e.message}", e)
        }
    }

    /**
     * Clear user session (logout)
     */
    fun logout() {
        try {
            prefs.edit().apply {
                clear()
                apply()
            }
            Log.d(TAG, "User logged out")
        } catch (e: Exception) {
            Log.e(TAG, "Error during logout: ${e.message}", e)
        }
    }

    /**
     * Get logged in user's username
     */
    fun getUsername(): String? {
        return try {
            prefs.getString(KEY_USERNAME, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting username: ${e.message}", e)
            null
        }
    }


    /**
     * Get logged in user's email
     */
    fun getEmail(): String? {
        return try {
            prefs.getString(KEY_EMAIL, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting email: ${e.message}", e)
            null
        }
    }

    /**
     * Get logged in user's full name
     */
    fun getFullName(): String? {
        return try {
            prefs.getString(KEY_FULL_NAME, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting full name: ${e.message}", e)
            null
        }
    }

    /**
     * Get logged in user's ID
     */
    fun getUserId(): Int {
        return try {
            prefs.getInt(KEY_USER_ID, -1)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user ID: ${e.message}", e)
            -1
        }
    }


    // Getter for background URL
    fun getBackgroundPictureUrl(): String? {
        return prefs.getString(KEY_BACKGROUND_PICTURE_URL, null)
    }

    // Setter called after successful background image upload
    fun updateBackgroundPicture(backgroundPictureUrl: String) {
        prefs.edit().apply {
            putString(KEY_BACKGROUND_PICTURE_URL, backgroundPictureUrl)
            apply()
        }
    }

    /**
     * Get logged in user's profile picture URL
     */
    fun getProfilePictureUrl(): String? {
        return try {
            prefs.getString(KEY_PROFILE_PICTURE_URL, null)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting profile picture URL: ${e.message}", e)
            null
        }
    }

    /**
     * Update profile picture URL in session
     */
    fun updateProfilePicture(profilePictureUrl: String) {
        try {
            prefs.edit().apply {
                putString(KEY_PROFILE_PICTURE_URL, profilePictureUrl)
                apply()
            }
            Log.d(TAG, "Profile picture URL updated")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile picture: ${e.message}", e)
        }
    }

    // Inside AuthManager.kt

    // Getter
    fun getBio(): String? {
        return prefs.getString(KEY_BIO, null)
    }

    // Setter
    fun updateBio(newBio: String) {
        prefs.edit().apply {
            putString(KEY_BIO, newBio)
            apply()
        }
    }
    /**
     * Perform login API call
     */
    suspend fun performLogin(email: String, password: String): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting login for: $email")

                val url = URL("$BASE_URL/login.php")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // Create JSON request body
                val jsonBody = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                Log.d(TAG, "Sending login request...")

                // Send request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

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
                    LoginResult.Success(
                        userId = data.getInt("user_id"),
                        username = data.getString("username"),
                        email = data.getString("email"),
                        fullName = data.optString("full_name", null),
                        sessionToken = data.getString("session_token"),
                        profilePictureUrl = data.optString("profile_picture_url", null)
                    )
                } else {
                    LoginResult.Error(message)
                }


            } catch (e: Exception) {
                Log.e(TAG, "Login error: ${e.message}", e)
                LoginResult.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Perform signup API call
     */
    suspend fun performSignup(name: String, email: String, password: String): SignupResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting signup for: $email")

                val url = URL("$BASE_URL/signup.php")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // Create JSON request body
                val jsonBody = JSONObject().apply {
                    put("name", name)
                    put("email", email)
                    put("password", password)
                }

                Log.d(TAG, "Sending signup request...")

                // Send request
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                // Read response
                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")

                val response = if (responseCode in 200..299) {
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
                    SignupResult.Success(
                        userId = data.getInt("user_id"),
                        username = data.getString("username"),
                        email = data.getString("email"),
                        fullName = data.getString("full_name")
                    )
                } else {
                    SignupResult.Error(message)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Signup error: ${e.message}", e)
                SignupResult.Error("Network error: ${e.message}")
            }
        }
    }

    /**
     * Result classes for API calls
     */
    sealed class LoginResult {
        data class Success(
            val userId: Int,
            val username: String,
            val email: String,
            val fullName: String?,
            val sessionToken: String,
            val profilePictureUrl: String?
        ) : LoginResult()

        data class Error(val message: String) : LoginResult()
    }

    sealed class SignupResult {
        data class Success(
            val userId: Int,
            val username: String,
            val email: String,
            val fullName: String
        ) : SignupResult()

        data class Error(val message: String) : SignupResult()
    }
}