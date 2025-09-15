package ai.chatrt.app.di

import ai.chatrt.app.logging.ChatRtLogger
import ai.chatrt.app.logging.DebugInfoCollector
import ai.chatrt.app.logging.Logger
import ai.chatrt.app.logging.WebRtcEventLogger
import ai.chatrt.app.network.ChatRtApiService
import ai.chatrt.app.repository.ChatRepository
import ai.chatrt.app.repository.ChatRepositoryImpl
import ai.chatrt.app.repository.SettingsRepository
import ai.chatrt.app.repository.SettingsRepositoryImpl
import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

/**
 * Shared Koin module for repositories and ViewModels
 * Contains dependencies that are common across all platforms
 */
val sharedModule =
    module {

        // Logging System
        single<Logger> { ChatRtLogger() }
        single<WebRtcEventLogger> { WebRtcEventLogger(get()) }
        single<DebugInfoCollector> { DebugInfoCollector(get(), get()) }

        // HTTP Client configuration
        single<HttpClient> {
            HttpClient {
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

        // API Service
        single<ChatRtApiService> {
            ChatRtApiService(
                // Default ChatRT backend URL
                baseUrl = "https://chatrt.val.town",
                httpClient = get(),
            )
        }

        // Repositories
        single<ChatRepository> {
            ChatRepositoryImpl(
                webRtcManager = get(),
                apiService = get(),
            )
        }

        single<SettingsRepository> {
            SettingsRepositoryImpl()
        }

        // ViewModels
        factory<MainViewModel> {
            MainViewModel(
                chatRepository = get(),
                logger = get(),
                webRtcEventLogger = get(),
                debugInfoCollector = get(),
            )
        }

        factory<SettingsViewModel> {
            SettingsViewModel(
                settingsRepository = get(),
            )
        }
    }
