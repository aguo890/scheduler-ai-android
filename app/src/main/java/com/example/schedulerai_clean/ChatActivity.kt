package com.example.schedulerai_clean

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.schedulerai_clean.databinding.ActivityChatBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ADDED: TextToSpeech.OnInitListener interface
class ChatActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityChatBinding
    private val client = OkHttpClient()

    // YOUR API KEY
    private val DEEPSEEK_API_KEY = "sk-8ff5853377a040bbaba812a575d50089"

    // Recycler View Vars
    private val messageList = mutableListOf<Message>()
    private lateinit var chatAdapter: ChatAdapter

    // ADDED: Text To Speech Object
    private lateinit var tts: TextToSpeech
    private val SPEECH_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ADDED: Initialize TTS
        tts = TextToSpeech(this, this)

        // Setup RecyclerView
        chatAdapter = ChatAdapter(messageList)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.adapter = chatAdapter

// Initial Greeting
// We get the string from resources. Android automatically picks English or Spanish based on phone settings.
        val greeting = getString(R.string.greeting_orion)
        addMessageToChat(greeting, false)
        // Note: We usually don't speak the long intro text automatically to avoid annoyance,
        // but if you want to, uncomment: speakResponse("Hello, I am Orion. How can I help?")

        binding.sendButton.setOnClickListener {
            val userText = binding.inputEditText.text.toString()
            if (userText.isNotBlank()) {
                addMessageToChat(userText, true)
                saveChatToFirebase("User", userText)
                processWithAI(userText)
                binding.inputEditText.text.clear()
            }
        }

        // ADDED: Microphone Button Logic
        binding.micButton.setOnClickListener {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speech_prompt))
            try {
                startActivityForResult(intent, SPEECH_REQUEST_CODE)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.error_speech_not_supported), Toast.LENGTH_SHORT).show()
            }
        }

        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.viewLogsButton.setOnClickListener {
            listAllCalendarsToLogs()
            startActivity(Intent(this, DebugActivity::class.java))
        }
    }

    // ADDED: Handle Voice Result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""
            // Populate EditText so user can verify before sending
            binding.inputEditText.setText(spokenText)
        }
    }

    // ADDED: TTS Init Listener
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                DebugLogger.log("TTS: Language not supported")
            }
        } else {
            DebugLogger.log("TTS: Initialization failed")
        }
    }

    // ADDED: Helper to speak text
    private fun speakResponse(text: String) {
        // Strip out markdown symbols (*, #, etc) so it speaks cleanly
        val cleanText = text.replace("*", "").replace("#", "")
        tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }

    private fun addMessageToChat(text: String, isUser: Boolean) {
        messageList.add(Message(text, isUser))
        chatAdapter.notifyItemInserted(messageList.size - 1)
        binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun processWithAI(prompt: String) {
        // 1. UI FEEDBACK
        val thinkingMessage = getString(R.string.msg_consulting)
        addMessageToChat(thinkingMessage, false)
        binding.sendButton.isEnabled = false
        binding.inputEditText.isEnabled = false

        DebugLogger.log("USER INPUT: $prompt")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Determine 'today' dynamically
                val now = System.currentTimeMillis()
                val dayFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.US)
                val todayStr = dayFormat.format(Date(now))

                // --- SYSTEM PROMPT ---
                val systemPrompt = "You are Orion, an intelligent executive assistant. Today is $todayStr. " +
                        "Determine if the user wants to CREATE, READ, or DELETE an event.\n\n" +
                        "1. If CREATING (e.g., 'Lunch tomorrow'):\n" +
                        "Return JSON: {\"action\": \"create\", \"title\": \"...\", \"dateString\": \"yyyy-MM-dd HH:mm\"}\n\n" +
                        "2. If READING (e.g., 'What is my schedule?'):\n" +
                        "Return JSON: {\"action\": \"read\", \"startDate\": \"yyyy-MM-dd\", \"endDate\": \"yyyy-MM-dd\"}\n\n" +
                        "3. If DELETING (e.g., 'Cancel lunch', 'Remove meeting with Bob'):\n" +
                        "Return JSON: {\"action\": \"delete\", \"title\": \"...\", \"dateString\": \"yyyy-MM-dd\"}\n" +
                        "(For delete, try to guess the specific date based on the user request)."

                val jsonBody = JSONObject()
                jsonBody.put("model", "deepseek-chat")
                jsonBody.put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })

                val request = Request.Builder()
                    .url("https://api.deepseek.com/chat/completions")
                    .addHeader("Authorization", "Bearer $DEEPSEEK_API_KEY")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                withContext(Dispatchers.Main) {
                    // Remove thinking bubble
                    if (messageList.isNotEmpty()) {
                        messageList.removeAt(messageList.size - 1)
                        chatAdapter.notifyItemRemoved(messageList.size)
                    }
                    binding.sendButton.isEnabled = true
                    binding.inputEditText.isEnabled = true

                    if (response.isSuccessful && responseBody != null) {
                        val jsonResponse = JSONObject(responseBody)
                        val content = jsonResponse.getJSONArray("choices")
                            .getJSONObject(0).getJSONObject("message").getString("content")

                        val cleanJson = content.replace("```json", "").replace("```", "").trim()

                        DebugLogger.log("AI RESPONSE RAW: $content")

                        // Parse JSON to decide Action
                        try {
                            val jsonObject = JSONObject(cleanJson)
                            val action = jsonObject.optString("action", "unknown")
                            var spokenResponse = ""

                            if (action == "create") {
                                spokenResponse = handleCreateEvent(jsonObject, prompt)
                            } else if (action == "read") {
                                spokenResponse = handleReadRequest(jsonObject)
                            } else if (action == "delete") {
                                spokenResponse = handleDeleteEvent(jsonObject)
                            } else {
                                val msg = "🤔 Orion is unsure. I specialize in Scheduling."
                                addMessageToChat(msg, false)
                                spokenResponse = msg
                            }

                            saveChatToFirebase("AI", content)

                            // ADDED: Make Orion Speak!
                            speakResponse(spokenResponse)

                        } catch (e: Exception) {
                            DebugLogger.log("JSON PARSE ERROR: ${e.message}")
                            addMessageToChat("Orion had trouble reading that request.", false)
                        }
                    } else {
                        addMessageToChat("Error: AI did not respond.", false)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (messageList.isNotEmpty()) {
                        messageList.removeAt(messageList.size - 1)
                        chatAdapter.notifyItemRemoved(messageList.size)
                    }
                    binding.sendButton.isEnabled = true
                    binding.inputEditText.isEnabled = true
                    addMessageToChat("Server error. Check internet.", false)
                }
            }
        }
    }

    // MODIFIED: Functions now return String so TTS knows what to say

    // --- LOGIC 1: CREATE EVENT ---
    private fun handleCreateEvent(json: JSONObject, rawUserPrompt: String): String {
        if (checkPermissions()) {
            try {
                val title = json.getString("title")
                val dateString = json.getString("dateString")
                DebugLogger.log("ACTION: CREATE -> $title @ $dateString")

                val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                val dateObject = format.parse(dateString)
                val startTime = dateObject?.time ?: System.currentTimeMillis()
                val endTime = startTime + (60 * 60 * 1000) // Default 1 hour duration

                // Conflict Check
                val selection = "(((${CalendarContract.Events.DTSTART} < ?) AND (${CalendarContract.Events.DTEND} > ?)))"
                val selectionArgs = arrayOf(endTime.toString(), startTime.toString())

                val cursor = contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events.TITLE),
                    selection,
                    selectionArgs,
                    null
                )

                val hasConflict = (cursor?.count ?: 0) > 0
                var conflictTitle = ""
                if (cursor != null && cursor.moveToFirst()) {
                    conflictTitle = cursor.getString(0)
                    cursor.close()
                }

                if (hasConflict && !rawUserPrompt.lowercase().contains("force")) {
                    val msg = "✋ Hold on! Conflict detected with '$conflictTitle'. Reply 'Force $title' to double-book."
                    addMessageToChat(msg, false)
                    return msg
                }

                val calendarIds = getAllGoogleCalendarIds()
                if (calendarIds.isEmpty()) {
                    addMessageToChat("No Google Accounts found on device.", false)
                    return "No calendars found."
                }

                for (calID in calendarIds) {
                    val values = ContentValues().apply {
                        put(CalendarContract.Events.DTSTART, startTime)
                        put(CalendarContract.Events.DTEND, endTime)
                        put(CalendarContract.Events.TITLE, title)
                        put(CalendarContract.Events.CALENDAR_ID, calID)
                        put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                    }
                    contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                }

                val dayNum = SimpleDateFormat("d", Locale.US).format(dateObject).toInt()
                val suffix = if (dayNum in 11..13) "th" else when (dayNum % 10) { 1 -> "st" 2 -> "nd" 3 -> "rd" else -> "th" }
                val niceDatePart1 = SimpleDateFormat("MMMM d", Locale.US).format(dateObject)
                val niceDatePart2 = SimpleDateFormat(", yyyy 'at' h:mm a", Locale.US).format(dateObject)
                val finalFriendlyDate = "$niceDatePart1$suffix$niceDatePart2"

                val userFeedback = "✅ Orion has secured '$title' for $finalFriendlyDate."
                addMessageToChat(userFeedback, false)
                Toast.makeText(this, "Saved to ${calendarIds.size} calendars", Toast.LENGTH_SHORT).show()
                return "I have booked $title."

            } catch (e: Exception) {
                DebugLogger.log("CREATE ERROR: ${e.message}")
            }
        }
        return "Permission required or error occurred."
    }

    // --- LOGIC 3: DELETE EVENT ---
    private fun handleDeleteEvent(json: JSONObject): String {
        if (checkPermissions()) {
            try {
                val titleQuery = json.getString("title")
                val dateString = json.getString("dateString")
                DebugLogger.log("ACTION: DELETE -> $titleQuery @ $dateString")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val startMillis = dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
                val endMillis = startMillis + (24 * 60 * 60 * 1000)

                val selection = "${CalendarContract.Events.TITLE} LIKE ? AND ${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
                val selectionArgs = arrayOf("%$titleQuery%", startMillis.toString(), endMillis.toString())

                val cursor = contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    arrayOf(CalendarContract.Events._ID, CalendarContract.Events.TITLE),
                    selection,
                    selectionArgs,
                    null
                )

                var deletedCount = 0
                cursor?.use {
                    while (it.moveToNext()) {
                        val eventId = it.getLong(0)
                        val eventTitle = it.getString(1)
                        val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
                        val rows = contentResolver.delete(deleteUri, null, null)
                        if (rows > 0) deletedCount++
                    }
                }

                if (deletedCount > 0) {
                    val msg = "🗑️ As requested, I have removed '$titleQuery' from your agenda."
                    addMessageToChat(msg, false)
                    return "Deleted $titleQuery."
                } else {
                    val msg = "I couldn't find a meeting matching '$titleQuery' on that day."
                    addMessageToChat(msg, false)
                    return msg
                }

            } catch (e: Exception) {
                DebugLogger.log("DELETE ERROR: ${e.message}")
            }
        }
        return "Error deleting event."
    }

    // --- LOGIC 2: READ EVENTS ---
    private fun handleReadRequest(json: JSONObject): String {
        if (checkPermissions()) {
            try {
                val startDateStr = json.getString("startDate")
                val endDateStr = json.getString("endDate")
                DebugLogger.log("ACTION: READ -> $startDateStr to $endDateStr")

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val startMillis = dateFormat.parse(startDateStr)?.time ?: System.currentTimeMillis()
                val endMillis = (dateFormat.parse(endDateStr)?.time ?: System.currentTimeMillis()) + (24 * 60 * 60 * 1000)

                val projection = arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART)
                val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
                val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

                val cursor = contentResolver.query(
                    CalendarContract.Events.CONTENT_URI, projection, selection, selectionArgs, "${CalendarContract.Events.DTSTART} ASC"
                )

                val eventsFound = StringBuilder()
                val displayFormat = SimpleDateFormat("EEE HH:mm", Locale.US)
                var count = 0

                cursor?.use {
                    while (it.moveToNext()) {
                        val title = it.getString(0)
                        val time = it.getLong(1)
                        eventsFound.append("• ${displayFormat.format(Date(time))}: $title\n")
                        count++
                    }
                }

                if (eventsFound.isNotEmpty()) {
                    addMessageToChat("Here is your schedule:\n\n$eventsFound", false)
                    return "You have $count events scheduled."
                } else {
                    addMessageToChat("Your schedule looks clear.", false)
                    return "Your schedule is clear."
                }

            } catch (e: Exception) {
                DebugLogger.log("READ ERROR: ${e.message}")
            }
        }
        return "I could not read the calendar."
    }

    private fun checkPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR), 100)
            return false
        }
        return true
    }

    private fun listAllCalendarsToLogs() {
        // ... (Code unchanged, purely for logging)
        DebugLogger.log("--- SCANNING CALENDARS ---")
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_NAME)
        val cursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                DebugLogger.log("ID: ${it.getLong(0)} - Account: ${it.getString(1)}")
            }
        }
    }

    private fun getAllGoogleCalendarIds(): List<Long> {
        val ids = mutableListOf<Long>()
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_NAME)
        val cursor = contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(0)
                val name = it.getString(1).lowercase()
                if (name.contains("gwu") || name.contains("gmail") || name.contains("google")) {
                    ids.add(id)
                }
            }
        }
        return ids
    }

    private fun saveChatToFirebase(role: String, message: String) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val database = Firebase.database
        val myRef = database.getReference("chats/$uid").push()
        myRef.setValue(mapOf("role" to role, "message" to message))
    }
}