@file:Suppress("FunctionName", "ktlint:standard:if-else-wrapping")

package ai.chatrt.app.ui.components

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.NetworkQuality
import ai.chatrt.app.ui.theme.chatRtMotion
import ai.chatrt.app.ui.theme.chatRtShapes
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * ConnectionStatusIndicator composable with Material 3 Expressive design and enhanced animations
 * Displays real-time connection status with visual feedback, network quality, and call pause status
 * Enhanced with Material 3 Expressive motion system and micro-interactions
 *
 * Requirements: 4.1, 4.2, 4.3, 5.2, 6.2
 */
@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    networkQuality: NetworkQuality = NetworkQuality.GOOD,
    isCallPaused: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val motionScheme = MaterialTheme.chatRtMotion
    val expressiveShapes = MaterialTheme.chatRtShapes

    val (statusColor, statusText, description) =
        when {
            isCallPaused ->
                Triple(
                    MaterialTheme.colorScheme.tertiary,
                    "Call Paused",
                    "Call paused due to phone call interruption",
                )
            connectionState == ConnectionState.DISCONNECTED ->
                Triple(
                    MaterialTheme.colorScheme.outline,
                    "Disconnected",
                    "Disconnected, ready to connect",
                )
            connectionState == ConnectionState.CONNECTING ->
                Triple(
                    MaterialTheme.colorScheme.tertiary,
                    "Connecting...",
                    "Connecting, establishing connection",
                )
            connectionState == ConnectionState.CONNECTED ->
                Triple(
                    MaterialTheme.colorScheme.secondary,
                    "Connected",
                    "Connected, connection established",
                )
            connectionState == ConnectionState.FAILED ->
                Triple(
                    MaterialTheme.colorScheme.error,
                    "Connection Failed",
                    "Connection failed, tap to retry",
                )
            connectionState == ConnectionState.RECONNECTING ->
                Triple(
                    MaterialTheme.colorScheme.tertiary,
                    "Reconnecting...",
                    "Reconnecting, attempting to restore connection",
                )
            else ->
                Triple(
                    MaterialTheme.colorScheme.outline,
                    "Unknown",
                    "Unknown connection state",
                )
        }

    // Enhanced pulse animation using Material 3 Expressive motion
    val infiniteTransition = rememberInfiniteTransition(label = "connection_animation")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse_alpha",
    )

    // Scale animation for connection state changes
    val scale by animateFloatAsState(
        targetValue =
            when (connectionState) {
                ConnectionState.CONNECTED -> 1.05f
                ConnectionState.FAILED -> 0.98f
                else -> 1f
            },
        animationSpec = motionScheme.buttonPress,
        label = "connection_scale",
    )

    // Ripple effect for successful connection
    val rippleScale by animateFloatAsState(
        targetValue = if (connectionState == ConnectionState.CONNECTED) 1.3f else 1f,
        animationSpec = motionScheme.rippleExpansion,
        label = "connection_ripple",
    )

    val shouldPulse =
        connectionState == ConnectionState.CONNECTING ||
            connectionState == ConnectionState.RECONNECTING

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .scale(scale)
                .testTag("connection_status")
                .semantics {
                    contentDescription = description
                },
        colors =
            CardDefaults.cardColors(
                containerColor =
                    when (connectionState) {
                        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        ConnectionState.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation =
                    when (connectionState) {
                        ConnectionState.CONNECTED -> 4.dp
                        ConnectionState.FAILED -> 1.dp
                        else -> 2.dp
                    },
            ),
        shape = expressiveShapes.logCard,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Enhanced status indicator with ripple effect
            Box(
                modifier = Modifier.size(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Background ripple for connected state
                if (connectionState == ConnectionState.CONNECTED) {
                    Canvas(
                        modifier =
                            Modifier
                                .size(16.dp)
                                .scale(rippleScale),
                    ) {
                        // Fade out as the ripple expands
                        drawCircle(
                            color = statusColor,
                            alpha = 0.2f * (2f - rippleScale),
                            radius = size.minDimension / 2,
                        )
                    }
                }

                // Main status indicator circle with enhanced animation
                Canvas(
                    modifier = Modifier.size(12.dp),
                ) {
                    val alpha = if (shouldPulse) pulseAlpha else 1f
                    drawCircle(
                        color = statusColor.copy(alpha = alpha),
                        radius = size.minDimension / 2,
                    )

                    // Inner highlight for connected state
                    if (connectionState == ConnectionState.CONNECTED) {
                        drawCircle(
                            color = Color.White,
                            alpha = 0.4f,
                            radius = size.minDimension / 4,
                        )
                    }
                }
            }

            // Status text with enhanced typography
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight =
                    when (connectionState) {
                        ConnectionState.CONNECTED -> FontWeight.SemiBold
                        ConnectionState.FAILED -> FontWeight.Medium
                        else -> FontWeight.Normal
                    },
                color =
                    when (connectionState) {
                        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.secondary
                        ConnectionState.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )

            Spacer(modifier = Modifier.weight(1f))

            // Network quality indicator with enhanced animation (only when connected)
            if (connectionState == ConnectionState.CONNECTED && !isCallPaused) {
                NetworkQualityIndicator(
                    quality = networkQuality,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            // Call pause indicator with enhanced styling
            if (isCallPaused) {
                Icon(
                    imageVector = BasicIcons.pause,
                    contentDescription = "Call Paused",
                    modifier = Modifier.size(18.dp),
                    tint = statusColor,
                )
            } else if (shouldPulse) {
                // Enhanced connection progress indicator for connecting states
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = statusColor,
                    strokeWidth = 2.5.dp,
                )
            } else if (connectionState == ConnectionState.CONNECTED) {
                // Success indicator for connected state
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Check,
                    contentDescription = "Connected",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            } else if (connectionState == ConnectionState.FAILED) {
                // Error indicator for failed state
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Error,
                    contentDescription = "Connection failed",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/**
 * Network quality indicator with signal strength visualization
 */
@Composable
private fun NetworkQualityIndicator(
    quality: NetworkQuality,
    modifier: Modifier = Modifier,
) {
    val qualityIcon =
        when (quality) {
            NetworkQuality.EXCELLENT -> BasicIcons.signal4
            NetworkQuality.GOOD -> BasicIcons.signal3
            NetworkQuality.FAIR -> BasicIcons.signal2
            NetworkQuality.POOR -> BasicIcons.signal1
        }

    val qualityColor =
        when (quality) {
            NetworkQuality.EXCELLENT -> MaterialTheme.colorScheme.secondary
            NetworkQuality.GOOD -> MaterialTheme.colorScheme.secondary
            NetworkQuality.FAIR -> MaterialTheme.colorScheme.tertiary
            NetworkQuality.POOR -> MaterialTheme.colorScheme.error
        }

    Icon(
        imageVector = qualityIcon,
        contentDescription = "Network Quality: ${quality.name}",
        modifier = modifier.size(16.dp),
        tint = qualityColor,
    )
}
