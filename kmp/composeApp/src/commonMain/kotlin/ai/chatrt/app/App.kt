package ai.chatrt.app

import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        // For now, show a simple demo screen that demonstrates Koin integration
        // This will be replaced with the actual MainScreen in task 9
        DemoScreen()
    }
}

@Composable
fun DemoScreen() {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ChatRT Android",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Koin Dependency Injection Setup Complete",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Ready for UI implementation in task 8",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}