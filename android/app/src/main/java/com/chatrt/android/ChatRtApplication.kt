package com.chatrt.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChatRtApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
    }
}