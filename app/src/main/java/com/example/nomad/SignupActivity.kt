package com.example.nomad

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.nomad.R
class SignupActivity : AppCompatActivity() {
    private lateinit var authManager: AuthManager
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signupButton: Button
    private lateinit var loginText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        authManager = AuthManager(this)

        // Initialize views
        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        signupButton = findViewById(R.id.signupButton)
        loginText = findViewById(R.id.loginText)

        // Handle Sign Up Button Click
        signupButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perform signup
            performSignup(name, email, password)
        }

        // Handle "already have an account? login" Click
        loginText.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun performSignup(name: String, email: String, password: String) {
        // Disable button to prevent multiple clicks
        signupButton.isEnabled = false
        signupButton.text = "Creating account..."

        // Launch coroutine for network call
        CoroutineScope(Dispatchers.Main).launch {
            val result = authManager.performSignup(name, email, password)

            when (result) {
                is AuthManager.SignupResult.Success -> {
                    Toast.makeText(
                        this@SignupActivity,
                        "Account created successfully! Please log in.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Navigate to login
                    navigateToLogin()
                }

                is AuthManager.SignupResult.Error -> {
                    Toast.makeText(
                        this@SignupActivity,
                        result.message,
                        Toast.LENGTH_LONG
                    ).show()

                    // Re-enable button
                    signupButton.isEnabled = true
                    signupButton.text = "sign up"
                }
            }
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }
}