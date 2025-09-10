package ai.chatrt.app.ui.components

import ai.chatrt.app.models.ConnectionState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * ConnectionStatusIndicator composable with Material 3 Expressive design and connection state animations
 * Displays real-time connection status with visual feedback
 * 
 * Requirements: 4.1, 4.2, 4.3
 */
@Composable
fun ConnectionStatusIndicator(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    val statusColor = when (connectionState) {
        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
        ConnectionState.CONNECTING -> MaterialTheme.colorScheme.tertiary
        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.secondary
        ConnectionState.FAILED -> MaterialTheme.colorScheme.error
        ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.tertiary
    }
    
    val statusText = when (connectionState) {
        ConnectionState.DISCONNECTED -> "Disconnected"
        ConnectionState.CONNECTING -> "Connecting..."
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.FAILED -> "Connection Failed"
        ConnectionState.RECONNECTING -> "Reconnecting..."
    }
    
    // Animated pulse for connecting/reconnecting states
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    val shouldPulse = connectionState == ConnectionState.CONNECTING || 
                     connectionState == ConnectionState.RECONNECTING
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status indicator circle with animation
            Canvas(
                modifier = Modifier.size(12.dp)
            ) {
                val alpha = if (shouldPulse) pulseAlpha else 1f
                drawCircle(
                    color = statusColor.copy(alpha = alpha),
                    radius = size.minDimension / 2
                )
            }
            
            // Status text
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Connection progress indicator for connecting states
            if (shouldPulse) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = statusColor,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}