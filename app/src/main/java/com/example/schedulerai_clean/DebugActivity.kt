package com.example.schedulerai_clean

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        val logView = findViewById<TextView>(R.id.logTextView)
        logView.text = DebugLogger.getLogs()
    }
}