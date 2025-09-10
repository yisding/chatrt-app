package ai.chatrt.app.ui.components

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.NetworkQuality
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * ConnectionStatusIndicator composable with Material 3 Expressive design and connection state animations
 * Displays real-time connection status with visual feedback, network quality, and call pause status
 *
 * Requirements: 4.1, 4.2, 4.3, 5.2
 */
@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    networkQuality: NetworkQuality = NetworkQuality.GOOD,
    isCallPaused: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val statusColor =
        when {
            isCallPaused -> MaterialTheme.colorScheme.tertiary
            connectionState == ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
            connectionState == ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
            connectionState == ConnectionState.CONNECTED -> MaterialTheme.colorScheme.secondary
            connectionState == ConnectionState.FAILED -> MaterialTheme.colorScheme.error
            connectionState == ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.outline
        }

    val statusText =
        when {
            isCallPaused -> "Call Paused"
            connectionState == ConnectionState.DISCONNECTED -> "Disconnected"
            connectionState == ConnectionState.CONNECTING -> "Connecting..."
            connectionState == ConnectionState.CONNECTED -> "Connected"
            connectionState == ConnectionState.FAILED -> "Connection Failed"
            connectionState == ConnectionState.RECONNECTING -> "Reconnecting..."
            else -> "Unknown"
        }

    // Animated pulse for connecting/reconnecting states
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
    )

    val shouldPulse =
        connectionState == ConnectionState.CONNECTING ||
            connectionState == ConnectionState.RECONNECTING

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Status indicator circle with animation
            Canvas(
                modifier = Modifier.size(12.dp),
            ) {
                val alpha = if (shouldPulse) pulseAlpha else 1f
                drawCircle(
                    color = statusColor.copy(alpha = alpha),
                    radius = size.minDimension / 2,
                )
            }

            // Status text
            Text(
                text = statusText,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.weight(1f))

            // Network quality indicator (only when connected)
            if (connectionState == ConnectionState.CONNECTED && !isCallPaused) {
                NetworkQualityIndicator(
                    quality = networkQuality,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            // Call pause indicator
            if (isCallPaused) {
                Icon(
                    imageVector = BasicIcons.pause,
                    contentDescription = "Call Paused",
                    modifier = Modifier.size(16.dp),
                    tint = statusColor,
                )
            }
            // Connection progress indicator for connecting states
            else if (shouldPulse) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = statusColor,
                    strokeWidth = 2.dp,
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
