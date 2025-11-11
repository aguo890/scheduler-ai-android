// Make sure this is your correct package name
package com.example.schedulerai_clean

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.schedulerai_clean.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // We can add a title to the action bar
        supportActionBar?.title = "Scheduler AI"
    }
}