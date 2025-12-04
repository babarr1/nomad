package com.example.nomad

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ProfileActivity : AppCompatActivity() {
    private lateinit var authManager: AuthManager
    private lateinit var profilePic: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileBio: TextView
    private lateinit var postsCount: TextView
    private lateinit var friendsCount: TextView
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var backBtn: ImageView
    private lateinit var topRightIcon: ImageView

    private var viewedUserId: Int = -1
    private var isOwnProfile: Boolean = false
    private val BASE_URL = "http://192.168.18.51/nomad_api"
    private val TAG = "ProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        authManager = AuthManager(this)

        // Get user ID from intent (if viewing another user's profile)
        viewedUserId = intent.getIntExtra("USER_ID", -1)
        val currentUserId = authManager.getUserId()

        // Check if viewing own profile
        isOwnProfile = viewedUserId == -1 || viewedUserId == currentUserId
        if (isOwnProfile) {
            viewedUserId = currentUserId
        }

        initializeViews()
        setupListeners()
        loadProfileData()
    }

    private fun initializeViews() {
        profilePic = findViewById(R.id.profilePic)
        profileName = findViewById(R.id.profileName)
        profileBio = findViewById(R.id.profileBio)
        backBtn = findViewById(R.id.backBtn)
        topRightIcon = findViewById(R.id.topRightIcon)
        postsRecyclerView = findViewById(R.id.postsRecyclerView)

        // Setup posts grid
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)
    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            finish()
        }

        // Top right icon functionality (could be edit profile for own profile, add friend for others)
        topRightIcon.setOnClickListener {
            if (isOwnProfile) {
                Toast.makeText(this, "Edit profile coming soon", Toast.LENGTH_SHORT).show()
            } else {
                // Add friend functionality
                Toast.makeText(this, "Add friend coming soon", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileData() {
        if (viewedUserId == -1) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val profileData = fetchProfileData(viewedUserId)

                if (profileData != null) {
                    displayProfileData(profileData)
                } else {
                    Toast.makeText(
                        this@ProfileActivity,
                        "Failed to load profile",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading profile: ${e.message}", e)
                Toast.makeText(
                    this@ProfileActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun fetchProfileData(userId: Int): ProfileData? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching profile for user: $userId")

                val url = URL("$BASE_URL/get_profile.php?user_id=$userId")
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

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
                val success = jsonResponse.getBoolean("success")

                if (success) {
                    val data = jsonResponse.getJSONObject("data")
                    ProfileData(
                        fullName = data.getString("full_name"),
                        username = data.getString("username"),
                        bio = data.optString("bio", "No bio available"),
                        profilePictureUrl = data.optString("profile_picture_url", null),
                        postsCount = data.getInt("posts_count"),
                        friendsCount = data.getInt("friends_count"),
                        posts = parsePostsArray(data.optJSONArray("posts"))
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fetch profile error: ${e.message}", e)
                null
            }
        }
    }

    private fun parsePostsArray(postsArray: org.json.JSONArray?): List<Post> {
        val posts = mutableListOf<Post>()

        if (postsArray != null) {
            for (i in 0 until postsArray.length()) {
                val postObj = postsArray.getJSONObject(i)
                posts.add(
                    Post(
                        observationId = postObj.getInt("observation_id"),
                        imageUrl = postObj.getString("image_url"),
                        title = postObj.optString("title", "")
                    )
                )
            }
        }

        return posts
    }

    private fun displayProfileData(data: ProfileData) {
        // Set profile name
        profileName.text = data.fullName

        // Set bio
        profileBio.text = data.bio

        // Load profile picture
        if (!data.profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(data.profilePictureUrl)
                .transform(CircleTransform())
                .placeholder(R.drawable.icon_profile)
                .error(R.drawable.icon_profile)
                .into(profilePic)
        } else {
            profilePic.setImageResource(R.drawable.icon_profile)
        }

        // Update stats - find TextViews in stats layout
        val statsLayout = findViewById<android.widget.LinearLayout>(R.id.profileStats)
        val postsText = statsLayout.getChildAt(0) as TextView
        val friendsText = statsLayout.getChildAt(1) as TextView

        postsText.text = "${data.postsCount}\nPosts"
        friendsText.text = "${data.friendsCount}\nFriends"

        // Setup posts grid
        val postsAdapter = PostsAdapter(data.posts)
        postsRecyclerView.adapter = postsAdapter
    }

    data class ProfileData(
        val fullName: String,
        val username: String,
        val bio: String,
        val profilePictureUrl: String?,
        val postsCount: Int,
        val friendsCount: Int,
        val posts: List<Post>
    )

    data class Post(
        val observationId: Int,
        val imageUrl: String,
        val title: String
    )
}