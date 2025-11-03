# Voice Recording & Transcription App

A robust Android application for recording voice with automatic transcription and AI-powered summary generation. Built with modern Android development practices using Kotlin, Jetpack Compose, and MVVM architecture.

## 📋 Overview

This app provides a complete solution for voice recording with intelligent handling of real-world edge cases including phone calls, audio focus changes, storage limitations, and process death scenarios.

### Key Features

✅ **Robust Audio Recording**
- Foreground service with persistent notification
- 30-second chunks with 2-second overlap
- Background recording support
- Automatic chunk management

✅ **Edge Case Handling**
- Phone call interruption (pause/resume)
- Audio focus loss detection
- Microphone source changes (Bluetooth/wired headsets)
- Low storage detection
- Process death recovery
- Silent audio detection

✅ **Automatic Transcription**
- Background processing with WorkManager
- Chunk-by-chunk transcription
- Automatic retry on failure
- Room database for persistence

✅ **AI-Powered Summaries**
- Structured summary generation
- Title, Summary, Action Items, Key Points
- Streaming UI updates
- Background generation (survives app kill)

## 🎯 Technical Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose (100%)
- **Architecture**: MVVM + Repository Pattern
- **Dependency Injection**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Async**: Coroutines + Flow
- **Background Work**: WorkManager + Foreground Service
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

## 🚀 Quick Start

### Prerequisites
- Android Studio (Arctic Fox or later)
- JDK 17 or higher
- Android SDK (API 24-34)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Sannidhi1112/twinmind.git
   cd twinmind
   ```

2. **Open in Android Studio**
   - File → Open → Select `twinmind` folder
   - Wait for Gradle sync to complete

3. **Run the app**
   - Select device/emulator
   - Click Run button (▶️)

For detailed setup instructions, see [SETUP_INSTRUCTIONS.md](SETUP_INSTRUCTIONS.md)

## 📱 App Flow

1. **Dashboard** → View all recordings
2. **Start Recording** → Tap + button → Grant permissions
3. **Record** → Speak into microphone → See live timer
4. **Stop** → Recording saved automatically
5. **Transcription** → Automatic background processing
6. **Summary** → AI-generated summary with action items

## 🏗️ Architecture

### MVVM + Clean Architecture

```
UI Layer (Compose) → ViewModel → Repository → Data Sources (Room + Retrofit)
```

### Key Components

- **Data Layer**: Room database, Retrofit APIs, Repositories
- **Domain Layer**: Business logic, Use cases
- **UI Layer**: Jetpack Compose screens, ViewModels
- **Services**: RecordingService (foreground), WorkManager workers

## 📂 Project Structure

```
app/src/main/java/com/twinmind/voicerecorder/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # API services
│   └── repository/     # Repository implementations
├── di/                 # Hilt dependency injection
├── service/            # RecordingService, Workers
├── ui/
│   ├── screen/        # Compose screens
│   ├── viewmodel/     # ViewModels
│   ├── navigation/    # Navigation setup
│   └── theme/         # Material 3 theme
└── utils/             # Utility classes
```

## 🔧 Configuration

### Mock API (Default)
The app uses mock services by default - no API keys required.

### Real API Integration

#### OpenAI
1. Get API key from: https://platform.openai.com/api-keys
2. Add to `local.properties`:
   ```
   OPENAI_API_KEY=sk-...
   ```
3. Update `TranscriptionRepository.kt` and `SummaryRepository.kt`

#### Google Gemini
1. Get API key from: https://makersuite.google.com/app/apikey
2. Add to `local.properties`:
   ```
   GEMINI_API_KEY=...
   ```
3. Update repository implementations

## 🎨 UI Screens

### Dashboard
- Meeting list with status
- Transcription progress
- FAB for new recording

### Recording
- Live timer display
- Status indicators
- Record/Stop controls

### Summary
- Title section
- Summary text
- Action items (checkboxes)
- Key points (bullets)

## 🧪 Testing

### Test Scenarios
- ✅ Basic recording flow
- ✅ Phone call interruption
- ✅ Audio focus loss
- ✅ Storage limits
- ✅ Process death recovery
- ✅ Transcription with retry
- ✅ Summary generation

## 🔐 Permissions

- `RECORD_AUDIO` - Required for recording
- `FOREGROUND_SERVICE` - Background recording
- `POST_NOTIFICATIONS` - Android 13+ notifications
- `READ_PHONE_STATE` - Phone call detection (optional)

## 📊 Technical Highlights

### Edge Case Handling
- **Phone Calls**: Automatic pause/resume
- **Audio Focus**: Smart handling of interruptions
- **Storage**: Pre-flight checks before recording
- **Process Death**: State persistence with Room
- **Silent Audio**: 10-second detection with warning

### Data Flow
1. Record → Save chunk to Room
2. Trigger WorkManager for transcription
3. API call → Save transcript to Room
4. All chunks complete → Generate summary
5. Stream summary updates to UI via Flow

## 🚀 Performance

- Chunked recording (30s) for memory efficiency
- WorkManager for reliable background processing
- Flow-based reactive updates
- Room as single source of truth
- Compose for efficient UI rendering

## 📝 Implementation Details

### Recording Service
- Foreground service with notification
- MediaRecorder lifecycle management
- Phone state & audio focus listeners
- Storage monitoring
- Chunk overlap for continuity

### Transcription
- WorkManager integration
- Retry mechanism (max 3 attempts)
- Preserves chunk order
- Progress tracking in database

### Summary
- LLM integration (OpenAI/Gemini)
- Streaming response parsing
- Structured output (Title, Summary, Actions, Points)
- Background generation with WorkManager

## 🐛 Known Issues

1. Mock API doesn't provide real transcription
2. Limited emulator audio support
3. Network required for transcription/summary

## 🔮 Future Enhancements

- Export transcript/summary to PDF
- Cloud backup
- Multi-language support
- Speaker identification
- Real-time transcription
- Search functionality

## 📄 License

This project is created as a take-home assignment demonstration.

## 👨‍💻 Development

Built with modern Android development best practices:
- Kotlin Coroutines & Flow
- Jetpack Compose
- Hilt Dependency Injection
- Room Database
- MVVM Architecture
- Material 3 Design

---

For detailed setup instructions and troubleshooting, see [SETUP_INSTRUCTIONS.md](SETUP_INSTRUCTIONS.md)
