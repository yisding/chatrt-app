package ai.chatrt.app

import ai.chatrt.app.viewmodel.MainViewModel
import ai.chatrt.app.viewmodel.SettingsViewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.KoinTest
import kotlin.test.assertNotNull

/**
 * Integration test for Koin dependency injection in Android context
 */
@RunWith(AndroidJUnit4::class)
class KoinIntegrationTest : KoinTest {
    
    @Test
    fun testKoinDependencyInjectionWorks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val application = context.applicationContext as ChatRtApplication
        
        // Verify that Koin is initialized
        val koin = getKoin()
        assertNotNull(koin)
        
        // Verify that ViewModels can be created
        val mainViewModel = koin.get<MainViewModel>()
        val settingsViewModel = koin.get<SettingsViewModel>()
        
        assertNotNull(mainViewModel)
        assertNotNull(settingsViewModel)
    }
}