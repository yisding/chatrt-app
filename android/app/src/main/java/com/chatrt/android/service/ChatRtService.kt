package com.chatrt.android.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service for maintaining WebRTC connections in the background
 */
@AndroidEntryPoint
class ChatRtService : Service() {

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service implementation will be added in subsequent tasks
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup will be implemented in subsequent tasks
    }
}