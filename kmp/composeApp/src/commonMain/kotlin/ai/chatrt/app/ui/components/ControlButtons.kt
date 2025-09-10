package ai.chatrt.app.ui.components

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
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
 * ControlButtons composable with Material 3 Expressive FABs, buttons, and interactive elements
 * Provides main connection controls and secondary actions
 *
 * Requirements: 1.2, 1.6, 2.4
 */
@Composable
fun ControlButtons(
    connectionState: ConnectionState,
    videoMode: VideoMode,
    onStartConnection: () -> Unit,
    onStopConnection: () -> Unit,
    onCameraSwitch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Main action button with enhanced Material 3 Expressive styling
        MainActionButton(
            connectionState = connectionState,
            videoMode = videoMode,
            onStartConnection = onStartConnection,
            onStopConnection = onStopConnection,
        )

        // Secondary action buttons
        SecondaryActionButtons(
            connectionState = connectionState,
            videoMode = videoMode,
            onCameraSwitch = onCameraSwitch,
            onOpenSettings = onOpenSettings,
        )
    }
}

/**
 * Main action button (Start/Stop connection) with Material 3 Expressive animations
 */
@Composable
private fun MainActionButton(
    connectionState: ConnectionState,
    videoMode: VideoMode,
    onStartConnection: () -> Unit,
    onStopConnection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = connectionState == ConnectionState.CONNECTED
    val isConnecting =
        connectionState == ConnectionState.CONNECTING ||
            connectionState == ConnectionState.RECONNECTING

    // Button animations
    val animatedScale by animateFloatAsState(
        targetValue =
            when {
                isConnecting -> 0.95f
                isConnected -> 1.05f
                else -> 1f
            },
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
    )

    val animatedElevation by animateDpAsState(
        targetValue =
            when {
                isConnected -> 12.dp
                isConnecting -> 4.dp
                else -> 8.dp
            },
        animationSpec =
            tween(
                durationMillis = 300,
                easing = EaseInOutCubic,
            ),
    )

    // Button colors based on state
    val buttonColors =
        when {
            isConnected ->
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            isConnecting ->
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            else ->
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
        }

    Button(
        onClick = {
            if (isConnected) {
                onStopConnection()
            } else if (!isConnecting) {
                onStartConnection()
            }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(animatedScale),
        enabled = !isConnecting,
        colors = buttonColors,
        elevation =
            ButtonDefaults.buttonElevation(
                defaultElevation = animatedElevation,
                pressedElevation = animatedElevation + 4.dp,
                disabledElevation = 0.dp,
            ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Button icon with animation
            val iconRotation by animateFloatAsState(
                targetValue = if (isConnecting) 360f else 0f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
            )

            if (isConnecting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onTertiary,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(
                    imageVector = getMainActionIcon(connectionState, videoMode),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Button text
            Text(
                text = getMainActionText(connectionState, videoMode),
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }
    }
}

/**
 * Secondary action buttons with Material 3 Expressive styling
 */
@Composable
private fun SecondaryActionButtons(
    connectionState: ConnectionState,
    videoMode: VideoMode,
    onCameraSwitch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        // Camera switch button (only visible for webcam mode)
        AnimatedVisibility(
            visible = videoMode == VideoMode.WEBCAM,
            enter =
                scaleIn(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ) + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            SecondaryActionButton(
                icon = BasicIcons.cameraSwitch,
                contentDescription = "Switch Camera",
                onClick = onCameraSwitch,
                enabled = connectionState != ConnectionState.CONNECTING,
            )
        }

        // Settings button
        SecondaryActionButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
            enabled = true,
        )

        // Additional action button (placeholder for future features)
        SecondaryActionButton(
            icon = Icons.Default.MoreVert,
            contentDescription = "More Options",
            onClick = { /* TODO: Implement more options */ },
            enabled = connectionState != ConnectionState.CONNECTING,
        )
    }
}

/**
 * Individual secondary action button with Material 3 Expressive styling
 */
@Composable
private fun SecondaryActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.8f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
    )

    OutlinedButton(
        onClick = onClick,
        modifier =
            modifier
                .size(48.dp)
                .scale(animatedScale),
        enabled = enabled,
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            ),
        border =
            ButtonDefaults.outlinedButtonBorder.copy(
                width = 1.dp,
            ),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Gets the appropriate icon for the main action button
 */
private fun getMainActionIcon(
    connectionState: ConnectionState,
    videoMode: VideoMode,
): ImageVector =
    when {
        connectionState == ConnectionState.CONNECTED -> BasicIcons.callEnd
        connectionState == ConnectionState.CONNECTING ||
            connectionState == ConnectionState.RECONNECTING -> BasicIcons.refresh
        else ->
            when (videoMode) {
                VideoMode.AUDIO_ONLY -> BasicIcons.call
                VideoMode.WEBCAM -> BasicIcons.videoCall
                VideoMode.SCREEN_SHARE -> BasicIcons.presentToAll
            }
    }

/**
 * Gets the appropriate text for the main action button
 */
private fun getMainActionText(
    connectionState: ConnectionState,
    videoMode: VideoMode,
): String =
    when {
        connectionState == ConnectionState.CONNECTED -> "End Chat"
        connectionState == ConnectionState.CONNECTING -> "Connecting..."
        connectionState == ConnectionState.RECONNECTING -> "Reconnecting..."
        connectionState == ConnectionState.FAILED -> "Retry Connection"
        else ->
            when (videoMode) {
                VideoMode.AUDIO_ONLY -> "Start Voice Chat"
                VideoMode.WEBCAM -> "Start Video Chat"
                VideoMode.SCREEN_SHARE -> "Start Screen Share"
            }
    }
