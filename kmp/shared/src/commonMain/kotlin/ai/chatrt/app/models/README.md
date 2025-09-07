# ChatRT Data Models

This package contains all the shared data models used across the ChatRT application.

## Core Models

### ConnectionState.kt
- `ConnectionState` enum: Represents WebRTC connection states
  - DISCONNECTED, CONNECTING, CONNECTED, FAILED, RECONNECTING

### VideoMode.kt
- `VideoMode` enum: Available video modes in the application
  - AUDIO_ONLY, WEBCAM, SCREEN_SHARE

### LogEntry.kt
- `LogEntry` data class: Represents log entries with timestamp and message
- `LogLevel` enum: Log levels (DEBUG, INFO, WARNING, ERROR)

### AppSettings.kt
- `AppSettings` data class: Application settings and preferences
- `AudioQuality` enum: Audio quality settings (LOW, MEDIUM, HIGH)
- `CameraFacing` enum: Camera direction (FRONT, BACK)

## API Models

### SessionConfig.kt
- `SessionConfig` data class: Configuration for ChatRT session
- `AudioConfig` data class: Audio configuration
- `AudioInputConfig` data class: Audio input settings
- `NoiseReductionConfig` data class: Noise reduction settings
- `AudioOutputConfig` data class: Audio output settings

### ApiModels.kt
- `CallRequest` data class: Request model for creating calls
- `CallResponse` data class: Response model for call creation
- `ConnectionParams` data class: Connection parameters

## Error Handling

### ChatRtError.kt
- `ChatRtError` sealed class: Comprehensive error hierarchy
  - NetworkError, PermissionDenied, WebRtcError
  - AudioDeviceError, CameraError, ScreenCaptureError
  - ServiceConnectionError, PhoneCallInterruptionError
  - BatteryOptimizationError, NetworkQualityError
  - ApiError (with code and message)

## Platform-Specific Models

### PlatformModels.kt
- `PlatformOptimization` data class: Platform-agnostic optimization recommendations
- `OptimizationReason` enum: Reasons for optimization
- `SystemInterruption` data class: System interruption information
- `InterruptionType` enum: Types of system interruptions
- `ResourceConstraints` data class: Resource constraint information
- `NetworkQuality` enum: Network quality levels
- `PowerSavingMode` enum: Power saving modes
- `PowerSavingRecommendation` data class: Power saving recommendations

## Serialization

All data models use `kotlinx.serialization` for JSON serialization/deserialization.
Models marked with `@Serializable` can be safely serialized for:
- API communication
- Local storage
- Inter-process communication

## Testing

Comprehensive unit tests are available in the `commonTest` directory:
- `DataModelTests.kt`: Tests for core data models and serialization
- `PlatformModelTests.kt`: Tests for platform-specific models
- `ChatRtErrorTests.kt`: Tests for error handling models

All tests verify:
- Serialization/deserialization correctness
- Default value behavior
- Enum value completeness
- Data class property validation