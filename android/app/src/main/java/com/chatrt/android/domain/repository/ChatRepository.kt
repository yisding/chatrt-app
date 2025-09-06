package com.chatrt.android.domain.repository

import com.chatrt.android.data.model.ConnectionState
import com.chatrt.android.data.model.LogEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for ChatRT functionality
 */
interface ChatRepository {
    suspend fun createCall(sdpOffer: String): Result<String>
    fun observeConnectionState(): Flow<ConnectionState>
    fun observeLogs(): Flow<List<LogEntry>>
}