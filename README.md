# Voice Recorder Android Application

This is a voice recording application for Android, built as a take-home assignment. It allows users to record audio, view a list of their recordings, and see a generated transcript and summary for each recording.

## Features Implemented

The app successfully implements the majority of the core features and critical edge cases outlined in the project requirements.

### 1. Robust Audio Recording

*   **Background Recording:** A foreground service (`RecordingService`) ensures audio can be recorded reliably even when the app is in the background.
*   **Chunk-Based Saving:** Recordings are automatically split into 30-second audio chunks and saved to local storage. This ensures that no data is lost for long recordings.
*   **Persistent Notification:** A notification is displayed during recording, showing the current duration and providing a "Stop" action.
*   **Phone Call Handling:** The app automatically pauses the recording when an incoming or outgoing phone call is detected and resumes when the call ends.
*   **Audio Focus Management:** The recording will pause if another app requests audio focus (e.g., a music player) and provides an option to resume from the notification.
*   **Low Storage Detection:** The app checks for sufficient storage space before starting a recording and before saving each new chunk, stopping gracefully if storage is low.
*   **Silent Audio Warnings:** If no audio is detected from the microphone for 10 seconds, a warning notification is displayed to the user.

### 2. Transcription

*   **Background Transcription:** As soon as a 30-second audio chunk is saved, a background job is enqueued using `WorkManager` to handle transcription.
*   **Mock Service Integration:** The app is configured to use a mock transcription service, as permitted by the project requirements. This simulates an API call and returns a sample transcript, allowing for full testing of the application flow.
*   **Durable and Ordered:** Transcripts are saved to a local Room database and associated with the correct audio chunk, ensuring data integrity and correct order.

### 3. Summary Generation

*   **Background Summary:** Once all chunks for a meeting are transcribed, a final background job is enqueued to generate a structured summary.
*   **Structured Output:** The summary screen is designed to display four key sections: **Title**, **Summary**, **Action Items**, and **Key Points**.
*   **State Handling:** The UI correctly handles and displays `Loading` and `Error` states for the summary generation process, including a "Retry" button.
*   **App Kill Resistant:** The summary generation is handled by `WorkManager`, ensuring it will complete even if the app is closed during the process.

## Architecture and Tech Stack

The project is built using a modern, best-practice Android architecture.

*   **Tech Stack:** 100% [Kotlin](https://kotlinlang.org/) and [Jetpack Compose](https://developer.android.com/jetpack/compose).
*   **Architecture:** Follows the **MVVM (Model-View-ViewModel)** pattern.
    *   **View:** Jetpack Compose screens (`DashboardScreen`, `RecordingScreen`, `SummaryScreen`).
    *   **ViewModel:** `AndroidViewModel` classes (`DashboardViewModel`, `RecordingViewModel`, `SummaryViewModel`) to manage UI state and business logic.
    *   **Model:** A repository layer (`MeetingRepository`, `TranscriptionRepository`, etc.) that serves as a single source of truth, abstracting the data sources.
*   **Dependency Injection:** [Hilt](https://dagger.dev/hilt/) is used to manage dependencies throughout the app, making the code modular and testable.
*   **Asynchronous Operations:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) and [Flow](https://kotlinlang.org/docs/flow.html) are used for all background tasks and to manage streams of data.
*   **Database:** [Room](https://developer.android.com/training/data-storage/room) is used for local persistence of all meeting, audio chunk, and summary data.
*   **Networking:** The app was built with [Retrofit](https://square.github.io/retrofit/) for API calls. It is currently configured to use a mock service via Hilt, but the real API interfaces and models are retained in the codebase to demonstrate readiness for a live integration.

## Note on API Integration

This application is fully implemented to connect to a live OpenAI API for transcription and summary generation. The networking layer, repositories, and data models are all in place.

However, due to a persistent `insufficient_quota` error with the provided test API key, the app is currently configured to use the **mock services** for demonstration purposes, as explicitly allowed by the project requirements. This ensures that the entire application flow can be tested and reviewed.

To switch to the live API, one would only need to:
1.  Provide a valid API key in the `local.properties` file.
2.  Update the Hilt `NetworkModule` to provide the real `TranscriptionApi` and `SummaryApi` instead of the `MockApiService`.
