# ChatRT Repository Layer

This package contains the repository interfaces and implementations for the ChatRT application, providing a clean abstraction layer between the UI and data sources.

## Architecture

The repository layer follows the Repository pattern to provide:
- Clean separation between business logic and data access
- Testable interfaces with mock implementations
- Consistent error handling across the application
- Reactive data streams using Kotlin Flow

## Components

### ChatRepository

**Interface**: `ChatRepository`
**Implementation**: `ChatRepositoryImpl`

Manages WebRTC connections and real-time communication with the ChatRT backend.

**Key Features**:
- Call creation and management
- Connection state monitoring
- Real-time logging
- Error handling with retry logic

**Usage**:
```kotlin
val repository = ChatRepositoryImpl(apiService)

// Create a call
val callRequest = CallRequest(sdp = "...", session = sessionConfig)
val result = repository.createCall(callRequest)

// Observe connection state
repository.observeConnectionState().collect { state ->
    // Handle connection state changes
}

// Observe logs
repository.observeLogs().collect { logs ->
    // Display real-time logs
}
```

### SettingsRepository

**Interface**: `SettingsRepository`
**Implementation**: `SettingsRepositoryImpl`

Manages application settings and user preferences with reactive updates.

**Key Features**:
- Persistent settings storage (platform-specific implementations)
- Reactive settings updates via Flow
- Individual setting accessors
- Default value management

**Usage**:
```kotlin
val repository = SettingsRepositoryImpl()

// Update settings
val newSettings = AppSettings(defaultVideoMode = VideoMode.WEBCAM)
repository.updateSettings(newSettings)

// Observe settings changes
repository.observeSettings().collect { settings ->
    // React to settings changes
}

// Individual setting updates
repository.setDefaultVideoMode(VideoMode.SCREEN_SHARE)
repository.setAudioQuality(AudioQuality.HIGH)
```

## Network Layer

### ChatRtApiService

Handles HTTP communication with the ChatRT backend using Ktor client.

**Key Features**:
- Automatic retry logic with exponential backoff
- Comprehensive error handling
- Request/response serialization
- Connection timeout management

**Usage**:
```kotlin
val apiService = ChatRtApiService("https://api.chatrt.com")

// Create a call
val result = apiService.createCall(callRequest)

// Start monitoring
val monitorResult = apiService.startCallMonitoring(callId)

// Health check
val healthResult = apiService.checkHealth()
```

## Error Handling

All repository methods return `Result<T>` types for consistent error handling:

```kotlin
when (val result = repository.createCall(request)) {
    is Result.Success -> {
        // Handle success
        val response = result.getOrNull()
    }
    is Result.Failure -> {
        // Handle error
        val error = result.exceptionOrNull()
        when (error) {
            is ChatRtError.NetworkError -> // Handle network issues
            is ChatRtError.ApiError -> // Handle API errors
            else -> // Handle other errors
        }
    }
}
```

## Testing

The repository layer includes comprehensive tests:

- **Unit Tests**: Test individual repository methods and error scenarios
- **Integration Tests**: Test complete workflows and component interactions
- **Mock Support**: Interfaces allow easy mocking for UI layer tests

Run tests with:
```bash
./gradlew :shared:test
```

## Platform-Specific Implementations

The base implementations use in-memory storage. Platform-specific implementations should extend these classes to provide persistent storage:

- **Android**: Use SharedPreferences or DataStore
- **iOS**: Use UserDefaults or Core Data

Example platform-specific implementation:
```kotlin
class AndroidSettingsRepository(
    private val sharedPreferences: SharedPreferences
) : SettingsRepositoryImpl() {
    
    override suspend fun updateSettings(settings: AppSettings): Result<Unit> {
        return try {
            // Save to SharedPreferences
            sharedPreferences.edit()
                .putString("settings", Json.encodeToString(settings))
                .apply()
            super.updateSettings(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```
