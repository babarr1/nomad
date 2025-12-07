package com.example.nomad

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
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
    private lateinit var postsCount: TextView
    private lateinit var friendsCount: TextView
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var backBtn: ImageView

    // Updated variable declarations to match the new XML
    private lateinit var profileBackground: ImageView // NEW: For the WoW factor background
    private lateinit var profilePicture: ImageView // Updated ID
    private lateinit var usernameText: TextView    // Updated ID
    private lateinit var bioText: TextView         // Updated ID
    private lateinit var postCount: TextView
    private lateinit var menuButton: ImageView     // Updated ID for the three dots

    private val PICK_PROFILE_REQUEST = 100 // New unique request code for profile pic
    private val PICK_BACKGROUND_REQUEST = 101 // New unique request code for background
    private var viewedUserId: Int = -1
    private var isOwnProfile: Boolean = false
    private val BASE_URL = "http://192.168.100.10/nomad_api"
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
        profilePicture = findViewById(R.id.profilePicture)
        profileBackground = findViewById(R.id.profileBackground)
        usernameText = findViewById(R.id.usernameText)
        bioText = findViewById(R.id.bioText)
        backBtn = findViewById(R.id.backBtn)
        menuButton = findViewById(R.id.menuButton)
        postCount = findViewById(R.id.postCount)       // Error variable fixed here!
        friendsCount = findViewById(R.id.friendsCount)
        postsRecyclerView = findViewById(R.id.profileRecyclerView)

        // Setup posts grid
        postsRecyclerView.layoutManager = GridLayoutManager(this, 3)

    }

    private fun setupListeners() {
        backBtn.setOnClickListener {
            finish()
        }

        // Top right icon functionality (could be edit profile for own profile, add friend for others)
        menuButton.setOnClickListener { view ->
            showProfileMenu(view)

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
                        backgroundPictureUrl = data.optString("background_picture_url", null),
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
        usernameText.text = data.fullName

        // Set bio
        bioText.text = data.bio

        // Load profile picture
        if (!data.profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(data.profilePictureUrl)
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .transform(CircleTransform())
                .placeholder(R.drawable.icon_profile)
                .error(R.drawable.icon_profile)
                .into(profilePicture)
        } else {
            profilePicture.setImageResource(R.drawable.icon_profile)
        }

        // 2. Load Background Picture (NEW LOGIC with Cache Bypass)
        if (!data.backgroundPictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(data.backgroundPictureUrl) /* Uses new URL field */
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)/* FIX: Bypass cache */
                .networkPolicy(NetworkPolicy.NO_CACHE)
                .resize(1000, 500)
                .centerCrop()
                .error(R.drawable.bg_profile) // Placeholder you set

                .placeholder(R.drawable.bg_profile)
                .error(R.drawable.bg_profile)
                .into(profileBackground)
                // Displayed in the header ImageView
        }
        else {
            // Fallback for default background if URL is missing
            profileBackground.setImageResource(R.drawable.bg_profile)
        }

        // Update stats - find TextViews in stats layout

        postCount.text = data.postsCount.toString()
        friendsCount.text = data.friendsCount.toString()

        // Setup posts grid
        val postsAdapter = PostsAdapter(data.posts)
        postsRecyclerView.adapter = postsAdapter
    }

    // Inside ProfileActivity.kt

    // Function to launch the gallery for the profile picture
    private fun openImagePickerForProfile() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_PROFILE_REQUEST)
    }

    override fun onResume() {
        super.onResume()
        // CRITICAL: Force a reload of the data every time the screen becomes active
        if (viewedUserId != -1) {
            loadProfileData()
        }
    }

    // Function to launch the gallery for the background picture
    private fun openImagePickerForBackground() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_BACKGROUND_REQUEST)
    }

    private fun showProfileMenu(view: View) {
        // Only show if it's the user's own profile
        if (!isOwnProfile) return

        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.profile_edit_menu, popup.menu) // Create this menu XML!

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_change_profile_pic -> {
                    // Launch the gallery to pick a new profile picture (reusing HomeActivity logic)
                    openImagePickerForProfile()
                    true
                }
                R.id.action_change_background -> {
                    // Launch the gallery to pick a new background picture
                    openImagePickerForBackground()
                    true
                }
                R.id.action_edit_bio -> {
                    // Launch EditProfileActivity (New Activity)
                    startActivity(Intent(this, EditProfileActivity::class.java).apply {
                        putExtra("ACTION", "BIO")
                    })
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

// NOTE: You need to create R.menu.profile_edit_menu.xml and the EditProfileActivity.kt



    data class ProfileData(
        val fullName: String,
        val username: String,
        val bio: String,
        val profilePictureUrl: String?,
        val backgroundPictureUrl: String?,
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