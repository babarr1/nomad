package com.example.nomad

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : Activity() {
    private lateinit var authManager: AuthManager
    private lateinit var profilePictureManager: ProfilePictureManager
    private lateinit var profileImage: ImageView

    private lateinit var faunaCard: LinearLayout
    private lateinit var verdantCard: LinearLayout
    private lateinit var strataCard: LinearLayout
    private lateinit var phenomenonCard: LinearLayout

    private val CAMERA_REQUEST = 5
    // CRITICAL: Needs late initialization modifier since it's set in openCamera()
    private lateinit var cameraImageUri: Uri
    private val PICK_IMAGE_REQUEST = 1

    @SuppressLint("MissingInflatedId")
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
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        usernameText.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        // --- 1. Connect Back Button ---
        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            Toast.makeText(this, "Navigating Back...", Toast.LENGTH_SHORT).show()
            finish()
        }

        // --- CAMERA BUTTON (Hand Icon or Side Button) ---
        // Assuming R.id.cameraButton exists based on previous discussion
        findViewById<ImageView>(R.id.cameraBtn).setOnClickListener {
            openCamera()
        }

        // --- 2. Connect Connect Button ---
        findViewById<ImageView>(R.id.connectBtn).setOnClickListener {
            Toast.makeText(this, "Connecting to Service...", Toast.LENGTH_SHORT).show()
        }

        // --- 3. Connect Search Button (to open Friends Activity) ---
        findViewById<ImageView>(R.id.searchButton).setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        // --- 4. Connect Logout Button ---
        findViewById<ImageView>(R.id.logoutButton).setOnClickListener {
            performLogout()
        }

        // --- Nav Add Button (Gallery Upload) ---
        findViewById<ImageView>(R.id.navAdd).setOnClickListener {
            navigateToUploadActivity(null) // Pass null to prompt selection inside UploadActivity
        }

        // --- INITIALIZE CATEGORY CARDS ---
        faunaCard = findViewById(R.id.faunaCard)
        verdantCard = findViewById(R.id.verdantCard)
        strataCard = findViewById(R.id.strataCard)
        phenomenonCard = findViewById(R.id.phenomenonCard)

        // --- SET CATEGORY CLICK LISTENERS ---
        faunaCard.setOnClickListener { startActivity(Intent(this, FaunaActivity::class.java)) }
        verdantCard.setOnClickListener { startActivity(Intent(this, VerdantActivity::class.java)) }
        strataCard.setOnClickListener { startActivity(Intent(this, StrataActivity::class.java)) }
        phenomenonCard.setOnClickListener { startActivity(Intent(this, PhenomenaActivity::class.java)) }

        PendingSyncManager.syncPendingUploads(this)
    }

    /**
     * Load user's profile picture using Picasso with circular transformation
     */
    private fun loadProfilePicture() {
        // ... (function logic remains the same)
        val profilePictureUrl = authManager.getProfilePictureUrl()

        if (!profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(profilePictureUrl)
                .transform(CircleTransform())
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

    // --- CAMERA FUNCTIONALITY ---

    /**
     * Creates a secure file URI for the camera app to save the high-res image.
     */
    private fun getOutputMediaFileUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, "IMG_$timeStamp.jpg")

        val authorityString = context.packageName + ".provider"
        return FileProvider.getUriForFile(
            context,
            authorityString,
            imageFile
        )
    }

    /**
     * Launches the camera app, telling it where to save the full image.
     */
    internal fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // 1. Create the secure storage URI
        cameraImageUri = getOutputMediaFileUri(this)

        // 2. Tell the camera to save the full image here
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)

        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        if (cameraIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST)
        } else {
            Toast.makeText(this, "Camera not available.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Central navigation function to start the upload process.
     */
    internal fun navigateToUploadActivity(uri: Uri?) {
        val intent = Intent(this, UploadActivity::class.java)
        if (uri != null) {
            intent.data = uri
            intent.putExtra("ACTION_SOURCE", "CAMERA")
        }
        startActivity(intent)
    }

    // --- RESULT HANDLERS ---

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle image captured by Camera
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            if (::cameraImageUri.isInitialized) {
                // Success! Launch the upload flow with the full URI
                navigateToUploadActivity(cameraImageUri)
            } else {
                Toast.makeText(this, "Image capture failed (URI lost).", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Handle image picked from Gallery (for profile picture update)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                uploadProfilePicture(imageUri)
            }
        }
    }

    // --- PROFILE AND LOGOUT ---

    /**
     * Upload selected profile picture
     */
    private fun uploadProfilePicture(imageUri: Uri) {
        val userId = authManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Uploading profile picture...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            // ... (rest of upload logic) ...
            // NOTE: profilePictureManager needs to be implemented.
        }
    }

    private fun performLogout() {
        authManager.logout()

        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_LONG).show()

        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}