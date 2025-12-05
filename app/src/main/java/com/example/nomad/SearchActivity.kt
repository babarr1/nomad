package com.example.nomad

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SearchActivity : AppCompatActivity() {
    private lateinit var authManager: AuthManager
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateText: TextView
    private lateinit var backButton: ImageView
    private lateinit var sectionTitle: TextView
    private lateinit var resultCount: TextView
    private lateinit var searchAdapter: SearchAdapter

    private val BASE_URL = "http://192.168.18.51/nomad_api"
    private val TAG = "SearchActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        authManager = AuthManager(this)

        // Initialize views
        searchInput = findViewById(R.id.searchInput)
        recyclerView = findViewById(R.id.friendsRecyclerView)
        emptyStateText = findViewById(R.id.emptyStateText)
        backButton = findViewById(R.id.backBtn)
        sectionTitle = findViewById(R.id.sectionTitle)
        resultCount = findViewById(R.id.resultCount)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup search functionality
        setupSearch()

        // Setup back button
        backButton.setOnClickListener {
            finish()
        }

        // Load all users initially
        loadUsers("")
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter(
            users = emptyList(),
            onUserClick = { user ->
                openUserProfile(user)
            },
            onAddFriendClick = { user ->
                addFriend(user)
            }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchAdapter
        }
    }

    private fun setupSearch() {
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                loadUsers(query)

                // Update section title based on search
                if (query.isEmpty()) {
                    sectionTitle.text = "All Users"
                } else {
                    sectionTitle.text = "Search Results"
                }
            }
        })
    }

    private fun loadUsers(query: String) {
        val userId = authManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val users = searchUsersAPI(query, userId)

                if (users.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyStateText.visibility = View.VISIBLE
                    resultCount.text = ""
                    emptyStateText.text = if (query.isEmpty()) {
                        "No users found in database"
                    } else {
                        "No users found matching '$query'"
                    }
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyStateText.visibility = View.GONE
                    resultCount.text = "${users.size} user${if (users.size != 1) "s" else ""}"
                    searchAdapter.updateUsers(users)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users: ${e.message}", e)
                Toast.makeText(
                    this@SearchActivity,
                    "Error loading users: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun searchUsersAPI(query: String, userId: Int): List<User> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Searching users with query: '$query'")

                val url = URL("$BASE_URL/search_friends.php")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                // Create JSON request body
                val jsonBody = JSONObject().apply {
                    put("query", query)
                    put("user_id", userId)
                }

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

                if (success) {
                    val data = jsonResponse.getJSONObject("data")
                    val usersArray = data.getJSONArray("users")

                    val users = mutableListOf<User>()
                    for (i in 0 until usersArray.length()) {
                        val userObj = usersArray.getJSONObject(i)
                        users.add(
                            User(
                                userId = userObj.getInt("user_id"),
                                username = userObj.getString("username"),
                                fullName = userObj.getString("full_name"),
                                profilePictureUrl = userObj.optString("profile_picture_url", null),
                                isFriend = userObj.getBoolean("is_friend")
                            )
                        )
                    }
                    Log.d(TAG, "Found ${users.size} users")
                    users
                } else {
                    emptyList()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Search API error: ${e.message}", e)
                emptyList()
            }
        }
    }

    private fun openUserProfile(user: User) {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("USER_ID", user.userId)
        startActivity(intent)
    }

    private fun addFriend(user: User) {
        val currentUserId = authManager.getUserId()

        if (currentUserId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = sendFriendRequest(currentUserId, user.userId)

                if (result) {
                    Toast.makeText(
                        this@SearchActivity,
                        "Friend request sent to ${user.fullName}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Reload users to update friend status
                    loadUsers(searchInput.text.toString().trim())
                } else {
                    Toast.makeText(
                        this@SearchActivity,
                        "Failed to send friend request",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding friend: ${e.message}", e)
                Toast.makeText(
                    this@SearchActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun sendFriendRequest(fromUserId: Int, toUserId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Sending friend request from $fromUserId to $toUserId")

                val url = URL("$BASE_URL/add_friend.php")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val jsonBody = JSONObject().apply {
                    put("user_id", fromUserId)
                    put("friend_id", toUserId)
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")

                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "Response: $response")
                connection.disconnect()

                val jsonResponse = JSONObject(response)
                jsonResponse.getBoolean("success")

            } catch (e: Exception) {
                Log.e(TAG, "Add friend error: ${e.message}", e)
                false
            }
        }
    }
}