package com.redcode.app.data.remote

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed class SseEvent {
    data class Open(val response: Response) : SseEvent()
    data class Message(val id: String?, val type: String?, val data: String) : SseEvent()
    data class Closed(val reason: String = "Closed") : SseEvent()
    data class Failure(val throwable: Throwable?, val response: Response?) : SseEvent()
}

@Singleton
class OpenCodeSseClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "OpenCodeSseClient"
    }

    private val _last50Events = MutableStateFlow<List<String>>(emptyList())
    val last50Events: StateFlow<List<String>> = _last50Events.asStateFlow()

    fun recordEvent(raw: String) {
        val current = _last50Events.value.toMutableList()
        current.add(raw)
        if (current.size > 50) current.removeAt(0)
        _last50Events.value = current
    }

    fun observeEventStream(url: String, password: String): Flow<SseEvent> = callbackFlow {
        // Dedicated OkHttpClient with infinite readTimeout (0) for SSE streaming
        val sseClient = okHttpClient.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")

        if (password.isNotBlank()) {
            val credentials = "opencode:$password"
            val basicAuth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
            requestBuilder.header("Authorization", basicAuth)
        }
        val request = requestBuilder.build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE Connected to $url (code=${response.code})")
                trySend(SseEvent.Open(response))
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                recordEvent(data)
                // In OpenCode v2, event type is embedded in JSON `data`: {"type":"session.text.delta", ...}
                val resolvedType = if (!type.isNullOrBlank()) type else {
                    try {
                        JSONObject(data).optString("type")
                    } catch (_: Exception) {
                        null
                    }
                }
                Log.d(TAG, "SSE Event: type=$resolvedType, data=$data")
                trySend(SseEvent.Message(id, resolvedType, data))
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE Closed")
                trySend(SseEvent.Closed())
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE Failure: ${t?.message}, code=${response?.code}")
                trySend(SseEvent.Failure(t, response))
                close(t)
            }
        }

        val eventSource = EventSources.createFactory(sseClient).newEventSource(request, listener)

        awaitClose {
            Log.d(TAG, "Cancelling SSE stream")
            eventSource.cancel()
        }
    }
}
