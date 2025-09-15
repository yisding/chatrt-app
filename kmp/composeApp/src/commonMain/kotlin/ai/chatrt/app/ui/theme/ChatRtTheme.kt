@file:Suppress("FunctionName")

package ai.chatrt.app.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive Theme for ChatRT
 * Provides a modern, personalized user interface with enhanced motion and visual design
 * Requirements: 6.2 (Material 3 Expressive UI)
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

    val motionScheme = ChatRtMotionScheme()
    val expressiveShapes = ChatRtExpressiveShapes()

    CompositionLocalProvider(
        LocalChatRtMotionScheme provides motionScheme,
        LocalChatRtExpressiveShapes provides expressiveShapes,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ChatRtExpressiveTypography,
            shapes = ChatRtShapes,
            content = content,
        )
    }
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

/**
 * Material 3 Expressive Motion Scheme
 * Provides enhanced animation and motion design system
 */
data class ChatRtMotionScheme(
    // Connection state animations
    val connectionPulse: AnimationSpec<Float> =
        infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
    // Button press animations
    val buttonPress: AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
    // Video mode transitions
    val videoModeTransition: AnimationSpec<Float> =
        tween(
            durationMillis = 400,
            easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f),
        ),
    // Camera switch animation
    val cameraSwitchFlip: AnimationSpec<Float> =
        tween(
            durationMillis = 600,
            easing = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f),
        ),
    // Error display animations
    val errorSlideIn: AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
    // Log entry animations
    val logEntryFadeIn: AnimationSpec<Float> =
        tween(
            durationMillis = 300,
            easing = EaseOutCubic,
        ),
    // Settings navigation
    val settingsSlide: AnimationSpec<Float> =
        tween(
            durationMillis = 350,
            easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f),
        ),
    // Micro-interactions
    val hoverScale: AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
    val rippleExpansion: AnimationSpec<Float> =
        tween(
            durationMillis = 200,
            easing = EaseOutQuart,
        ),
)

/**
 * Material 3 Expressive Shapes
 * Enhanced shape system for more expressive UI components
 */
data class ChatRtExpressiveShapes(
    // Connection status shapes
    val connectionIndicator: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(50),
    // Video preview shapes
    val videoPreviewLarge: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(24.dp),
    val videoPreviewSmall: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(16.dp),
    // Button shapes with expressive corners
    val primaryButton: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(20.dp),
    val secondaryButton: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(16.dp),
    // Card shapes for logs and errors
    val logCard: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(12.dp),
    val errorCard: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(16.dp),
    // Settings shapes
    val settingsCard: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(20.dp),
    // Floating action button shape
    val fab: androidx.compose.foundation.shape.RoundedCornerShape =
        androidx.compose.foundation.shape
            .RoundedCornerShape(28.dp),
)

// Composition locals for accessing theme extensions
val LocalChatRtMotionScheme = staticCompositionLocalOf { ChatRtMotionScheme() }
val LocalChatRtExpressiveShapes = staticCompositionLocalOf { ChatRtExpressiveShapes() }

// Extension properties for easy access
val MaterialTheme.chatRtMotion: ChatRtMotionScheme
    @Composable get() = LocalChatRtMotionScheme.current

val MaterialTheme.chatRtShapes: ChatRtExpressiveShapes
    @Composable get() = LocalChatRtExpressiveShapes.current

/**
 * Enhanced Material 3 Expressive Typography
 * Fine-tuned typography scales for optimal user experience
 */
val ChatRtExpressiveTypography =
    Typography(
        // Display styles for main headings
        displayLarge =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
        displayMedium =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),
        // Headline styles for section headers
        headlineLarge =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        // Title styles for component headers
        titleLarge =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
        titleSmall =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        // Body styles for main content
        bodyLarge =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp,
            ),
        bodyMedium =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        // Label styles for buttons and form elements
        labelLarge =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
        labelSmall =
            androidx.compose.ui.text.TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )
