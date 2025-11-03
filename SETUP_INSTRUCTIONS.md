# Voice Recording App - Setup Instructions

## Prerequisites

Before opening the project in Android Studio, ensure you have:

1. **Android Studio** (Arctic Fox or later recommended)
   - Download from: https://developer.android.com/studio

2. **JDK 17** or higher
   - Android Studio typically includes this

3. **Android SDK**
   - Minimum SDK: API 24 (Android 7.0)
   - Target SDK: API 34 (Android 14)
   - Compile SDK: API 34

## Step-by-Step Setup Instructions

### 1. Open the Project in Android Studio

1. Launch **Android Studio**
2. Select **File → Open**
3. Navigate to the `twinmind` directory (the root directory of this project)
4. Click **OK** to open the project

### 2. Wait for Gradle Sync

Android Studio will automatically:
- Download required dependencies
- Sync Gradle files
- Index the project

**This may take several minutes on first run.**

If Gradle sync fails:
- Check your internet connection
- Click **File → Sync Project with Gradle Files**
- If issues persist, click **File → Invalidate Caches / Restart**

### 3. Configure Android SDK

1. Go to **File → Project Structure → SDK Location**
2. Ensure Android SDK location is set correctly
3. Go to **Tools → SDK Manager**
4. Under **SDK Platforms**, ensure Android 14.0 (API 34) is installed
5. Under **SDK Tools**, ensure these are installed:
   - Android SDK Build-Tools
   - Android SDK Platform-Tools
   - Android Emulator
   - Google Play Services

### 4. Set Up an Emulator or Physical Device

#### Option A: Android Emulator
1. Go to **Tools → Device Manager**
2. Click **Create Device**
3. Select a phone (e.g., Pixel 6)
4. Select a system image (API 34 recommended)
5. Click **Finish**

#### Option B: Physical Device
1. Enable **Developer Options** on your Android device:
   - Go to **Settings → About Phone**
   - Tap **Build Number** 7 times
2. Enable **USB Debugging**:
   - Go to **Settings → Developer Options**
   - Enable **USB Debugging**
3. Connect your device via USB
4. Accept the debugging authorization prompt on your device

### 5. Build the Project

1. Click **Build → Make Project** (or press Ctrl+F9 / Cmd+F9)
2. Wait for the build to complete
3. Check the **Build** tab at the bottom for any errors

### 6. Run the Application

1. Select your target device from the device dropdown (top toolbar)
2. Click the **Run** button (green play icon) or press Shift+F10
3. The app will install and launch on your device/emulator

### 7. Grant Permissions

When the app first launches:
1. Navigate to the **Recording** screen
2. Grant **Microphone** permission when prompted
3. Grant **Notification** permission (Android 13+)
4. Optionally grant **Phone State** permission for call handling

## Building APK

### Debug APK
1. Go to **Build → Build Bundle(s) / APK(s) → Build APK(s)**
2. Wait for the build to complete
3. Click **locate** in the notification to find the APK
4. APK location: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
1. Go to **Build → Generate Signed Bundle / APK**
2. Select **APK**
3. Create a new keystore or use an existing one
4. Fill in the keystore details
5. Select **release** build variant
6. Click **Finish**
7. APK location: `app/build/outputs/apk/release/app-release.apk`

## Project Structure

```
app/
├── build.gradle.kts                  # App-level Gradle configuration
├── src/
│   └── main/
│       ├── AndroidManifest.xml       # App manifest with permissions
│       ├── java/com/twinmind/voicerecorder/
│       │   ├── MainActivity.kt       # Main entry point
│       │   ├── VoiceRecorderApplication.kt  # Application class
│       │   ├── data/
│       │   │   ├── local/            # Room database, DAOs, entities
│       │   │   ├── remote/           # API services
│       │   │   └── repository/       # Repository layer
│       │   ├── di/                   # Hilt dependency injection
│       │   ├── service/              # Recording service, workers
│       │   ├── ui/                   # Compose UI screens, ViewModels
│       │   └── utils/                # Utility classes
│       └── res/                      # Resources (layouts, strings, etc.)
```

