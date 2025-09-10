package ai.chatrt.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Material 3 Expressive Theme for ChatRT
 * Provides a modern, personalized user interface with enhanced motion and visual design
 */
@Composable
fun ChatRtTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) {
            chatRtDarkColorScheme()
        } else {
            chatRtLightColorScheme()
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChatRtTypography,
        shapes = ChatRtShapes,
        content = content,
    )
}

/**
 * Light color scheme for ChatRT with Material 3 Expressive colors
 */
private fun chatRtLightColorScheme() =
    lightColorScheme(
        primary = Color(0xFF1976D2),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFE3F2FD),
        onPrimaryContainer = Color(0xFF0D47A1),
        secondary = Color(0xFF4CAF50),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE8F5E8),
        onSecondaryContainer = Color(0xFF1B5E20),
        tertiary = Color(0xFFFF9800),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFF3E0),
        onTertiaryContainer = Color(0xFFE65100),
        error = Color(0xFFD32F2F),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFEBEE),
        onErrorContainer = Color(0xFFB71C1C),
        background = Color(0xFFFAFAFA),
        onBackground = Color(0xFF1C1B1F),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFF313033),
        inverseOnSurface = Color(0xFFF4EFF4),
        inversePrimary = Color(0xFF90CAF9),
    )

/**
 * Dark color scheme for ChatRT with Material 3 Expressive colors
 */
private fun chatRtDarkColorScheme() =
    darkColorScheme(
        primary = Color(0xFF90CAF9),
        onPrimary = Color(0xFF0D47A1),
        primaryContainer = Color(0xFF1565C0),
        onPrimaryContainer = Color(0xFFE3F2FD),
        secondary = Color(0xFF81C784),
        onSecondary = Color(0xFF1B5E20),
        secondaryContainer = Color(0xFF388E3C),
        onSecondaryContainer = Color(0xFFE8F5E8),
        tertiary = Color(0xFFFFB74D),
        onTertiary = Color(0xFFE65100),
        tertiaryContainer = Color(0xFFF57C00),
        onTertiaryContainer = Color(0xFFFFF3E0),
        error = Color(0xFFEF5350),
        onError = Color(0xFFB71C1C),
        errorContainer = Color(0xFFC62828),
        onErrorContainer = Color(0xFFFFEBEE),
        background = Color(0xFF121212),
        onBackground = Color(0xFFE6E1E5),
        surface = Color(0xFF1E1E1E),
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF2C2C2C),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        scrim = Color(0xFF000000),
        inverseSurface = Color(0xFFE6E1E5),
        inverseOnSurface = Color(0xFF313033),
        inversePrimary = Color(0xFF1976D2),
    )
