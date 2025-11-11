//LoginActivity.kt

package com.example.schedulerai_clean

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.schedulerai_clean.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Set OnClick Listeners for the buttons
        binding.loginButton.setOnClickListener {
            loginUser()
        }

        binding.signUpButton.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Email and Password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                Toast.makeText(this@LoginActivity, "Sign up successful! Please log in.", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun loginUser() {
        val email = binding.emailEditText.text.toString()
        val password = binding.passwordEditText.text.toString()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Email and Password cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)
        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                // Navigate to the main app screen on success
                startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                finish() // Close LoginActivity so user can't go back
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, e.message, Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}