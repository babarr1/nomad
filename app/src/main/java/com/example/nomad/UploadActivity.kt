package com.example.nomad

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UploadActivity : AppCompatActivity() {

    // --- Constants and Variables ---
    private val PICK_IMAGE_REQUEST = 100 // Request code for the gallery Intent
    private var selectedImageUri: Uri? = null
    private var selectedCategoryId: Int = -1

    private lateinit var selectedImageView: ImageView
    private lateinit var categorySpinner: Spinner
    private lateinit var captionInput: EditText
    private lateinit var postButton: Button
    private lateinit var uploadManager: UploadManager
    private lateinit var initialPlaceholder: LinearLayout // For the "Tap to select image" view
    private lateinit var inputContainer: LinearLayout // Container for category/caption inputs

    // Corresponds to the category IDs in your 'category' table (from your DB screenshot)
    private val categoriesMap = mapOf(
        "FAUNA" to 1,
        "VERDANT" to 2,
        "STRATA" to 3,
        "PHENOMENON" to 4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        // 1. Initialize views and manager
        selectedImageView = findViewById(R.id.selectedImageView)
        categorySpinner = findViewById(R.id.categorySpinner)
        captionInput = findViewById(R.id.captionInput)
        postButton = findViewById(R.id.postButton)
        initialPlaceholder = findViewById(R.id.initialPlaceholder)
        inputContainer = findViewById(R.id.inputContainer)
        uploadManager = UploadManager(this)

        // --- NEW: Check for incoming URI from Camera/Gallery ---
        val incomingUri: Uri? = intent.data
        if (incomingUri != null) {
            // An image URI was passed directly (from the swipe/camera)
            handleImageSelection(incomingUri)
        } else {
            // No URI was passed, so prompt the user to select one (via the icon/button click)
            openImagePicker()
        }

        // 2. Setup UI actions
        setupCategorySpinner()
        findViewById<ImageView>(R.id.backBtn).setOnClickListener { finish() }

        // Clicking the selection area (including the placeholder) opens the picker
        val selectionArea = findViewById<RelativeLayout>(R.id.imageSelectionArea)
        selectionArea.setOnClickListener { openImagePicker() }

        // Final Post Action
        postButton.setOnClickListener { performUpload() }

        // 3. Initial state: Prompt user to select an image immediately
        openImagePicker()
    }

    // --- Helper Functions ---

    private fun setupCategorySpinner() {
        // We add "Select Category" as the first prompt
        val categoryNamesWithPrompt = mutableListOf("Select Category").apply {
            addAll(categoriesMap.keys.toList())
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categoryNamesWithPrompt
        )
        categorySpinner.adapter = adapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    selectedCategoryId = -1 // Prompt selected
                } else {
                    val selectedName = categoryNamesWithPrompt[position]
                    selectedCategoryId = categoriesMap[selectedName] ?: -1
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCategoryId = -1
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    // --- Image Selection & Result Handling ---

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                val imageUri = data.data
                if (imageUri != null) {
                    handleImageSelection(imageUri)
                    return
                }
            } else if (resultCode == RESULT_CANCELED) {
                // If the user backs out of the gallery, send them back to the previous screen (Home)
                if (selectedImageUri == null) {
                    Toast.makeText(this, "Upload cancelled.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun handleImageSelection(imageUri: Uri) {
        selectedImageUri = imageUri

        // Show the image and hide the placeholder text
        Picasso.get()
            .load(imageUri)
            .into(selectedImageView)

        initialPlaceholder.visibility = View.GONE

        // Reveal the input fields now that an image is selected
        inputContainer.visibility = View.VISIBLE
    }

    // --- Upload Logic ---

// --- Upload Logic ---

    private fun performUpload() {
        val caption = captionInput.text.toString().trim()

        // Disable button immediately to prevent double-click
        postButton.isEnabled = false

        // 1. Initial Quick Checks
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show()
            postButton.isEnabled = true
            return
        }
        if (selectedCategoryId == -1) {
            Toast.makeText(this, "Please select a valid category.", Toast.LENGTH_SHORT).show()
            postButton.isEnabled = true
            return
        }

        // 2. Combined Caption Validation (FIXED the database insertion issue)
        if (caption.length < 5) {
            Toast.makeText(this, "Please enter a descriptive caption (minimum 5 characters).", Toast.LENGTH_SHORT).show()
            postButton.isEnabled = true
            postButton.text = "POST DISCOVERY"
            return
        }

        val authManager = AuthManager(this)
        val userId = authManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User not logged in. Please log in again.", Toast.LENGTH_SHORT).show()
            postButton.isEnabled = true
            return
        }

        // Proceed to upload state
        postButton.text = "Uploading..."

        CoroutineScope(Dispatchers.Main).launch {
            val result = uploadManager.uploadDiscovery(
                userId = userId,
                categoryId = selectedCategoryId,
                caption = caption,
                imageUri = selectedImageUri!!
            )

            if (result is UploadManager.UploadResult.Success) {

                // --- NEW NAVIGATION LOGIC ---
                val categoryActivityClass = when (selectedCategoryId) {
                    1 -> FaunaActivity::class.java
                    2 -> VerdantActivity::class.java
                    3 -> StrataActivity::class.java
                    4 -> PhenomenaActivity::class.java
                    else -> HomeActivity::class.java
                }

                val intent = Intent(this@UploadActivity, categoryActivityClass).apply {
                    // Clears activity history so the back button goes Home, not back to Upload
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
               // Close the upload screen
                finish()
                // --- END NEW NAVIGATION LOGIC ---
            } else if (result is UploadManager.UploadResult.Error) {
                Toast.makeText(this@UploadActivity, "Upload failed: ${result.message}", Toast.LENGTH_LONG).show()
                postButton.isEnabled = true
                postButton.text = "POST DISCOVERY"
            }
        }
    }
}