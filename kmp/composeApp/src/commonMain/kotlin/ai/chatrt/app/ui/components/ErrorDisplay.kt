package ai.chatrt.app.ui.components

import ai.chatrt.app.models.ChatRtError
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * ErrorDisplay composable with Material 3 Expressive error styling and retry buttons
 * Shows user-friendly error messages with recovery suggestions
 *
 * Requirements: 4.3
 */
@Composable
fun ErrorDisplay(
    error: ChatRtError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
    )

    val animatedElevation by animateDpAsState(
        targetValue = 6.dp,
        animationSpec =
            tween(
                durationMillis = 300,
                easing = EaseInOutCubic,
            ),
    )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .scale(animatedScale),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Error header with icon and title
            ErrorHeader(
                error = error,
                onDismiss = onDismiss,
            )

            // Error message and description
            ErrorContent(error = error)

            // Action buttons
            ErrorActions(
                error = error,
                onRetry = onRetry,
                onDismiss = onDismiss,
            )
        }
    }
}

/**
 * Error header with icon, title, and dismiss button
 */
@Composable
private fun ErrorHeader(
    error: ChatRtError,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Error icon with pulse animation
            val infiniteTransition = rememberInfiniteTransition()
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1000, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse,
                    ),
            )

            Icon(
                imageVector = getErrorIcon(error),
                contentDescription = "Error",
                modifier =
                    Modifier
                        .size(24.dp)
                        .scale(pulseScale),
                tint = MaterialTheme.colorScheme.error,
            )

            Text(
                text = getErrorTitle(error),
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        // Dismiss button
        IconButton(
            onClick = onDismiss,
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
        ) {
            Icon(
                imageVector = BasicIcons.close,
                contentDescription = "Dismiss",
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Error content with message and helpful suggestions
 */
@Composable
private fun ErrorContent(
    error: ChatRtError,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Main error message
        Text(
            text = getErrorMessage(error),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )

        // Helpful suggestions
        val suggestions = getErrorSuggestions(error)
        if (suggestions.isNotEmpty()) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = BasicIcons.lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        Text(
                            text = "Suggestions:",
                            style =
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    suggestions.forEach { suggestion ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )

                            Text(
                                text = suggestion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Error action buttons with Material 3 Expressive styling
 */
@Composable
private fun ErrorActions(
    error: ChatRtError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Retry button (if applicable)
        if (isRetryable(error)) {
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
            ) {
                Icon(
                    imageVector = BasicIcons.refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Retry",
                    style =
                        MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                )
            }
        }

        // Dismiss button
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            border =
                ButtonDefaults.outlinedButtonBorder.copy(
                    brush =
                        androidx.compose.foundation
                            .BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onErrorContainer,
                            ).brush,
                ),
        ) {
            Text(
                text = "Dismiss",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
    }
}

/**
 * Gets the appropriate icon for each error type
 */
private fun getErrorIcon(error: ChatRtError): ImageVector =
    when (error) {
        is ChatRtError.NetworkError -> BasicIcons.wifiOff
        is ChatRtError.PermissionDenied -> BasicIcons.block
        is ChatRtError.WebRtcError -> BasicIcons.videocamOff
        is ChatRtError.AudioDeviceError -> BasicIcons.micOff
        is ChatRtError.CameraError -> BasicIcons.cameraAlt
        is ChatRtError.ScreenCaptureError -> BasicIcons.screenShareOff
        is ChatRtError.ServiceConnectionError -> BasicIcons.cloudOff
        is ChatRtError.PhoneCallInterruptionError -> BasicIcons.phoneDisabled
        is ChatRtError.BatteryOptimizationError -> BasicIcons.batteryAlert
        is ChatRtError.NetworkQualityError -> BasicIcons.wifi
        is ChatRtError.ApiError -> BasicIcons.error
    }

/**
 * Gets the appropriate title for each error type
 */
private fun getErrorTitle(error: ChatRtError): String =
    when (error) {
        is ChatRtError.NetworkError -> "Connection Error"
        is ChatRtError.PermissionDenied -> "Permission Required"
        is ChatRtError.WebRtcError -> "Call Failed"
        is ChatRtError.AudioDeviceError -> "Audio Error"
        is ChatRtError.CameraError -> "Camera Error"
        is ChatRtError.ScreenCaptureError -> "Screen Share Error"
        is ChatRtError.ServiceConnectionError -> "Service Error"
        is ChatRtError.PhoneCallInterruptionError -> "Call Interrupted"
        is ChatRtError.BatteryOptimizationError -> "Battery Issue"
        is ChatRtError.NetworkQualityError -> "Poor Connection"
        is ChatRtError.ApiError -> "API Error"
    }

/**
 * Gets the appropriate message for each error type
 */
private fun getErrorMessage(error: ChatRtError): String =
    when (error) {
        is ChatRtError.NetworkError -> "Unable to connect to the server. Please check your internet connection."
        is ChatRtError.PermissionDenied -> "Required permissions are not granted. Please enable permissions in settings."
        is ChatRtError.WebRtcError -> "Failed to establish the call connection. Please try again."
        is ChatRtError.AudioDeviceError -> "Unable to access the microphone. Please check your audio settings."
        is ChatRtError.CameraError -> "Unable to access the camera. Please check your camera permissions."
        is ChatRtError.ScreenCaptureError -> "Unable to start screen sharing. Please grant screen recording permission."
        is ChatRtError.ServiceConnectionError -> "Unable to connect to the ChatRT service. Please try again later."
        is ChatRtError.PhoneCallInterruptionError -> "The call was interrupted by an incoming phone call."
        is ChatRtError.BatteryOptimizationError -> "Battery optimization is affecting the app performance."
        is ChatRtError.NetworkQualityError -> "Poor network quality is affecting the call. Consider switching to audio-only mode."
        is ChatRtError.ApiError -> error.message
    }

/**
 * Gets helpful suggestions for each error type
 */
private fun getErrorSuggestions(error: ChatRtError): List<String> =
    when (error) {
        is ChatRtError.NetworkError ->
            listOf(
                "Check your WiFi or mobile data connection",
                "Try switching between WiFi and mobile data",
                "Move closer to your router if using WiFi",
            )
        is ChatRtError.PermissionDenied ->
            listOf(
                "Go to Settings > Apps > ChatRT > Permissions",
                "Enable Camera and Microphone permissions",
                "Restart the app after granting permissions",
            )
        is ChatRtError.WebRtcError ->
            listOf(
                "Check your internet connection",
                "Try switching to audio-only mode",
                "Restart the app if the problem persists",
            )
        is ChatRtError.AudioDeviceError ->
            listOf(
                "Check if another app is using the microphone",
                "Try unplugging and reconnecting headphones",
                "Restart the app to reset audio settings",
            )
        is ChatRtError.CameraError ->
            listOf(
                "Check if another app is using the camera",
                "Try switching to audio-only mode",
                "Restart the app to reset camera settings",
            )
        is ChatRtError.ScreenCaptureError ->
            listOf(
                "Grant screen recording permission when prompted",
                "Try switching to camera mode instead",
                "Check if screen recording is restricted by your device",
            )
        is ChatRtError.NetworkQualityError ->
            listOf(
                "Switch to audio-only mode for better performance",
                "Move closer to your WiFi router",
                "Close other apps using internet",
            )
        else -> emptyList()
    }

/**
 * Determines if an error type supports retry functionality
 */
private fun isRetryable(error: ChatRtError): Boolean =
    when (error) {
        is ChatRtError.NetworkError,
        is ChatRtError.WebRtcError,
        is ChatRtError.ServiceConnectionError,
        is ChatRtError.ApiError,
        -> true
        else -> false
    }
