package ai.chatrt.app.di

import ai.chatrt.app.logging.AndroidDebugInfoCollector
import ai.chatrt.app.logging.AndroidLogStorageManager
import ai.chatrt.app.logging.AndroidLogger
import ai.chatrt.app.logging.LogStorageManager
import ai.chatrt.app.platform.AndroidAudioManager
import ai.chatrt.app.platform.AndroidBatteryMonitor
import ai.chatrt.app.platform.AndroidLifecycleManager
import ai.chatrt.app.platform.AndroidNetworkMonitor
import ai.chatrt.app.platform.AndroidPlatformManager
import ai.chatrt.app.platform.AndroidScreenCaptureManager
import ai.chatrt.app.platform.AndroidVideoManager
import ai.chatrt.app.platform.AndroidWebRtcManager
import ai.chatrt.app.platform.AudioManager
import ai.chatrt.app.platform.BatteryMonitor
import ai.chatrt.app.platform.LifecycleManager
import ai.chatrt.app.platform.NetworkMonitor
import ai.chatrt.app.platform.PermissionManager
import ai.chatrt.app.platform.PlatformManager
import ai.chatrt.app.platform.ScreenCaptureManager
import ai.chatrt.app.platform.SimpleAndroidPermissionManager
import ai.chatrt.app.platform.VideoManager
import ai.chatrt.app.platform.WebRtcManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module for platform dependencies
 */
val androidModule =
    module {
        // Android-specific logging components
        single<LogStorageManager> { AndroidLogStorageManager(androidContext()) }
        single { AndroidLogger() }
        single { AndroidDebugInfoCollector(androidContext(), get()) }

        // Core platform managers (bind concrete + interface alias to share one instance)
        single { AndroidLifecycleManager(androidContext()) }
        single<LifecycleManager> { get<AndroidLifecycleManager>() }

        single<PermissionManager> { SimpleAndroidPermissionManager() }

        single { AndroidAudioManager(androidContext()) }
        single<AudioManager> { get<AndroidAudioManager>() }

        single { AndroidVideoManager(androidContext()) }
        single<VideoManager> { get<AndroidVideoManager>() }

        single { AndroidScreenCaptureManager(androidContext()) }
        single<ScreenCaptureManager> { get<AndroidScreenCaptureManager>() }

        // Network and battery monitoring
        single { AndroidNetworkMonitor(androidContext()) }
        single<NetworkMonitor> { get<AndroidNetworkMonitor>() }

        single { AndroidBatteryMonitor(androidContext()) }
        single<BatteryMonitor> { get<AndroidBatteryMonitor>() }

        // WebRTC manager with dependencies
        single {
            AndroidWebRtcManager(
                context = androidContext(),
                audioManager = get(),
                videoManager = get(),
                screenCaptureManager = get(),
            )
        }
        single<WebRtcManager> { get<AndroidWebRtcManager>() }

        // Platform manager that coordinates all platform services
        single<PlatformManager> { AndroidPlatformManager(androidContext()) }
    }
