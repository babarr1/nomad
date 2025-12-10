package com.example.nomad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class StrataActivity : AppCompatActivity() {

    // --- Configuration ---
    private val CATEGORY_ID = 3
    private val CATEGORY_NAME = "Strata"

    private lateinit var observationManager: ObservationManager
    private lateinit var observationAdapter: ObservationAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strata)

        observationManager = ObservationManager(this)
        recyclerView = findViewById(R.id.strataRecyclerView)

        setupRecyclerView()

        findViewById<ImageView>(R.id.backBtn).setOnClickListener {
            finish()
        }

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadObservationData()
    }

    private fun setupRecyclerView() {
        observationAdapter = ObservationAdapter(emptyList())
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@StrataActivity, 1)
            adapter = observationAdapter
        }
    }

    // ✅ ✅ ✅ OFFLINE + ONLINE SMART LOADING
    private fun loadObservationData() {

        val db = AppDatabase.getDatabase(this)
        val dao = db.offlinePostDao()

        if (isInternetAvailable(this)) {

            Toast.makeText(this, "Online - Loading from server", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {

                val itemList = observationManager.fetchCategoryItems(CATEGORY_ID)

                if (itemList.isNotEmpty()) {

                    observationAdapter.updateData(itemList)

                    // ✅ SAVE TO ROOM CACHE
                    val offlinePosts = itemList.map { post ->
                        OfflinePostEntity(
                            observationId = post.id,
                            imageUrl = post.imageUrl,
                            caption = post.description,
                            category = CATEGORY_NAME,
                            username = post.title,
                            timestamp = post.dateObserved ?: ""
                        )
                    }

                    dao.insertPosts(offlinePosts)

                } else {
                    Toast.makeText(
                        this@StrataActivity,
                        "No Strata sightings found.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } else {

            // ✅ ✅ ✅ OFFLINE MODE
            Toast.makeText(
                this,
                "Offline Mode – Showing Saved Strata",
                Toast.LENGTH_SHORT
            ).show()

            lifecycleScope.launch {

                val cachedPosts = dao.getPostsByCategory(CATEGORY_NAME)

                if (cachedPosts.isNotEmpty()) {

                    val fakePosts = cachedPosts.map {
                        ObservationManager.ObservationItem(
                            id = it.observationId,
                            userId = 0,
                            categoryId = CATEGORY_ID,
                            title = it.username,
                            description = it.caption,
                            imageUrl = it.imageUrl,
                            latitude = null,
                            longitude = null,
                            dateObserved = it.timestamp
                        )
                    }

                    observationAdapter.updateData(fakePosts)

                } else {
                    Toast.makeText(
                        this@StrataActivity,
                        "No offline data available yet.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupBottomNavigation() {

        val navHome = findViewById<ImageView>(R.id.navHome)
        val navSearch = findViewById<ImageView>(R.id.navSearch)
        val navAdd = findViewById<ImageView>(R.id.navAdd)
        val navProfile = findViewById<ImageView>(R.id.navProfile)

        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        navSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        navAdd.setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}
