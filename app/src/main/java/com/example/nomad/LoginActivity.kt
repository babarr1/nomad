package com.example.nomad

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signupText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authManager = AuthManager(this)
        firebaseAuth = FirebaseAuth.getInstance()

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signupText = findViewById(R.id.signupText)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performLogin(email, password)
        }

        signupText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun performLogin(email: String, password: String) {

        loginButton.isEnabled = false
        loginButton.text = "Checking cloud..."

        // ✅ STEP 1: Firebase Cloud Authentication
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    // ✅ STEP 2: Local XAMPP Authentication
                    loginButton.text = "Verifying account..."

                    CoroutineScope(Dispatchers.Main).launch {

                        val result = authManager.performLogin(email, password)

                        when (result) {
                            is AuthManager.LoginResult.Success -> {

                                authManager.login(
                                    userId = result.userId,
                                    username = result.username,
                                    email = result.email,
                                    fullName = result.fullName,
                                    sessionToken = result.sessionToken,
                                    profilePictureUrl = result.profilePictureUrl
                                )

                                Toast.makeText(
                                    this@LoginActivity,
                                    "Welcome back, ${result.username}!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                navigateToHome()
                            }

                            is AuthManager.LoginResult.Error -> {
                                Toast.makeText(
                                    this@LoginActivity,
                                    result.message,
                                    Toast.LENGTH_LONG
                                ).show()

                                loginButton.isEnabled = true
                                loginButton.text = "LOGIN"
                            }
                        }
                    }

                } else {

                    // ❌ Firebase Failed
                    Toast.makeText(
                        this,
                        "Cloud Login Failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    loginButton.isEnabled = true
                    loginButton.text = "LOGIN"
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
