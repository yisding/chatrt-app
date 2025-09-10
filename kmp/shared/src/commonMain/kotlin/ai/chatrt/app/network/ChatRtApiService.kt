package ai.chatrt.app.network

import ai.chatrt.app.models.CallRequest
import ai.chatrt.app.models.CallResponse
import ai.chatrt.app.models.ChatRtError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlin.math.min
import kotlin.math.pow

/**
 * API service for ChatRT backend communication
 * Handles HTTP requests with retry logic and error handling
 */
class ChatRtApiService(
    private val baseUrl: String,
    private val httpClient: HttpClient = createDefaultHttpClient(),
) {
    companion object {
        private const val DEFAULT_TIMEOUT_MS = 30_000L
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 10_000L

        /**
         * Creates a default HTTP client with common configuration
         */
        fun createDefaultHttpClient(): HttpClient =
            HttpClient {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            isLenient = true
                            encodeDefaults = true
                        },
                    )
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = DEFAULT_TIMEOUT_MS
                    connectTimeoutMillis = DEFAULT_TIMEOUT_MS
                    socketTimeoutMillis = DEFAULT_TIMEOUT_MS
                }

                install(DefaultRequest) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    header(HttpHeaders.Accept, ContentType.Application.Json)
                }
            }
    }

    /**
     * Creates a new call with the ChatRT backend
     * @param callRequest The call request containing SDP offer and session config
     * @return Result containing the call response or error
     */
    suspend fun createCall(callRequest: CallRequest): Result<CallResponse> =
        executeWithRetry {
            try {
                val response =
                    httpClient.post("$baseUrl/rtc") {
                        setBody(callRequest)
                    }

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val callResponse: CallResponse = response.body()
                        Result.success(callResponse)
                    }
                    HttpStatusCode.BadRequest -> {
                        Result.failure(ChatRtError.ApiError(400, "Bad request - invalid SDP or session config"))
                    }
                    HttpStatusCode.Unauthorized -> {
                        Result.failure(ChatRtError.ApiError(401, "Unauthorized - check API credentials"))
                    }
                    HttpStatusCode.InternalServerError -> {
                        Result.failure(ChatRtError.ApiError(500, "Server error - please try again"))
                    }
                    else -> {
                        Result.failure(ChatRtError.ApiError(response.status.value, "Unexpected error: ${response.status.description}"))
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is HttpRequestTimeoutException -> {
                        Result.failure(ChatRtError.NetworkError)
                    }
                    is ClientRequestException -> {
                        Result.failure(ChatRtError.ApiError(e.response.status.value, e.message ?: "Client error"))
                    }
                    is ServerResponseException -> {
                        Result.failure(ChatRtError.ApiError(e.response.status.value, e.message ?: "Server error"))
                    }
                    else -> {
                        Result.failure(ChatRtError.NetworkError)
                    }
                }
            }
        }

    /**
     * Starts monitoring a call by establishing observer connection
     * @param callId The ID of the call to monitor
     * @return Result indicating success or failure
     */
    suspend fun startCallMonitoring(callId: String): Result<Unit> =
        executeWithRetry {
            try {
                val response = httpClient.post("$baseUrl/observer/$callId")

                when (response.status) {
                    HttpStatusCode.OK -> {
                        Result.success(Unit)
                    }
                    HttpStatusCode.NotFound -> {
                        Result.failure(ChatRtError.ApiError(404, "Call not found"))
                    }
                    HttpStatusCode.InternalServerError -> {
                        Result.failure(ChatRtError.ApiError(500, "Server error - monitoring failed"))
                    }
                    else -> {
                        Result.failure(ChatRtError.ApiError(response.status.value, "Failed to start monitoring"))
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is HttpRequestTimeoutException -> {
                        Result.failure(ChatRtError.NetworkError)
                    }
                    is ClientRequestException -> {
                        Result.failure(ChatRtError.ApiError(e.response.status.value, e.message ?: "Client error"))
                    }
                    is ServerResponseException -> {
                        Result.failure(ChatRtError.ApiError(e.response.status.value, e.message ?: "Server error"))
                    }
                    else -> {
                        Result.failure(ChatRtError.NetworkError)
                    }
                }
            }
        }

    /**
     * Checks the health of the ChatRT backend
     * @return Result indicating if the service is healthy
     */
    suspend fun checkHealth(): Result<Boolean> =
        try {
            val response = httpClient.get("$baseUrl/health")
            Result.success(response.status == HttpStatusCode.OK)
        } catch (e: Exception) {
            Result.failure(ChatRtError.NetworkError)
        }

    /**
     * Executes a network operation with exponential backoff retry logic
     * @param operation The operation to execute
     * @return Result of the operation after retries
     */
    private suspend fun <T> executeWithRetry(operation: suspend () -> Result<T>): Result<T> {
        var lastException: Throwable? = null

        repeat(MAX_RETRY_ATTEMPTS) { attempt ->
            try {
                val result = operation()

                // If successful or non-retryable error, return immediately
                if (result.isSuccess || !isRetryableError(result.exceptionOrNull())) {
                    return result
                }

                lastException = result.exceptionOrNull()
            } catch (e: Exception) {
                lastException = e

                // Don't retry for non-retryable exceptions
                if (!isRetryableError(e)) {
                    return Result.failure(e)
                }
            }

            // Don't delay after the last attempt
            if (attempt < MAX_RETRY_ATTEMPTS - 1) {
                val delayMs = calculateRetryDelay(attempt)
                delay(delayMs)
            }
        }

        // All retries failed, return the last exception
        return Result.failure(lastException ?: ChatRtError.NetworkError)
    }

    /**
     * Determines if an error is retryable
     * @param throwable The error to check
     * @return true if the error should be retried
     */
    private fun isRetryableError(throwable: Throwable?): Boolean =
        when (throwable) {
            is ChatRtError.NetworkError -> true
            is HttpRequestTimeoutException -> true
            is ServerResponseException -> {
                // Retry on 5xx server errors
                throwable.response.status.value >= 500
            }
            is ChatRtError.ApiError -> {
                // Retry on 5xx server errors and some 4xx errors
                throwable.code >= 500 || throwable.code == 429 // Too Many Requests
            }
            else -> false
        }

    /**
     * Calculates the delay for retry attempts using exponential backoff
     * @param attempt The current attempt number (0-based)
     * @return Delay in milliseconds
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RETRY_DELAY_MS * (2.0.pow(attempt)).toLong()
        return min(exponentialDelay, MAX_RETRY_DELAY_MS)
    }

    /**
     * Closes the HTTP client and releases resources
     */
    fun close() {
        httpClient.close()
    }
}
