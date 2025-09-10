package ai.chatrt.app.ui.components

import ai.chatrt.app.models.VideoMode
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * VideoModeSelector with Material 3 Expressive radio buttons and enhanced visual feedback
 * Provides selection between audio-only, webcam, and screen sharing modes
 *
 * Requirements: 1.2, 2.1, 3.1
 */
@Composable
fun VideoModeSelector(
    selectedMode: VideoMode,
    onModeSelected: (VideoMode) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
        ) {
            Text(
                text = "Communication Mode",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VideoMode.entries.forEach { mode ->
                    VideoModeOption(
                        mode = mode,
                        isSelected = selectedMode == mode,
                        onSelected = { onModeSelected(mode) },
                        enabled = enabled,
                    )
                }
            }
        }
    }
}

/**
 * Individual video mode option with Material 3 Expressive styling and animations
 */
@Composable
private fun VideoModeOption(
    mode: VideoMode,
    isSelected: Boolean,
    onSelected: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec =
            tween(
                durationMillis = 200,
                easing = EaseInOutCubic,
            ),
    )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .scale(animatedScale)
                .selectable(
                    selected = isSelected,
                    onClick = onSelected,
                    enabled = enabled,
                    role = Role.RadioButton,
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Radio button with enhanced styling
            RadioButton(
                selected = isSelected,
                onClick = null, // Handled by card click
                enabled = enabled,
                colors =
                    RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.outline,
                    ),
            )

            // Mode icon with animation
            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            )

            Icon(
                imageVector = getModeIcon(mode),
                contentDescription = null,
                modifier =
                    Modifier
                        .size(24.dp)
                        .scale(iconScale),
                tint =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )

            // Mode details
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = getModeTitle(mode),
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )

                Text(
                    text = getModeDescription(mode),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                )
            }

            // Selection indicator with animation
            AnimatedVisibility(
                visible = isSelected,
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
                Icon(
                    imageVector = BasicIcons.checkCircle,
                    contentDescription = "Selected",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Gets the appropriate icon for each video mode
 */
private fun getModeIcon(mode: VideoMode): ImageVector =
    when (mode) {
        VideoMode.AUDIO_ONLY -> BasicIcons.mic
        VideoMode.WEBCAM -> BasicIcons.videocam
        VideoMode.SCREEN_SHARE -> BasicIcons.screenShare
    }

/**
 * Gets the display title for each video mode
 */
private fun getModeTitle(mode: VideoMode): String =
    when (mode) {
        VideoMode.AUDIO_ONLY -> "Voice Only"
        VideoMode.WEBCAM -> "Video Chat"
        VideoMode.SCREEN_SHARE -> "Screen Share"
    }

/**
 * Gets the description for each video mode
 */
private fun getModeDescription(mode: VideoMode): String =
    when (mode) {
        VideoMode.AUDIO_ONLY -> "Audio conversation with AI"
        VideoMode.WEBCAM -> "Video call with camera feed"
        VideoMode.SCREEN_SHARE -> "Share your screen with AI"
    }
