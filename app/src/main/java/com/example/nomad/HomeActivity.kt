package com.example.nomad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.nomad.R
import com.example.nomad.LoginActivity
import com.example.nomad.AuthManager

class HomeActivity : Activity() {
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        authManager = AuthManager(this)

        // Display username
        val usernameText = findViewById<TextView>(R.id.usernameText)
        val username = authManager.getUsername()
        usernameText.text = username ?: "User"

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

        // --- 3. Connect Logout Button ---
        val logoutButton = findViewById<ImageView>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            performLogout()
        }

        // Add listeners for the four category layouts (FAUNA, VERDANT, STRATA, PHENOMENON) here if needed.
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