# How to Run AOAI App

## Prerequisites
1. **Java Development Kit (JDK)** - Ensure JAVA_HOME is set
2. **Android SDK** - Installed and configured
3. **Android Device or Emulator** - Connected and visible to adb

## Option 1: Run with Gradle (Recommended)

### 1. Build and Install Debug APK
```bash
cd C:\AOAI
.\gradlew installDebug
```
This builds the debug APK and installs it on the connected device/emulator.

### 2. Launch the App
The app will start automatically after installation, or you can launch it:
```bash
adb shell am start -n com.aoai.chat.ai/.MainActivity
```

## Option 2: Build APK Only (without installing)

### Debug APK
```bash
.\gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
```bash
.\gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## Option 3: Run via Android Studio

1. Open the project in Android Studio
2. Select a device or emulator from the device dropdown
3. Click **Run** (green play button) or press `Shift + F10`
4. Android Studio will build, install, and launch the app

## Troubleshooting

### JAVA_HOME Not Set
```bash
# Set JAVA_HOME (replace path with your JDK location)
set JAVA_HOME=C:\Program Files\Java\jdk-17
# Or for PowerShell:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
```

### No Connected Devices
```bash
# List connected devices
adb devices

# Start emulator from Android Studio or:
emulator -avd <emulator_name>
```

### Build Stuck/Cached Issues
```bash
# Clean and rebuild
.\gradlew clean build
```

### Debug App Crashes
```bash
# View live logs
adb logcat | findstr "AOAI"
```

## Expected Features

After successful installation, you should see:
✅ Chat interface with message bubbles
✅ Input field for user messages
✅ Voice input (microphone icon)
✅ Text-to-speech (speaker icon)
✅ Language selection
✅ Network status indicator
✅ Chat history

## Initial Setup

On first launch, the app will:
1. Show a consent dialog for permissions (Camera, Microphone, Network)
2. Request permissions for distributed AI participation
3. Initialize the AOAI01Agent brain system
4. Display the main chat screen

You can then start chatting with the AOAI01 agent!

## Development Commands

```bash
# Run unit tests
.\gradlew test

# Run instrumented tests (on device/emulator)
.\gradlew connectedAndroidTest

# Generate code coverage
.\gradlew testDebugUnitTestCoverage

# View available tasks
.\gradlew tasks

# Build with detailed output
.\gradlew build --info

# Verbose output for debugging
.\gradlew build -x lint --stacktrace
```

## Next Steps

1. Ensure Java 17+ is installed and JAVA_HOME is set
2. Connect an Android device (minSdk=26, Android 8+) or start an emulator
3. Run: `.\gradlew installDebug`
4. Enjoy testing AOAI! 🚀

---

**Note:** The app requires:
- **Minimum SDK:** Android 8 (API 26)
- **Target SDK:** Android 15 (API 35)
- **Permissions:** Camera, Microphone, Network, Location, Notifications

