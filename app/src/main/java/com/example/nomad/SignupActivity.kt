package com.example.nomad

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class SignupActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var signupButton: Button
    private lateinit var loginText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        authManager = AuthManager(this)
        firebaseAuth = FirebaseAuth.getInstance()

        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        signupButton = findViewById(R.id.signupButton)
        loginText = findViewById(R.id.loginText)

        signupButton.setOnClickListener {

            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            performSignup(name, email, password)
        }

        loginText.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun performSignup(name: String, email: String, password: String) {

        signupButton.isEnabled = false
        signupButton.text = "Creating account..."

        // ✅ STEP 1: Firebase Cloud Signup
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    // ✅ STEP 2: Local XAMPP Signup
                    CoroutineScope(Dispatchers.Main).launch {

                        val result = authManager.performSignup(name, email, password)

                        when (result) {
                            is AuthManager.SignupResult.Success -> {
                                Toast.makeText(
                                    this@SignupActivity,
                                    "Account created! Please log in.",
                                    Toast.LENGTH_LONG
                                ).show()

                                navigateToLogin()
                            }

                            is AuthManager.SignupResult.Error -> {
                                Toast.makeText(
                                    this@SignupActivity,
                                    result.message,
                                    Toast.LENGTH_LONG
                                ).show()

                                signupButton.isEnabled = true
                                signupButton.text = "SIGN UP"
                            }
                        }
                    }

                } else {

                    Toast.makeText(
                        this,
                        "Cloud Signup Failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    signupButton.isEnabled = true
                    signupButton.text = "SIGN UP"
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
