package ai.chatrt.app

import ai.chatrt.app.ui.screens.MainScreen
import ai.chatrt.app.ui.screens.SettingsScreen
import ai.chatrt.app.ui.theme.ChatRtTheme
import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.compose.koinInject

/**
 * Android-specific App composable with navigation setup using Material 3 Expressive navigation
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
@Suppress("FunctionName")
@Composable
fun AndroidApp() {
    ChatRtTheme {
        // Get ViewModels from Koin using koinInject for commonMain compatibility
        val mainViewModel: MainViewModel = koinInject()
        val settingsViewModel: SettingsViewModel = koinInject()

        // Navigation setup
        val navController = rememberNavController()

        // NavHost with Material 3 Expressive motion system for navigation transitions
        NavHost(
            navController = navController,
            startDestination = "main",
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec =
                        tween(
                            durationMillis = 300,
                            easing = EaseInOutCubic,
                        ),
                ) +
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 300,
                                easing = EaseInOutCubic,
                            ),
                    )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec =
                        tween(
                            durationMillis = 300,
                            easing = EaseInOutCubic,
                        ),
                ) +
                    fadeOut(
                        animationSpec =
                            tween(
                                durationMillis = 300,
                                easing = EaseInOutCubic,
                            ),
                    )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec =
                        tween(
                            durationMillis = 300,
                            easing = EaseInOutCubic,
                        ),
                ) +
                    fadeIn(
                        animationSpec =
                            tween(
                                durationMillis = 300,
                                easing = EaseInOutCubic,
                            ),
                    )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec =
                        tween(
                            durationMillis = 300,
                            easing = EaseInOutCubic,
                        ),
                ) +
                    fadeOut(
                        animationSpec =
                            tween(
                                durationMillis = 300,
                                easing = EaseInOutCubic,
                            ),
                    )
            },
        ) {
            // Main Screen Route
            composable("main") {
                MainScreen(
                    mainViewModel = mainViewModel,
                    settingsViewModel = settingsViewModel,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                )
            }

            // Settings Screen Route
            composable("settings") {
                SettingsScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
