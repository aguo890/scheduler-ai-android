package com.example.schedulerai_clean

data class Message(
    val text: String,
    val isUser: Boolean, // true = Right side (User), false = Left side (AI)
    val timestamp: Long = System.currentTimeMillis()
)