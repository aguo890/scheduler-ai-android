# Scheduler AI 📅

**Scheduler AI** is an intelligent Android assistant that converts natural language chat messages into real events on your device's native calendar. It bridges the gap between conversational AI and the native Android ecosystem, creating a seamless scheduling experience.

This project was developed for the "Software Design for Handheld Devices" course.

## ✨ Features

*   **Natural Language Processing:** Type commands like *"Lunch with the Professor tomorrow at 12pm"* or *"Team meeting next Friday at 3"* and the app understands.
*   **DeepSeek AI Integration:** Uses the DeepSeek API to parse unstructured text into structured JSON data, including ISO-8601 timestamps.
*   **Native Calendar Sync:** Bypasses complex web OAuth by using Android's native `ContentResolver` and `CalendarContract` for direct, permission-based calendar access.
*   **Real-time Chat:** The chat interface is powered by Firebase Realtime Database.
*   **User Authentication:** Secure login and registration handled by Firebase Authentication.
*   **In-App Language Switching:** Instantly toggle the app's language between English and Spanish from the settings menu without changing device-wide settings.

## 🛠️ Tech Stack

*   **Language:** Kotlin
*   **UI:** XML / Android Views with View Binding
*   **Backend:** Firebase Authentication & Realtime Database
*   **AI API:** DeepSeek (HTTP/JSON parsing via OkHttp)
*   **Android Components:**
    *   `ContentResolver` & `CalendarContract` for native calendar operations.
    *   `ActivityCompat` for runtime permissions (`READ_CALENDAR`, `WRITE_CALENDAR`).
    *   `AppCompatDelegate` for per-app language preferences.

## ⚙️ Setup & Installation

To build and run this project yourself, follow these steps:

1.  **Clone the Repo:**
    ```bash
    git clone https://github.com/your-username/scheduler-ai.git
    ```

2.  **Configure API Keys:**
    *   In the root directory of the project, create a file named `secrets.properties`.
    *   Add your DeepSeek API key to this file:
        ```properties
        DEEPSEEK_API_KEY="sk-your-actual-api-key-here"
        ```

3.  **Firebase Setup:**
    *   Follow the Firebase console instructions to create a project.
    *   Download your `google-services.json` file and place it in the `/app` folder of the project.
    *   Enable **Authentication** (Email/Password) and the **Realtime Database** in the Firebase console.

4.  **Build and Run:**
    *   Open the project in Android Studio and let it sync.
    *   Run the app on an emulator or a physical device.

5.  **Permissions:**
    *   On first launch, the app will request `READ_CALENDAR` and `WRITE_CALENDAR` permissions. You must **Allow** these for the app's core functionality to work.

## 📝 How to Use

1.  **Login/Sign Up:** Create an account or log in.
2.  **Chat:** In the main chat window, type a scheduling request.
    *   *Example:* "Dinner with Mom next Tuesday at 7pm."
3.  **Confirm:** Wait for the confirmation toast message.
4.  **View:** Open your phone's native Calendar app (e.g., Google Calendar) to see the event populated automatically.
