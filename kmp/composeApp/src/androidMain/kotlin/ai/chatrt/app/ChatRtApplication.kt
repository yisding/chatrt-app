package ai.chatrt.app

import ai.chatrt.app.di.androidModule
import ai.chatrt.app.di.initKoin
import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

/**
 * Android Application class with Koin dependency injection initialization
 */
class ChatRtApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin dependency injection
        initKoin(
            platformModules = listOf(androidModule),
        ) {
            // Enable Android logging for Koin (only in debug builds)
            androidLogger(Level.DEBUG)

            // Provide Android context
            androidContext(this@ChatRtApplication)
        }
    }
}
