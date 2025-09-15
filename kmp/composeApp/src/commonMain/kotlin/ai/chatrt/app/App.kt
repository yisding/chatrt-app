@file:Suppress("FunctionName")

package ai.chatrt.app

import ai.chatrt.app.ui.screens.MainScreen
import ai.chatrt.app.ui.theme.ChatRtTheme
import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

/**
 * Main App composable - common implementation without navigation
 * Platform-specific implementations can add navigation as needed
 * Requirements: 6.1, 6.2, 6.3
 */
@Composable
@Preview
fun App() {
    ChatRtTheme {
        // Get ViewModels from Koin using koinInject for commonMain compatibility
        val mainViewModel: MainViewModel = koinInject()
        val settingsViewModel: SettingsViewModel = koinInject()

        // Simple main screen without navigation for common implementation
        MainScreen(
            mainViewModel = mainViewModel,
            settingsViewModel = settingsViewModel,
            onNavigateToSettings = {
                // Platform-specific navigation will be handled by platform implementations
            },
        )
    }
}
