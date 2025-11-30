package com.example.schedulerai_clean

object DebugLogger {
    private val logs = StringBuilder()

    fun log(text: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        logs.append("[$timestamp] $text\n")
    }

    fun getLogs(): String {
        return logs.toString()
    }

    fun clear() {
        logs.setLength(0)
    }
}