package ai.chatrt.app.ui.components

import ai.chatrt.app.models.OptimizationReason
import ai.chatrt.app.models.PlatformOptimization
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
 * OptimizationSuggestion composable with Material 3 Expressive suggestion cards and animations
 * Shows platform optimization recommendations with apply/dismiss actions
 *
 * Requirements: Platform optimization suggestions
 */
@Composable
fun OptimizationSuggestion(
    optimization: PlatformOptimization,
    onApplyOptimization: (PlatformOptimization) -> Unit,
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
        targetValue = 4.dp,
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
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
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
            // Optimization header
            OptimizationHeader(
                optimization = optimization,
                onDismiss = onDismiss,
            )

            // Optimization details
            OptimizationContent(optimization = optimization)

            // Action buttons
            OptimizationActions(
                optimization = optimization,
                onApplyOptimization = onApplyOptimization,
                onDismiss = onDismiss,
            )
        }
    }
}

/**
 * Optimization header with icon, title, and dismiss button
 */
@Composable
private fun OptimizationHeader(
    optimization: PlatformOptimization,
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
            // Optimization icon with gentle pulse animation
            val infiniteTransition = rememberInfiniteTransition()
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOut),
                        repeatMode = RepeatMode.Reverse,
                    ),
            )

            Icon(
                imageVector = getOptimizationIcon(optimization.reason),
                contentDescription = "Optimization",
                modifier =
                    Modifier
                        .size(24.dp)
                        .scale(pulseScale),
                tint = MaterialTheme.colorScheme.tertiary,
            )

            Text(
                text = "Performance Suggestion",
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }

        // Dismiss button
        IconButton(
            onClick = onDismiss,
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
 * Optimization content with recommendations and benefits
 */
@Composable
private fun OptimizationContent(
    optimization: PlatformOptimization,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Main recommendation message
        Text(
            text = getOptimizationMessage(optimization),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )

        // Recommended changes card
        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = BasicIcons.trendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    Text(
                        text = "Recommended Changes:",
                        style =
                            MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                // Video mode recommendation
                OptimizationItem(
                    icon = BasicIcons.videocam,
                    label = "Video Mode",
                    value = getVideoModeDisplayName(optimization.recommendedVideoMode),
                    isChange = true,
                )

                // Audio quality recommendation
                OptimizationItem(
                    icon = BasicIcons.volumeUp,
                    label = "Audio Quality",
                    value = getAudioQualityDisplayName(optimization.recommendedAudioQuality),
                    isChange = true,
                )

                // Additional optimizations
                if (optimization.disableVideoPreview) {
                    OptimizationItem(
                        icon = BasicIcons.visibilityOff,
                        label = "Video Preview",
                        value = "Disabled for better performance",
                        isChange = true,
                    )
                }
            }
        }

        // Benefits section
        val benefits = getOptimizationBenefits(optimization.reason)
        if (benefits.isNotEmpty()) {
            Card(
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                            imageVector = BasicIcons.checkCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )

                        Text(
                            text = "Expected Benefits:",
                            style =
                                MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    benefits.forEach { benefit ->
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
                                text = benefit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
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
 * Individual optimization item display
 */
@Composable
private fun OptimizationItem(
    icon: ImageVector,
    label: String,
    value: String,
    isChange: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isChange) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
        )

        Text(
            text = label,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isChange) FontWeight.SemiBold else FontWeight.Normal,
                ),
            color = if (isChange) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Optimization action buttons
 */
@Composable
private fun OptimizationActions(
    optimization: PlatformOptimization,
    onApplyOptimization: (PlatformOptimization) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Apply optimization button
        Button(
            onClick = { onApplyOptimization(optimization) },
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
        ) {
            Icon(
                imageVector = BasicIcons.checkCircle,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Apply",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }

        // Not now button
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors =
                ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ),
            border =
                ButtonDefaults.outlinedButtonBorder.copy(
                    brush =
                        androidx.compose.foundation
                            .BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.onTertiaryContainer,
                            ).brush,
                ),
        ) {
            Text(
                text = "Not Now",
                style =
                    MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
            )
        }
    }
}

/**
 * Gets the appropriate icon for each optimization reason
 */
private fun getOptimizationIcon(reason: OptimizationReason): ImageVector =
    when (reason) {
        OptimizationReason.LOW_BATTERY -> BasicIcons.batteryAlert
        OptimizationReason.HIGH_CPU_USAGE -> BasicIcons.memory
        OptimizationReason.LOW_MEMORY -> BasicIcons.storage
        OptimizationReason.POOR_NETWORK -> BasicIcons.wifi
    }

/**
 * Gets the optimization message for each reason
 */
private fun getOptimizationMessage(optimization: PlatformOptimization): String =
    when (optimization.reason) {
        OptimizationReason.LOW_BATTERY -> "Your battery is running low. We recommend reducing video quality to extend battery life."
        OptimizationReason.HIGH_CPU_USAGE -> "High CPU usage detected. Reducing video processing can improve performance."
        OptimizationReason.LOW_MEMORY -> "Available memory is low. Switching to audio-only mode can improve stability."
        OptimizationReason.POOR_NETWORK -> "Poor network quality detected. Audio-only mode will provide a better experience."
    }

/**
 * Gets the display name for video modes
 */
private fun getVideoModeDisplayName(videoMode: ai.chatrt.app.models.VideoMode): String =
    when (videoMode) {
        ai.chatrt.app.models.VideoMode.AUDIO_ONLY -> "Audio Only"
        ai.chatrt.app.models.VideoMode.WEBCAM -> "Video Chat"
        ai.chatrt.app.models.VideoMode.SCREEN_SHARE -> "Screen Share"
    }

/**
 * Gets the display name for audio quality
 */
private fun getAudioQualityDisplayName(audioQuality: ai.chatrt.app.models.AudioQuality): String =
    when (audioQuality) {
        ai.chatrt.app.models.AudioQuality.LOW -> "Low Quality"
        ai.chatrt.app.models.AudioQuality.MEDIUM -> "Medium Quality"
        ai.chatrt.app.models.AudioQuality.HIGH -> "High Quality"
    }

/**
 * Gets the expected benefits for each optimization reason
 */
private fun getOptimizationBenefits(reason: OptimizationReason): List<String> =
    when (reason) {
        OptimizationReason.LOW_BATTERY ->
            listOf(
                "Extended battery life",
                "Reduced power consumption",
                "Longer conversation time",
            )
        OptimizationReason.HIGH_CPU_USAGE ->
            listOf(
                "Improved app responsiveness",
                "Reduced device heating",
                "Better overall performance",
            )
        OptimizationReason.LOW_MEMORY ->
            listOf(
                "Improved app stability",
                "Reduced risk of crashes",
                "Better multitasking performance",
            )
        OptimizationReason.POOR_NETWORK ->
            listOf(
                "More stable connection",
                "Reduced audio dropouts",
                "Better call quality",
            )
    }
