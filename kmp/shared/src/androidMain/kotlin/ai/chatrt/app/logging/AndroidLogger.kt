package ai.chatrt.app.logging

import android.util.Log

/**
 * Android-specific logger implementation that integrates with Android's Log system
 */
class AndroidLogger(
    private val baseLogger: ChatRtLogger = ChatRtLogger(),
) : Logger by baseLogger {
    override fun debug(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.d(tag, message, throwable)
        baseLogger.debug(tag, message, throwable)
    }

    override fun info(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.i(tag, message, throwable)
        baseLogger.info(tag, message, throwable)
    }

    override fun warning(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.w(tag, message, throwable)
        baseLogger.warning(tag, message, throwable)
    }

    override fun error(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        Log.e(tag, message, throwable)
        baseLogger.error(tag, message, throwable)
    }

    override fun logWebRtcEvent(event: WebRtcEvent) {
        Log.i("WebRTC", "Event: ${event.type.name} for connection ${event.connectionId}")
        baseLogger.logWebRtcEvent(event)
    }

    override fun logConnectionDiagnostic(diagnostic: ConnectionDiagnostic) {
        Log.d("Diagnostics", "${diagnostic.type.name}: ${diagnostic.value}${diagnostic.unit?.let { " $it" } ?: ""}")
        baseLogger.logConnectionDiagnostic(diagnostic)
    }
}
