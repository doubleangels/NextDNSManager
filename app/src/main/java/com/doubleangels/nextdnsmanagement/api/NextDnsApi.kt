package com.doubleangels.nextdnsmanagement.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Thrown for any failed API call. [isAuthError] means the API key was rejected. */
class ApiException(
    message: String,
    val httpCode: Int? = null,
    val isAuthError: Boolean = false,
) : Exception(message)

/** The `data` and `meta` members of a NextDNS API response. */
data class ApiResponse(val data: JsonElement?, val meta: JsonObject?)

/**
 * Thin client for https://api.nextdns.io.
 *
 * Every response is either `{"data": ..., "meta": ...}` or `{"errors": [...]}`, and the
 * API can return errors with HTTP 200, so the body is always checked. Responses are
 * kept as JSON trees and read leniently (see Json.kt), because field names in the
 * public docs and the live API do not always agree.
 */
class NextDnsApi(private val apiKey: () -> String?) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Log streaming keeps the connection open indefinitely.
    private val streamClient = client.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    suspend fun get(path: String, query: Map<String, String?> = emptyMap()): ApiResponse =
        execute(request(path, query).get().build())

    suspend fun patch(path: String, body: JsonElement): ApiResponse =
        execute(request(path).patch(body.toRequestBody()).build())

    suspend fun post(path: String, body: JsonElement): ApiResponse =
        execute(request(path).post(body.toRequestBody()).build())

    suspend fun put(path: String, body: JsonElement): ApiResponse =
        execute(request(path).put(body.toRequestBody()).build())

    suspend fun delete(path: String): ApiResponse =
        execute(request(path).delete().build())

    /**
     * Streams new log entries as Server-Sent Events. [fromId] is a stream id from
     * `meta.stream.id` of the logs endpoint, so the stream continues where a page of
     * logs ended.
     */
    fun streamLogs(profileId: String, fromId: String?): Flow<JsonObject> = flow {
        val query = if (fromId != null) mapOf("id" to fromId) else emptyMap()
        val call = streamClient.newCall(request("profiles/$profileId/logs/stream", query).get().build())
        currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        call.execute().use { response ->
            checkStatus(response, null)
            val source = response.body.source()
            while (currentCoroutineContext().isActive) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data:")) {
                    val payload = line.removePrefix("data:").trim()
                    if (payload.isNotEmpty()) {
                        (runCatching { JSON.parseToJsonElement(payload) }.getOrNull() as? JsonObject)
                            ?.let { emit(it) }
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun request(path: String, query: Map<String, String?> = emptyMap()): Request.Builder {
        val key = apiKey() ?: throw ApiException("No API key", isAuthError = true)
        val url: HttpUrl = BASE_URL.newBuilder().apply {
            addPathSegments(path)
            query.forEach { (name, value) -> if (value != null) addQueryParameter(name, value) }
        }.build()
        return Request.Builder()
            .url(url)
            .header("X-Api-Key", key)
            .header("Accept", "application/json")
    }

    private suspend fun execute(request: Request): ApiResponse {
        var attempt = 0
        while (true) {
            val response = client.newCall(request).await()
            val retryable = response.code == 429 || response.code >= 500
            if (retryable && attempt < MAX_RETRIES) {
                response.close()
                delay(1000L shl attempt)
                attempt++
                continue
            }
            return response.use { parse(it) }
        }
    }

    private fun parse(response: Response): ApiResponse {
        val text = response.body.string()
        val root = if (text.isBlank()) null else runCatching { JSON.parseToJsonElement(text) }.getOrNull()
        checkStatus(response, root)
        val obj = root as? JsonObject ?: return ApiResponse(root, null)
        return ApiResponse(obj["data"], obj["meta"] as? JsonObject)
    }

    private fun checkStatus(response: Response, root: JsonElement?) {
        val errors = (root as? JsonObject)?.get("errors") as? JsonArray
        val detail = errors?.mapNotNull { error ->
            val obj = error as? JsonObject ?: return@mapNotNull null
            obj.str("detail") ?: obj.str("code")
        }?.joinToString("\n")
        val auth = response.code == 401 || response.code == 403
        if (auth) {
            throw ApiException(detail ?: "The API key was rejected", response.code, isAuthError = true)
        }
        if (!errors.isNullOrEmpty()) {
            throw ApiException(detail ?: "Request failed", response.code)
        }
        if (!response.isSuccessful) {
            throw ApiException("Request failed (HTTP ${response.code})", response.code)
        }
    }

    private fun JsonElement.toRequestBody(): RequestBody =
        toString().toRequestBody(JSON_MEDIA_TYPE)

    companion object {
        private val BASE_URL = "https://api.nextdns.io/".toHttpUrl()
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val MAX_RETRIES = 3
        val JSON = Json { ignoreUnknownKeys = true }
    }
}

/** Suspends until the call completes, cancelling it if the coroutine is cancelled. */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response)
        }

        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
    })
}
