# ChatRT Android

Native Android frontend for ChatRT - Real-time voice and video communication with OpenAI's Realtime API.

## Project Structure

```
android/
├── app/
│   ├── build.gradle.kts          # App-level build configuration
│   ├── proguard-rules.pro        # ProGuard rules for WebRTC and networking
│   └── src/main/
│       ├── AndroidManifest.xml   # App permissions and components
│       ├── java/com/chatrt/android/
│       │   ├── ChatRtApplication.kt    # Hilt application class
│       │   ├── ui/                     # UI layer (Compose)
│       │   │   ├── MainActivity.kt     # Main activity
│       │   │   └── theme/              # Material Design theme
│       │   ├── data/                   # Data layer
│       │   │   └── model/              # Data models
│       │   ├── domain/                 # Domain layer
│       │   │   └── repository/         # Repository interfaces
│       │   ├── di/                     # Dependency injection (Hilt)
│       │   └── service/                # Background services
│       └── res/                        # Android resources
├── build.gradle.kts              # Project-level build configuration
├── settings.gradle.kts           # Gradle settings
└── gradle.properties             # Gradle properties
```

## Dependencies

- **Kotlin** - Primary language
- **Jetpack Compose** - Modern UI toolkit
- **Hilt** - Dependency injection
- **WebRTC** - Real-time communication
- **Retrofit** - HTTP client
- **Accompanist Permissions** - Permission handling

## Build Requirements

- Android Studio Narwhal | 2025.1.1 or later
- Android Gradle Plugin 8.13
- Kotlin 1.9.22
- Minimum SDK: 34 (Android 14)
- Target SDK: 36 (Android 16)

## Getting Started

1. Open the `android` directory in Android Studio
2. Sync the project with Gradle files
3. Build and run on an Android device or emulator

## Features

- Real-time voice conversations with AI
- Video chat with front/back camera support
- Screen sharing capabilities
- Background call handling
- Permission management
- Settings and preferences
- Real-time logging and debugging

## Architecture

The app follows MVVM architecture with:
- **UI Layer**: Jetpack Compose screens and components
- **ViewModel Layer**: State management and business logic
- **Repository Layer**: Data access abstraction
- **Data Layer**: WebRTC, API services, and local storage

## Next Steps

This is the initial project setup. Subsequent tasks will implement:
1. Core data models and configuration
2. Dependency injection setup
3. Permission management system
4. WebRTC integration
5. UI components and screens
6. And more...