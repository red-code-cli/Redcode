package com.redcode.app.runtime

import com.redcode.app.data.local.PreferencesManager
import com.redcode.app.data.model.ServerHealthState
import com.redcode.app.data.remote.OpenCodeApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthMonitor @Inject constructor(
    private val apiService: OpenCodeApiService,
    private val preferencesManager: PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var monitorJob: Job? = null

    private val _healthState = MutableStateFlow(
        ServerHealthState(
            isConnected = false,
            latencyMs = 0L,
            address = "",
            statusText = "Checking..."
        )
    )
    val healthState: StateFlow<ServerHealthState> = _healthState.asStateFlow()

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                checkHealth()
                delay(2000L)
            }
        }
    }

    fun checkHealthNow() {
        scope.launch {
            checkHealth()
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    private suspend fun checkHealth() {
        val port = preferencesManager.serverPort.firstOrNull() ?: 49374
        val host = preferencesManager.serverHost.firstOrNull() ?: "127.0.0.1"
        val addr = "$host:$port · Loopback"

        val startTime = System.currentTimeMillis()
        try {
            val response = apiService.getServerInfo()
            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            if (response.isSuccessful && response.body() != null) {
                val version = response.body()?.version?.let { "v$it" } ?: ""
                _healthState.value = ServerHealthState(
                    isConnected = true,
                    latencyMs = latency,
                    address = addr,
                    statusText = if (version.isNotBlank()) "Online ($version)" else "Online"
                )
            } else {
                _healthState.value = ServerHealthState(
                    isConnected = false,
                    latencyMs = 0L,
                    address = addr,
                    statusText = "Server Offline"
                )
            }
        } catch (_: Exception) {
            _healthState.value = ServerHealthState(
                isConnected = false,
                latencyMs = 0L,
                address = addr,
                statusText = "Server Offline"
            )
        }
    }
}
