package com.example.schedulerai_clean

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
// NEW IMPORTS FOR LANGUAGE
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.schedulerai_clean.databinding.ActivitySettingsBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This initializes the XML layout
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the text view to show current user email
        val user = Firebase.auth.currentUser
        binding.userEmailText.text = user?.email ?: "No User Logged In"

        // --- 1. CHECK CURRENT LANGUAGE ON STARTUP ---
        // We look at what the app is currently using to check the correct box
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        if (!currentLocales.isEmpty && currentLocales[0]?.language == "es") {
            binding.rbSpanish.isChecked = true
        } else {
            // Default to English
            binding.rbEnglish.isChecked = true
        }

        // --- 2. HANDLE LANGUAGE CLICKS ---
        binding.radioGroupLanguages.setOnCheckedChangeListener { _, checkedId ->
            val languageCode = when (checkedId) {
                binding.rbSpanish.id -> "es" // Switch to Spanish
                else -> "en"                 // Switch to English
            }

            // This forces the app to reload with the new language
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        }

        // Handle Logout
        binding.logoutButton.setOnClickListener {
            Firebase.auth.signOut()

            // Clear the back stack so user cannot press "Back" to return to the app
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Handle Back Button
        binding.backButton.setOnClickListener {
            finish() // This closes SettingsActivity and reveals the previous screen
        }
    }
}