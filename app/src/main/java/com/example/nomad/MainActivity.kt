package com.example.nomad

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.nomad.AuthManager
import com.example.nomad.LoginActivity
import com.example.nomad.HomeActivity

class MainActivity : AppCompatActivity() {
    private lateinit var authManager: AuthManager
    private val splashDelay: Long = 2000 // 2 seconds delay for splash screen
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_main)

            // Initialize AuthManager
            authManager = AuthManager(this)

            Log.d(TAG, "MainActivity created successfully")

            // Handler to delay the check and navigation
            Handler(Looper.getMainLooper()).postDelayed({
                checkLoginStatusAndNavigate()
            }, splashDelay)

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            e.printStackTrace()
            // If there's an error, go to login screen
            navigateToLogin()
        }
    }

    /**
     * Checks if a user is already logged in and navigates accordingly.
     */
    private fun checkLoginStatusAndNavigate() {
        try {
            val isLoggedIn = authManager.isLoggedIn()
            Log.d(TAG, "User logged in status: $isLoggedIn")

            if (isLoggedIn) {
                navigateToHome()
            } else {
                navigateToLogin()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login status: ${e.message}", e)
            e.printStackTrace()
            // On error, navigate to login
            navigateToLogin()
        }
    }

    private fun navigateToHome() {
        try {
            val intent = Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to home: ${e.message}", e)
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to login: ${e.message}", e)
            e.printStackTrace()
        }
    }
}