@file:Suppress("FunctionName")
@file:OptIn(ExperimentalMaterial3Api::class)

package ai.chatrt.app.ui.components

import ai.chatrt.app.logging.ConnectionDiagnostic
import ai.chatrt.app.logging.LogCategory
import ai.chatrt.app.logging.LogEntry
import ai.chatrt.app.logging.LogLevel
import ai.chatrt.app.logging.WebRtcEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Debug panel component for displaying logs, WebRTC events, and diagnostics
 * Supports filtering, export, and real-time updates
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanel(
    logs: List<LogEntry>,
    webRtcEvents: List<WebRtcEvent>,
    diagnostics: List<ConnectionDiagnostic>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onExportLogs: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(0) }
    var logLevelFilter by remember { mutableStateOf<LogLevel?>(null) }
    var categoryFilter by remember { mutableStateOf<LogCategory?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column {
            // Header with expand/collapse button
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Debug Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isExpanded) {
                        IconButton(onClick = onExportLogs) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export Logs",
                            )
                        }

                        IconButton(onClick = onClearLogs) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Logs",
                            )
                        }
                    }

                    IconButton(onClick = onToggleExpanded) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                        )
                    }
                }
            }

            // Expandable content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column {
                    // Tab row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Logs (${logs.size})") },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("WebRTC (${webRtcEvents.size})") },
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Diagnostics (${diagnostics.size})") },
                        )
                    }

                    // Content based on selected tab
                    when (selectedTab) {
                        0 ->
                            LogsTab(
                                logs = logs,
                                levelFilter = logLevelFilter,
                                categoryFilter = categoryFilter,
                                onLevelFilterChange = { logLevelFilter = it },
                                onCategoryFilterChange = { categoryFilter = it },
                            )
                        1 -> WebRtcEventsTab(events = webRtcEvents)
                        2 -> DiagnosticsTab(diagnostics = diagnostics)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsTab(
    logs: List<LogEntry>,
    levelFilter: LogLevel?,
    categoryFilter: LogCategory?,
    onLevelFilterChange: (LogLevel?) -> Unit,
    onCategoryFilterChange: (LogCategory?) -> Unit,
) {
    val filteredLogs =
        remember(logs, levelFilter, categoryFilter) {
            logs.filter { log ->
                (levelFilter == null || log.level.priority >= levelFilter.priority) &&
                    (categoryFilter == null || log.category == categoryFilter)
            }
        }

    Column {
        // Filters
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Level filter
            var levelExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = levelExpanded,
                onExpandedChange = { levelExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = levelFilter?.name ?: "All Levels",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = levelExpanded) },
                    modifier = Modifier.menuAnchor(),
                )

                ExposedDropdownMenu(
                    expanded = levelExpanded,
                    onDismissRequest = { levelExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("All Levels") },
                        onClick = {
                            onLevelFilterChange(null)
                            levelExpanded = false
                        },
                    )
                    LogLevel.entries.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.name) },
                            onClick = {
                                onLevelFilterChange(level)
                                levelExpanded = false
                            },
                        )
                    }
                }
            }

            // Category filter
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.weight(1f),
            ) {
                OutlinedTextField(
                    value = categoryFilter?.name ?: "All Categories",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor(),
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("All Categories") },
                        onClick = {
                            onCategoryFilterChange(null)
                            categoryExpanded = false
                        },
                    )
                    LogCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                onCategoryFilterChange(category)
                                categoryExpanded = false
                            },
                        )
                    }
                }
            }
        }

        // Logs list
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 16.dp),
            state = rememberLazyListState(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filteredLogs.takeLast(100)) { log ->
                LogEntryItem(log = log)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LogEntryItem(log: LogEntry) {
    val backgroundColor =
        when (log.level) {
            LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
            LogLevel.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
            LogLevel.INFO -> MaterialTheme.colorScheme.primaryContainer
            LogLevel.DEBUG -> MaterialTheme.colorScheme.surfaceVariant
        }

    val textColor =
        when (log.level) {
            LogLevel.ERROR -> MaterialTheme.colorScheme.onErrorContainer
            LogLevel.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
            LogLevel.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
            LogLevel.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "${log.level.name} | ${log.category.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                )
                Text(
                    text = formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f),
                )
            }

            Text(
                text = "[${log.tag}] ${log.message}",
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                fontFamily = FontFamily.Monospace,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            log.throwable?.let { throwable ->
                Text(
                    text = "Exception: ${throwable.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun WebRtcEventsTab(events: List<WebRtcEvent>) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(events.takeLast(50)) { event ->
            WebRtcEventItem(event = event)
        }
    }
}

@Composable
private fun WebRtcEventItem(event: WebRtcEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = event.type.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = formatTimestamp(event.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                )
            }

            Text(
                text = "Connection: ${event.connectionId}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )

            if (event.details.isNotEmpty()) {
                Text(
                    text = "Details: ${event.details}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticsTab(diagnostics: List<ConnectionDiagnostic>) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(diagnostics.takeLast(50)) { diagnostic ->
            DiagnosticItem(diagnostic = diagnostic)
        }
    }
}

@Composable
private fun DiagnosticItem(diagnostic: ConnectionDiagnostic) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = diagnostic.type.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "Connection: ${diagnostic.connectionId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${diagnostic.value}${diagnostic.unit?.let { " $it" } ?: ""}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = formatTimestamp(diagnostic.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f),
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Instant): String {
    val localDateTime = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.hour.toString().padStart(2, '0')}:" +
        "${localDateTime.minute.toString().padStart(2, '0')}:" +
        "${localDateTime.second.toString().padStart(2, '0')}"
}
