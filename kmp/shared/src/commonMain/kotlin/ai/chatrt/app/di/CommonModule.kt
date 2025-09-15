package ai.chatrt.app.di

import ai.chatrt.app.network.ChatRtApiService
import ai.chatrt.app.repository.ChatRepository
import ai.chatrt.app.repository.ChatRepositoryImpl
import ai.chatrt.app.repository.SettingsRepository
import ai.chatrt.app.repository.SettingsRepositoryImpl
import ai.chatrt.app.utils.ErrorHandler
import ai.chatrt.app.utils.ErrorRecoveryManager
import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import org.koin.dsl.module

/**
 * Common Koin module for shared dependencies across all platforms
 */
val commonModule =
    module {
        // Network layer
        single { ChatRtApiService("https://api.chatrt.ai") }

        // Repositories
        single<ChatRepository> { ChatRepositoryImpl(get(), get()) }
        single<SettingsRepository> { SettingsRepositoryImpl() }

        // Error handling
        single { ErrorHandler() }
        single { ErrorRecoveryManager(get()) }

        // ViewModels (provide as factories to avoid Android-specific viewModel DSL in commonMain)
        factory { MainViewModel(chatRepository = get(), logger = get(), webRtcEventLogger = get(), debugInfoCollector = get()) }
        factory { SettingsViewModel(get()) }
    }
