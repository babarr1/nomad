package com.example.nomad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VerdantActivity : AppCompatActivity() {

    // --- Configuration ---
    // Fauna Category ID based on your database screenshot
    private val CATEGORY_ID = 2

    // Use the generic manager and adapter
    private lateinit var observationManager: ObservationManager
    private lateinit var observationAdapter: ObservationAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verdant)

        // 1. Initialize Manager and Views
        observationManager = ObservationManager(this)
        recyclerView = findViewById(R.id.verdantRecyclerView)

        // 2. Setup Data Display and Load Data
        setupRecyclerView()
        loadObservationData()

        // 3. Setup Navigation
        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish() // Go back to the previous activity (HomeActivity)
        }
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // ADD the data loading here so it refreshes every time the screen is viewed
        loadObservationData()
    }
    /**
     * Sets up the RecyclerView with the adapter and layout manager.
     */
    private fun setupRecyclerView() {
        // Initialize the adapter with an empty list of ObservationItems
        observationAdapter = ObservationAdapter(emptyList())
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@VerdantActivity, 1)
            adapter = observationAdapter
        }
    }

    /**
     * Executes the network call to fetch data for the Fauna category.
     */
    private fun loadObservationData() {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@VerdantActivity, "Loading Verdant...", Toast.LENGTH_SHORT).show()

            // Call the network function using the fixed CATEGORY_ID (1)
            val itemList = observationManager.fetchCategoryItems(CATEGORY_ID)

            if (itemList.isNotEmpty()) {
                // Update the adapter with the fetched list of ObservationItems
                observationAdapter.updateData(itemList)
            } else {
                Toast.makeText(this@VerdantActivity, "No fauna sightings found.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Handles all bottom navigation bar clicks.
     */
    private fun setupBottomNavigation() {
        val navHome = findViewById<ImageView>(R.id.navHome)
        val navSearch = findViewById<ImageView>(R.id.navSearch)
        val navAdd = findViewById<ImageView>(R.id.navAdd)
        val navProfile = findViewById<ImageView>(R.id.navProfile)

        // Home Button
        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                // Ensures HomeActivity is at the top of the stack
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        // Search Button
        navSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        // Add Button (Post Discovery)
        navAdd.setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }

        // Profile Button
        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}