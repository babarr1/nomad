package com.example.nomad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.view.View
import android.widget.ProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.nomad.HomeActivity
import com.example.nomad.R

class LoginActivity : AppCompatActivity() {
    private lateinit var authManager: AuthManager
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupText: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authManager = AuthManager(this)

        // Initialize views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signupText = findViewById(R.id.signupText)

        // Add ProgressBar to your layout or create programmatically
        // For now, we'll just disable the button during loading

        // Handle Login Button Click
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perform login
            performLogin(email, password)
        }

        // Handle "don't have an account? sign up" Click
        signupText.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin(email: String, password: String) {
        // Disable button to prevent multiple clicks
        loginButton.isEnabled = false
        loginButton.text = "Logging in..."

        // Launch coroutine for network call
        CoroutineScope(Dispatchers.Main).launch {
            val result = authManager.performLogin(email, password)

            when (result) {
                is AuthManager.LoginResult.Success -> {
                    // Store session
                    authManager.login(
                        result.userId,
                        result.username,
                        result.email,
                        result.fullName,
                        result.sessionToken
                    )

                    Toast.makeText(
                        this@LoginActivity,
                        "Welcome back, ${result.username}!",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Navigate to home
                    navigateToHome()
                }

                is AuthManager.LoginResult.Error -> {
                    Toast.makeText(
                        this@LoginActivity,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()

                    // Re-enable button
                    loginButton.isEnabled = true
                    loginButton.text = "login"
                }
            }
        }
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}