package ai.chatrt.app.ui.components

import ai.chatrt.app.models.LogEntry
import ai.chatrt.app.models.LogLevel
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * LogsDisplay composable with Material 3 Expressive expandable/collapsible sections
 * Shows real-time logging with timestamps and different severity levels
 *
 * Requirements: 4.4
 */
@Composable
fun LogsDisplay(
    logs: List<LogEntry>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    debugLoggingEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val animatedRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec =
            tween(
                durationMillis = 300,
                easing = EaseInOutCubic,
            ),
    )

    val animatedElevation by animateDpAsState(
        targetValue = if (isExpanded) 4.dp else 2.dp,
        animationSpec =
            tween(
                durationMillis = 300,
                easing = EaseInOutCubic,
            ),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation),
    ) {
        Column {
            // Header with expand/collapse functionality
            LogsHeader(
                logsCount = logs.size,
                isExpanded = isExpanded,
                onToggleExpanded = onToggleExpanded,
                debugLoggingEnabled = debugLoggingEnabled,
                animatedRotation = animatedRotation,
            )

            // Expandable content with Material 3 Expressive animations
            AnimatedVisibility(
                visible = isExpanded,
                enter =
                    expandVertically(
                        animationSpec =
                            tween(
                                durationMillis = 300,
                                easing = EaseInOutCubic,
                            ),
                    ) +
                        fadeIn(
                            animationSpec =
                                tween(
                                    durationMillis = 200,
                                    delayMillis = 100,
                                    easing = EaseInOutCubic,
                                ),
                        ),
                exit =
                    shrinkVertically(
                        animationSpec =
                            tween(
                                durationMillis = 300,
                                easing = EaseInOutCubic,
                            ),
                    ) +
                        fadeOut(
                            animationSpec =
                                tween(
                                    durationMillis = 200,
                                    easing = EaseInOutCubic,
                                ),
                        ),
            ) {
                LogsContent(
                    logs = logs,
                    debugLoggingEnabled = debugLoggingEnabled,
                )
            }
        }
    }
}

/**
 * Logs header with Material 3 Expressive styling and animations
 */
@Composable
private fun LogsHeader(
    logsCount: Int,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    debugLoggingEnabled: Boolean,
    animatedRotation: Float,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onToggleExpanded,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = BasicIcons.terminal,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                Column {
                    Text(
                        text = "Debug Logs",
                        style =
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$logsCount entries",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )

                        // Debug logging status indicator
                        if (debugLoggingEnabled) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary),
                            )

                            Text(
                                text = "Debug Mode",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                    }
                }
            }

            // Expand/collapse icon with rotation animation
            Icon(
                imageVector = BasicIcons.expandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier =
                    Modifier
                        .size(24.dp)
                        .rotate(animatedRotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Logs content with scrollable entries and enhanced typography
 */
@Composable
private fun LogsContent(
    logs: List<LogEntry>,
    debugLoggingEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs are added
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    if (logs.isEmpty()) {
        // Empty state
        Box(
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = BasicIcons.eventNote,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )

                Text(
                    text = "No logs yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text =
                        if (debugLoggingEnabled) {
                            "Debug logging is enabled"
                        } else {
                            "Enable debug logging in settings"
                        },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    } else {
        // Logs list with enhanced Material 3 Expressive styling
        LazyColumn(
            state = listState,
            modifier =
                modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = logs.takeLast(50), // Show only the last 50 logs for performance
                key = { log -> "${log.timestamp}-${log.message.hashCode()}" },
            ) { log ->
                LogEntry(
                    log = log,
                    debugLoggingEnabled = debugLoggingEnabled,
                )
            }
        }
    }
}

/**
 * Individual log entry with Material 3 Expressive styling
 */
@Composable
private fun LogEntry(
    log: LogEntry,
    debugLoggingEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    // Filter debug logs if debug logging is disabled
    if (log.level == LogLevel.DEBUG && !debugLoggingEnabled) {
        return
    }

    val logColor = getLogLevelColor(log.level)
    val logIcon = getLogLevelIcon(log.level)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Log level indicator
            Icon(
                imageVector = logIcon,
                contentDescription = log.level.name,
                modifier = Modifier.size(16.dp),
                tint = logColor,
            )

            // Log content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Log message with monospace font for better readability
                Text(
                    text = log.message,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Timestamp
                Text(
                    text = formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            // Log level badge
            Surface(
                modifier = Modifier.clip(MaterialTheme.shapes.small),
                color = logColor.copy(alpha = 0.1f),
                contentColor = logColor,
            ) {
                Text(
                    text = log.level.name.take(1),
                    style =
                        MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/**
 * Gets the appropriate color for each log level
 */
@Composable
private fun getLogLevelColor(level: LogLevel): Color =
    when (level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.DEBUG -> MaterialTheme.colorScheme.outline
    }

/**
 * Gets the appropriate icon for each log level
 */
private fun getLogLevelIcon(level: LogLevel): ImageVector =
    when (level) {
        LogLevel.ERROR -> BasicIcons.error
        LogLevel.WARNING -> BasicIcons.warning
        LogLevel.INFO -> BasicIcons.info
        LogLevel.DEBUG -> BasicIcons.bugReport
    }

/**
 * Formats timestamp for display
 */
private fun formatTimestamp(timestamp: Long): String =
    try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${localDateTime.hour.toString().padStart(2, '0')}:" +
            "${localDateTime.minute.toString().padStart(2, '0')}:" +
            "${localDateTime.second.toString().padStart(2, '0')}"
    } catch (e: Exception) {
        "00:00:00"
    }
