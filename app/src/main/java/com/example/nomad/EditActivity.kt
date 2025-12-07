package com.example.nomad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.squareup.picasso.Picasso
import com.example.nomad.ObservationManager.ObservationItem

class EditActivity : AppCompatActivity() {

    private var observationId: Int = -1
    private lateinit var observationManager: ObservationManager
    private lateinit var postActionManager: PostActionManager

    // UI elements
    private lateinit var editImageView: ImageView
    private lateinit var captionInput: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var saveButton: Button

    // Map used to translate category name (UI) to category ID (DB)
    private val categoriesMap = mapOf(
        "FAUNA" to 1,
        "VERDANT" to 2,
        "STRATA" to 3,
        "PHENOMENON" to 4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_post)

        // --- Initialization ---
        observationId = intent.getIntExtra("OBSERVATION_ID", -1)

        if (observationId == -1) {
            Toast.makeText(this, "Error: Post ID not found.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        Log.d("EditDebug", "Post ID received: $observationId")

        // Initialize Managers
        observationManager = ObservationManager(this)
        postActionManager = PostActionManager(this)

        // Initialize Views (using IDs from activity_edit_post.xml)
        editImageView = findViewById(R.id.editImageView)
        captionInput = findViewById(R.id.editCaptionInput)
        categorySpinner = findViewById(R.id.editCategorySpinner)
        saveButton = findViewById(R.id.saveButton)

        // --- Setup Actions ---
        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish() // FIX for Issue #3 (Back button)
        }
        setupCategorySpinner() // Must be called before loading data

        // 1. Load the current data for the post
        loadCurrentPostData(observationId)

        // 2. Setup save listener
        saveButton.setOnClickListener { performUpdate() }
    }

    // --- Helper Functions ---

    private fun setupCategorySpinner() {
        val categoryNames = categoriesMap.keys.toList()
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categoryNames
        )
        categorySpinner.adapter = adapter
    }

    private fun getCategoryIdFromSpinner(): Int {
        val selectedCategoryName = categorySpinner.selectedItem?.toString()?.uppercase() ?: ""
        return categoriesMap[selectedCategoryName] ?: -1
    }

    private fun setSpinnerSelection(categoryId: Int) {
        val adapter = categorySpinner.adapter

        // Find the category name that corresponds to the fetched ID (e.g., 1 -> FAUNA)
        val categoryName = categoriesMap.entries.find { it.value == categoryId }?.key

        if (categoryName != null && adapter != null) {
            val position = (0 until adapter.count).firstOrNull {
                adapter.getItem(it).toString().equals(categoryName, ignoreCase = true)
            }
            if (position != null) {
                categorySpinner.setSelection(position)
            }
        }
    }

    // --- Core Data Loading ---

    private fun loadCurrentPostData(id: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("EditDebug", "Attempting to fetch post: $id")
            Toast.makeText(this@EditActivity, "Loading post data...", Toast.LENGTH_SHORT).show()

            val item = observationManager.fetchSingleObservation(id)

            if (item != null) {
                Log.d("EditDebug", "Data successfully fetched: ${item.title}")

                // FIX: Populate fields
                captionInput.setText(item.title)
                setSpinnerSelection(item.categoryId)

                // Load Image Preview (using Picasso)
                if (item.imageUrl.isNotEmpty()) {
                    Picasso.get().load(item.imageUrl).into(editImageView)
                }

            } else {
                Log.e("EditDebug", "Failed to fetch post data for ID: $id")
                Toast.makeText(this@EditActivity, "Error fetching post data. Closing.", Toast.LENGTH_LONG).show()
                finish() // Closes the Activity, which is the immediate close you observed.
            }
        }
    }

    // --- Update Logic ---

    private fun performUpdate() {
        val newCaption = captionInput.text.toString().trim()
        val newCategoryId = getCategoryIdFromSpinner() // FIX for Issue #2 (Category not changing)

        // Validation Check
        if (newCaption.isEmpty() || newCategoryId == -1) {
            Toast.makeText(this, "Caption and Category are required.", Toast.LENGTH_SHORT).show()
            return
        }

        saveButton.isEnabled = false
        saveButton.text = "Saving..."

        CoroutineScope(Dispatchers.Main).launch {
            val success = postActionManager.updateObservation(
                observationId,
                AuthManager(this@EditActivity).getUserId(),
                newCategoryId,
                newCaption
            )

            if (success) {
                Toast.makeText(this@EditActivity, "Post updated!", Toast.LENGTH_LONG).show()
                finish() // Go back to the feed
            } else {
                Toast.makeText(this@EditActivity, "Update failed. Check credentials/connection.", Toast.LENGTH_LONG).show()
                saveButton.isEnabled = true
                saveButton.text = "Save"
            }
        }
    }
}