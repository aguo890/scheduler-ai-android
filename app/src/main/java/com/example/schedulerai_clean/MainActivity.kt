package com.example.schedulerai_clean

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A views project needs a layout file, even if it's empty
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
    }

    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Not signed in, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            // Already signed in, go to ChatActivity
            startActivity(Intent(this, ChatActivity::class.java))
        }
        finish() // Close this activity, we don't need it in the back stack
    }
}