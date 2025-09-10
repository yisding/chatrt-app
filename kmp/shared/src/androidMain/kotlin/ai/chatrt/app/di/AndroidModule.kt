package ai.chatrt.app.di

import ai.chatrt.app.platform.*
import android.content.Context
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module for platform managers
 * Contains Android platform implementations and system services
 */
val androidModule =
    module {

        // Override HTTP Client with Android engine
        single<HttpClient>(createdAtStart = true) {
            HttpClient(Android) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        },
                    )
                }
            }
        }

        // Platform Manager
        single<PlatformManager> {
            AndroidPlatformManager(
                context = androidContext(),
            )
        }

        // WebRTC Manager
        single<WebRtcManager> {
            AndroidWebRtcManager(
                context = androidContext(),
            )
        }

        // Audio Manager
        single<AudioManager> {
            AndroidAudioManager(
                context = androidContext(),
            )
        }

        // Video Manager
        single<VideoManager> {
            AndroidVideoManager(
                context = androidContext(),
            )
        }

        // Screen Capture Manager
        single<ScreenCaptureManager> {
            AndroidScreenCaptureManager(
                context = androidContext(),
            )
        }

        // Permission Manager
        single<PermissionManager> {
            AndroidPermissionManager(
                context = androidContext(),
            )
        }

        // Network Monitor
        single<NetworkMonitor> {
            AndroidNetworkMonitor(
                context = androidContext(),
            )
        }

        // Battery Monitor
        single<BatteryMonitor> {
            AndroidBatteryMonitor(
                context = androidContext(),
            )
        }

        // Lifecycle Manager
        single<LifecycleManager> {
            AndroidLifecycleManager(
                context = androidContext(),
            )
        }
    }
