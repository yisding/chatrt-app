package ai.chatrt.app

import ai.chatrt.app.ui.screens.MainScreen
import ai.chatrt.app.ui.theme.ChatRtTheme
import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
@Preview
fun App() {
    ChatRtTheme {
        // Get ViewModels from Koin using koinInject for commonMain compatibility
        val mainViewModel: MainViewModel = koinInject()
        val settingsViewModel: SettingsViewModel = koinInject()

        MainScreen(
            mainViewModel = mainViewModel,
            settingsViewModel = settingsViewModel,
        )
    }
}
