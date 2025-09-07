package ai.chatrt.app.models

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