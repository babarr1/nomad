package com.example.nomad

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import android.provider.MediaStore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var profilePictureManager: ProfilePictureManager
    private lateinit var bioInput: EditText
    private lateinit var saveButton: Button

    private val PICK_PROFILE_REQUEST = 100
    private val PICK_BACKGROUND_REQUEST = 101

    private var imageUpdateType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // NOTE: You must create this layout file!
        setContentView(R.layout.activity_edit_profile)

        authManager = AuthManager(this)
        profilePictureManager = ProfilePictureManager(this)

        // Initialize Views
        bioInput = findViewById(R.id.bioEditInput) // Assuming ID in XML is bioEditInput
        saveButton = findViewById(R.id.saveBioButton) // Assuming ID in XML is saveBioButton

        // Handle Back Button
        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }
// --- Check Intent for Requested Action ---
        val action = intent.getStringExtra("ACTION")

        if (action == "PROFILE_PIC") {
            // If launched specifically to change profile pic (from menu)
            openImagePicker("PROFILE")
        } else if (action == "BACKGROUND_PIC") {
            // If launched specifically to change background pic (from menu)
            openImagePicker("BACKGROUND")
        }
        // Load the current bio into the EditText
        loadCurrentBio()

        // Set up save listener
        saveButton.setOnClickListener {
            performBioUpdate()
        }
    }

    private fun openImagePicker(type: String) {
        imageUpdateType = type // Store the type of image we are expecting back
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"

        if (type == "PROFILE") {
            startActivityForResult(intent, PICK_PROFILE_REQUEST)
        } else if (type == "BACKGROUND") {
            startActivityForResult(intent, PICK_BACKGROUND_REQUEST)
        }
    }


    // --- Image Selection Result Handler ---

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri ?: return // Exit if URI is null

            when (requestCode) {
                PICK_PROFILE_REQUEST -> uploadPicture(imageUri, "PROFILE")
                PICK_BACKGROUND_REQUEST -> uploadPicture(imageUri, "BACKGROUND")
            }
        } else if (resultCode == RESULT_CANCELED) {
            // If user cancels selection, just close the edit screen if no other action was pending
            if (imageUpdateType != null) {
                Toast.makeText(this, "Picture selection cancelled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Upload Logic for Pictures ---

    private fun uploadPicture(imageUri: Uri, type: String) {
        val userId = authManager.getUserId()
        if (userId == -1) return

        Toast.makeText(this, "Uploading $type picture...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.Main).launch {
            val result = if (type == "PROFILE") {
                profilePictureManager.uploadProfilePicture(userId, imageUri)
            } else {
                profilePictureManager.uploadBackgroundPicture(userId, imageUri)
            }

            if (result is ProfilePictureManager.UploadResult.Success) {
                // Update AuthManager and send user back to profile
                if (type == "PROFILE") {
                    authManager.updateProfilePicture(result.imageUrl)
                } else {
                    authManager.updateBackgroundPicture(result.imageUrl)
                }

                Toast.makeText(this@EditProfileActivity, "$type updated!", Toast.LENGTH_LONG).show()

                // IMPORTANT: We navigate back to ProfileActivity to force a UI refresh
                finish()
            } else {
                Toast.makeText(this@EditProfileActivity, "Upload failed.", Toast.LENGTH_LONG).show()
            }
        }
    }
    /**
     * Loads the current user's bio from the AuthManager and displays it.
     */
    private fun loadCurrentBio() {
        val currentBio = authManager.getBio() // Assuming you add getBio() to AuthManager
        if (!currentBio.isNullOrEmpty()) {
            bioInput.setText(currentBio)
        }
    }

    /**
     * Executes the network call to update the bio text.
     */
    private fun performBioUpdate() {
        val userId = authManager.getUserId()
        val newBio = bioInput.text.toString().trim()

        if (userId == -1) {
            Toast.makeText(this, "Authentication error. Please re-login.", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the bio is too long or empty
        if (newBio.length > 150) {
            Toast.makeText(this, "Bio is too long (Max 150 chars).", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        saveButton.text = "Saving..."

        // Launch coroutine for network update
        CoroutineScope(Dispatchers.Main).launch {
            // NOTE: We rely on the PostActionManager (or similar) to have the updateProfileText function
            val success = profilePictureManager.updateProfileText(
                userId = userId,
                bio = newBio,
                // You can add logic for other fields here, like fullName
                fullName = authManager.getFullName()
            )

            if (success) {
                // Update local session (AuthManager) to reflect the new bio instantly
                authManager.updateBio(newBio)

                Toast.makeText(this@EditProfileActivity, "Bio updated successfully!", Toast.LENGTH_LONG).show()
                finish() // Close and return to ProfileActivity
            } else {
                Toast.makeText(this@EditProfileActivity, "Update failed. Try again.", Toast.LENGTH_LONG).show()
                saveButton.isEnabled = true
                saveButton.text = "Save"
            }
        }
    }
}