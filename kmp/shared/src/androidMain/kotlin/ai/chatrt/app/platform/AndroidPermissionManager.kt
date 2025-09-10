package ai.chatrt.app.platform

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Android implementation of PermissionManager
 */
class AndroidPermissionManager(
    private val context: Context,
) : PermissionManager {
    private val _permissionChanges = MutableSharedFlow<PermissionChange>()

    override suspend fun checkMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED

    override suspend fun checkCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED

    override suspend fun checkScreenCapturePermission(): Boolean {
        // Screen capture permission is handled differently on Android (MediaProjection)
        // This would typically be checked when requesting MediaProjection
        return true
    }

    override suspend fun checkNotificationPermission(): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required on older versions
        }

    override suspend fun requestMicrophonePermission(): PermissionStatus {
        // This would typically be handled by the Activity/Fragment
        // For now, return current status
        return if (checkMicrophonePermission()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    override suspend fun requestCameraPermission(): PermissionStatus {
        // This would typically be handled by the Activity/Fragment
        return if (checkCameraPermission()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }

    override suspend fun requestScreenCapturePermission(): ScreenCapturePermissionResult {
        // Screen capture permission requires MediaProjectionManager
        // This would return the MediaProjection intent data
        return ScreenCapturePermissionResult(
            status = PermissionStatus.NOT_DETERMINED,
            data = null, // Would contain MediaProjection intent
        )
    }

    override suspend fun requestNotificationPermission(): PermissionStatus =
        if (checkNotificationPermission()) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }

    override suspend fun requestPermissions(permissions: List<Permission>): Map<Permission, PermissionStatus> {
        // Request multiple permissions
        return permissions.associateWith { permission ->
            when (permission) {
                Permission.MICROPHONE -> requestMicrophonePermission()
                Permission.CAMERA -> requestCameraPermission()
                Permission.SCREEN_CAPTURE -> requestScreenCapturePermission().status
                Permission.NOTIFICATIONS -> requestNotificationPermission()
            }
        }
    }

    override suspend fun shouldShowRationale(permission: Permission): Boolean {
        // This would typically be handled by the Activity/Fragment
        return false
    }

    override suspend fun openAppSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        context.startActivity(intent)
    }

    override fun observePermissionChanges(): Flow<PermissionChange> = _permissionChanges.asSharedFlow()
}

/**
 * Factory function for creating Android permission manager
 */
actual fun createPermissionManager(): PermissionManager =
    throw IllegalStateException("Android PermissionManager requires Context. Use AndroidPermissionManager(context) directly.")
