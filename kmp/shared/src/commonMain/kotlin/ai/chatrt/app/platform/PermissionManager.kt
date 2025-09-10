package ai.chatrt.app.platform

import kotlinx.coroutines.flow.Flow

/**
 * Permission manager interface for handling platform-specific permissions
 */
interface PermissionManager {
    /**
     * Check if microphone permission is granted
     */
    suspend fun checkMicrophonePermission(): Boolean

    /**
     * Check if camera permission is granted
     */
    suspend fun checkCameraPermission(): Boolean

    /**
     * Check if screen capture permission is granted
     */
    suspend fun checkScreenCapturePermission(): Boolean

    /**
     * Check if notification permission is granted
     */
    suspend fun checkNotificationPermission(): Boolean

    /**
     * Request microphone permission
     */
    suspend fun requestMicrophonePermission(): PermissionStatus

    /**
     * Request camera permission
     */
    suspend fun requestCameraPermission(): PermissionStatus

    /**
     * Request screen capture permission
     */
    suspend fun requestScreenCapturePermission(): ScreenCapturePermissionResult

    /**
     * Request notification permission
     */
    suspend fun requestNotificationPermission(): PermissionStatus

    /**
     * Request multiple permissions at once
     */
    suspend fun requestPermissions(permissions: List<Permission>): Map<Permission, PermissionStatus>

    /**
     * Check if should show permission rationale
     */
    suspend fun shouldShowRationale(permission: Permission): Boolean

    /**
     * Open app settings for manual permission grant
     */
    suspend fun openAppSettings()

    /**
     * Observe permission changes
     */
    fun observePermissionChanges(): Flow<PermissionChange>
}

/**
 * Permission status result
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    NOT_DETERMINED,
}

/**
 * Screen capture permission result (may include additional data on some platforms)
 */
data class ScreenCapturePermissionResult(
    val status: PermissionStatus,
    val data: Any? = null, // Platform-specific data (e.g., MediaProjection intent on Android)
)

/**
 * Permission change event
 */
data class PermissionChange(
    val permission: Permission,
    val status: PermissionStatus,
)

/**
 * Expected permission manager factory function
 */
expect fun createPermissionManager(): PermissionManager
