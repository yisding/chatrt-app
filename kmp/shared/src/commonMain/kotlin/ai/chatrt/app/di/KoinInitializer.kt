package ai.chatrt.app.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration

/**
 * Shared Koin initialization function
 * Can be used by different platforms to initialize Koin with platform-specific modules
 */
fun initKoin(
    platformModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration = {},
) = startKoin {
    appDeclaration()
    modules(
        listOf(sharedModule) + platformModules,
    )
}

/**
 * Initialize Koin for shared module only (useful for testing)
 */
fun initKoinShared() = initKoin()