## Configuration

### API Keys (Optional)

The app currently uses **mock API services** for transcription and summary generation. To use real APIs:

#### OpenAI API
1. Get API key from: https://platform.openai.com/api-keys
2. Add to `local.properties`:
   ```
   OPENAI_API_KEY=your_api_key_here
   ```
3. Update `NetworkModule.kt` to inject the API key

#### Google Gemini API
1. Get API key from: https://makersuite.google.com/app/apikey
2. Add to `local.properties`:
   ```
   GEMINI_API_KEY=your_api_key_here
   ```
3. Update `NetworkModule.kt` to inject the API key

### Switching from Mock to Real APIs

1. Open `TranscriptionRepository.kt`
2. Replace `mockApiService.transcribeAudio()` with actual API calls
3. Open `SummaryRepository.kt`
4. Replace `mockApiService.generateSummary()` with actual API calls

## Testing the App

### Basic Flow
1. **Dashboard Screen**: Launch app → See empty state
2. **Start Recording**: Tap + button → Grant permissions → Tap Record
3. **Recording**: Speak into microphone → See timer counting
4. **Stop Recording**: Tap Stop button
5. **Transcription**: Automatic (background processing)
6. **View Summary**: Tap on meeting from dashboard → See summary

### Testing Edge Cases

#### Phone Call Interruption
1. Start recording
2. Make/receive a phone call
3. Recording should pause automatically
4. End call → Recording resumes

#### Audio Focus Loss
1. Start recording
2. Play music from another app
3. Recording should pause
4. Stop music → Recording resumes (or tap Resume in notification)

#### Low Storage
1. Fill device storage to near capacity
2. Try to start recording
3. Should show "Low storage" error

#### Process Death
1. Start recording
2. Force stop the app from Settings
3. Restart app → Recording should be saved

## Troubleshooting

### Gradle Sync Issues
- **Error**: "Plugin with id 'com.android.application' not found"
  - **Solution**: Update Gradle plugin in `build.gradle.kts` (root)

- **Error**: "Minimum supported Gradle version is X.X.X"
  - **Solution**: Update `gradle-wrapper.properties`

### Build Errors
- **Error**: "Unresolved reference: R"
  - **Solution**: Clean project → Rebuild project

- **Error**: "Duplicate class found"
  - **Solution**: Check dependencies for duplicates, add exclusions

### Runtime Errors
- **Crash on launch**: Check Logcat for stack trace
- **Permission denied**: Ensure all permissions are granted
- **Service not starting**: Check Android version and foreground service requirements

### Emulator Issues
- **App not installing**: Wipe emulator data → Restart emulator
- **Slow performance**: Allocate more RAM to emulator
- **Audio recording not working**: Use physical device (emulator has limited audio support)

## Additional Notes

### Minimum Requirements
- **Android OS**: 7.0 (API 24) or higher
- **Storage**: 100MB free space for recordings
- **Hardware**: Microphone required

### Permissions
- **RECORD_AUDIO**: Required for recording
- **FOREGROUND_SERVICE**: Required for background recording
- **POST_NOTIFICATIONS**: Required for Android 13+ notifications
- **READ_PHONE_STATE**: Optional, for call handling

### Known Limitations
1. Mock API is used by default (no real transcription/summary)
2. Audio quality depends on device microphone
3. Large recordings may impact performance
4. Background processing requires battery optimization exemption

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review Android Studio Logcat for errors
3. Ensure all dependencies are properly installed
4. Verify Android SDK configuration

## Next Steps

After successfully running the app:
1. Test all core features
2. Integrate real API keys for transcription/summary
3. Test on multiple devices and Android versions
4. Optimize performance and battery usage
5. Add unit and integration tests
