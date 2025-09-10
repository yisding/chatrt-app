package ai.chatrt.app.ui.components

import ai.chatrt.app.models.ConnectionState
import ai.chatrt.app.models.VideoMode
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * VideoPreview composable for camera and screen capture display with Material 3 Expressive containers
 * Handles video stream rendering with proper aspect ratio management for different orientations
 *
 * Requirements: 2.2, 2.4, 2.5, 3.2
 */
@Composable
fun VideoPreview(
    videoMode: VideoMode,
    isPreviewActive: Boolean,
    onCameraSwitch: () -> Unit,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isPreviewActive) 16.dp else 12.dp,
        animationSpec =
            tween(
                durationMillis = 300,
                easing = EaseInOutCubic,
            ),
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (isPreviewActive) 8.dp else 4.dp,
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
                .aspectRatio(16f / 9f),
        // Proper aspect ratio management
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
        shape =
            androidx.compose.foundation.shape
                .RoundedCornerShape(animatedCornerRadius),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            // Preview content based on video mode
            when (videoMode) {
                VideoMode.WEBCAM -> {
                    WebcamPreview(
                        isActive = isPreviewActive,
                        connectionState = connectionState,
                    )
                }
                VideoMode.SCREEN_SHARE -> {
                    ScreenSharePreview(
                        isActive = isPreviewActive,
                        connectionState = connectionState,
                    )
                }
                VideoMode.AUDIO_ONLY -> {
                    // This shouldn't be shown for audio-only mode
                    // but included for completeness
                    AudioOnlyIndicator()
                }
            }

            // Camera switch button (only for webcam mode)
            if (videoMode == VideoMode.WEBCAM) {
                CameraSwitchButton(
                    onClick = onCameraSwitch,
                    enabled = connectionState != ConnectionState.CONNECTING,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                )
            }

            // Connection status overlay
            if (connectionState == ConnectionState.CONNECTING) {
                ConnectingOverlay()
            }
        }
    }
}

/**
 * Webcam preview with Material 3 Expressive styling
 */
@Composable
private fun WebcamPreview(
    isActive: Boolean,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isActive && connectionState == ConnectionState.CONNECTED) {
            // In a real implementation, this would show the actual camera feed
            // For now, show a placeholder with animated gradient
            val infiniteTransition = rememberInfiniteTransition()
            val gradientOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                        ),
                                    start =
                                        androidx.compose.ui.geometry.Offset(
                                            gradientOffset * 1000f,
                                            gradientOffset * 1000f,
                                        ),
                                    end =
                                        androidx.compose.ui.geometry.Offset(
                                            (1f - gradientOffset) * 1000f,
                                            (1f - gradientOffset) * 1000f,
                                        ),
                                ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = BasicIcons.videocam,
                        contentDescription = "Camera Active",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Camera Active",
                        style =
                            MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            // Camera preview placeholder
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = BasicIcons.videocam,
                    contentDescription = "Camera Preview",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = "Camera Preview",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "Front-facing camera ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/**
 * Screen share preview with Material 3 Expressive styling
 */
@Composable
private fun ScreenSharePreview(
    isActive: Boolean,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (isActive && connectionState == ConnectionState.CONNECTED) {
            // Screen sharing active indicator
            val infiniteTransition = rememberInfiniteTransition()
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1500, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse,
                    ),
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = BasicIcons.screenShare,
                    contentDescription = "Screen Sharing Active",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = pulseAlpha),
                )

                Text(
                    text = "Screen Sharing Active",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "Your screen is being shared",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        } else {
            // Screen share preview placeholder
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = BasicIcons.screenShare,
                    contentDescription = "Screen Share Preview",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Text(
                    text = "Screen Share Preview",
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = "Ready to share your screen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/**
 * Audio-only mode indicator (fallback)
 */
@Composable
private fun AudioOnlyIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = BasicIcons.mic,
                contentDescription = "Audio Only",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )

            Text(
                text = "Audio Only Mode",
                style =
                    MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Camera switch button with Material 3 Expressive styling
 */
@Composable
private fun CameraSwitchButton(
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

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        elevation =
            FloatingActionButtonDefaults.elevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp,
            ),
    ) {
        Icon(
            imageVector = BasicIcons.cameraSwitch,
            contentDescription = "Switch Camera",
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Connecting overlay with loading animation
 */
@Composable
private fun ConnectingOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )

            Text(
                text = "Connecting...",
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
