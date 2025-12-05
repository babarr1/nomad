package com.example.nomad

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeActivity : Activity() {
    private lateinit var authManager: AuthManager
    private lateinit var profilePictureManager: ProfilePictureManager
    private lateinit var profileImage: ImageView

    private val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        authManager = AuthManager(this)
        profilePictureManager = ProfilePictureManager(this)

        // Display username
        val usernameText = findViewById<TextView>(R.id.usernameText)
        val username = authManager.getUsername()
        usernameText.text = username ?: "User"

        // Setup profile image
        profileImage = findViewById(R.id.profileImage)
        loadProfilePicture()

        // Make profile image clickable to change picture
        profileImage.setOnClickListener {
            openImagePicker()
        }

        // --- 1. Connect Back Button ---
        val backButton = findViewById<ImageView>(R.id.backBtn)
        backButton.setOnClickListener {
            Toast.makeText(this@HomeActivity, "Navigating Back...", Toast.LENGTH_SHORT).show()
            finish()
        }

        // --- 2. Connect Connect Button ---
        val connectButton = findViewById<ImageView>(R.id.connectBtn)
        connectButton.setOnClickListener {
            Toast.makeText(this@HomeActivity, "Connecting to Service...", Toast.LENGTH_SHORT).show()
        }

        // --- 3. Connect Search Button (to open Friends Activity) ---
        val searchButton = findViewById<ImageView>(R.id.searchButton)
        searchButton.setOnClickListener {
            val intent = Intent(this@HomeActivity, SearchActivity::class.java)
            startActivity(intent)
        }

        // --- 4. Connect Logout Button ---
        val logoutButton = findViewById<ImageView>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            performLogout()
        }
    }

    /**
     * Load user's profile picture using Picasso with circular transformation
     */
    private fun loadProfilePicture() {
        val profilePictureUrl = authManager.getProfilePictureUrl()

        if (!profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(profilePictureUrl)
                .transform(CircleTransform()) // Apply circular transformation
                .placeholder(R.drawable.icon_profile)
                .error(R.drawable.icon_profile)
                .fit()
                .centerCrop()
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.icon_profile)
        }
    }

    /**
     * Open image picker to select profile picture
     */
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                uploadProfilePicture(imageUri)
            }
        }
    }

    /**
     * Upload selected profile picture
     */
    private fun uploadProfilePicture(imageUri: Uri) {
        val userId = authManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val result = profilePictureManager.uploadProfilePicture(userId, imageUri)

            when (result) {
                is ProfilePictureManager.UploadResult.Success -> {
                    // Update AuthManager with new profile picture URL
                    authManager.updateProfilePicture(result.imageUrl)

                    // Load the new image with circular transformation
                    Picasso.get()
                        .load(result.imageUrl)
                        .transform(CircleTransform()) // Apply circular transformation
                        .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                        .placeholder(R.drawable.icon_profile)
                        .error(R.drawable.icon_profile)
                        .fit()
                        .centerCrop()
                        .into(profileImage)

                    Toast.makeText(
                        this@HomeActivity,
                        "Profile picture updated!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is ProfilePictureManager.UploadResult.Error -> {
                    Toast.makeText(
                        this@HomeActivity,
                        "Upload failed: ${result.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun performLogout() {
        // Clear session
        authManager.logout()

        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_LONG).show()

        // Navigate to Login screen
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}