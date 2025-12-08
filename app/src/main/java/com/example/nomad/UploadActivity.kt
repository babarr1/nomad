package com.example.nomad

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class UploadActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 100
    private var selectedImageUri: Uri? = null
    private var selectedCategoryId: Int = -1

    private lateinit var selectedImageView: ImageView
    private lateinit var categorySpinner: Spinner
    private lateinit var captionInput: EditText
    private lateinit var postButton: Button
    private lateinit var uploadManager: UploadManager
    private lateinit var initialPlaceholder: LinearLayout
    private lateinit var inputContainer: LinearLayout

    private val categoriesMap = mapOf(
        "FAUNA" to 1,
        "VERDANT" to 2,
        "STRATA" to 3,
        "PHENOMENON" to 4
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        selectedImageView = findViewById(R.id.selectedImageView)
        categorySpinner = findViewById(R.id.categorySpinner)
        captionInput = findViewById(R.id.captionInput)
        postButton = findViewById(R.id.postButton)
        initialPlaceholder = findViewById(R.id.initialPlaceholder)
        inputContainer = findViewById(R.id.inputContainer)
        uploadManager = UploadManager(this)

        val incomingUri: Uri? = intent.data
        if (incomingUri != null) {
            handleImageSelection(incomingUri)
        } else {
            openImagePicker()
        }

        setupCategorySpinner()
        findViewById<ImageView>(R.id.backBtn).setOnClickListener { finish() }

        val selectionArea = findViewById<RelativeLayout>(R.id.imageSelectionArea)
        selectionArea.setOnClickListener { openImagePicker() }

        postButton.setOnClickListener { performUpload() }

        openImagePicker()
    }

    private fun setupCategorySpinner() {
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
                selectedCategoryId = if (position == 0) -1 else categoriesMap[categoryNamesWithPrompt[position]] ?: -1
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == RESULT_OK && data != null) {
                data.data?.let {
                    handleImageSelection(it)
                }
            } else if (resultCode == RESULT_CANCELED && selectedImageUri == null) {
                Toast.makeText(this, "Upload cancelled.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun handleImageSelection(imageUri: Uri) {
        selectedImageUri = imageUri

        Picasso.get().load(imageUri).into(selectedImageView)

        initialPlaceholder.visibility = View.GONE
        inputContainer.visibility = View.VISIBLE
    }

    // ✅ ✅ ✅ OFFLINE + ONLINE UPLOAD LOGIC
    private fun performUpload() {

        val caption = captionInput.text.toString().trim()
        postButton.isEnabled = false

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

        if (caption.length < 5) {
            Toast.makeText(this, "Caption must be at least 5 characters.", Toast.LENGTH_SHORT).show()
            postButton.isEnabled = true
            return
        }

        val authManager = AuthManager(this)
        val userId = authManager.getUserId()

        if (userId == -1) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show()
            postButton.isEnabled = true
            return
        }

        postButton.text = "Uploading..."

        // ✅ ✅ ✅ OFFLINE CHECK
        if (!isInternetAvailable(this)) {

            // ✅ SAVE TO LOCAL QUEUE
            val inputStream = contentResolver.openInputStream(selectedImageUri!!)
            val fileName = "offline_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)

            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }


            val db = AppDatabase.getDatabase(this)
            val dao = db.pendingUploadDao()

            lifecycleScope.launch(Dispatchers.IO) {
                val pending = PendingUploadEntity(
                    imagePath = file.absolutePath,   // ✅ REAL FILE PATH NOW
                    caption = caption,
                    categoryId = selectedCategoryId
                )

                dao.insertUpload(pending)
            }

            Toast.makeText(
                this,
                "No internet. Post saved and will upload automatically.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // ✅ ✅ ✅ NORMAL ONLINE UPLOAD
        lifecycleScope.launch {

            val result = uploadManager.uploadDiscovery(
                userId = userId,
                categoryId = selectedCategoryId,
                caption = caption,
                imageUri = selectedImageUri!!
            )

            if (result is UploadManager.UploadResult.Success) {

                val categoryActivityClass = when (selectedCategoryId) {
                    1 -> FaunaActivity::class.java
                    2 -> VerdantActivity::class.java
                    3 -> StrataActivity::class.java
                    4 -> PhenomenaActivity::class.java
                    else -> HomeActivity::class.java
                }

                val intent = Intent(this@UploadActivity, categoryActivityClass).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()

            } else if (result is UploadManager.UploadResult.Error) {

                Toast.makeText(
                    this@UploadActivity,
                    "Upload failed: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()

                postButton.isEnabled = true
                postButton.text = "POST DISCOVERY"
            }
        }
    }
}
