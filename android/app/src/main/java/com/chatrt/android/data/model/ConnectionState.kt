package com.chatrt.android.data.model

/**
 * Represents the current state of the WebRTC connection
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    FAILED,
    RECONNECTING
}