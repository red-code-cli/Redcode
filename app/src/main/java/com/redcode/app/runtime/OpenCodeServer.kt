package com.redcode.app.runtime

import android.util.Log
import com.redcode.app.BuildConfig
import com.redcode.app.data.local.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

sealed class ServerState {
    object Stopped : ServerState()
    object Starting : ServerState()
    data class Running(val port: Int, val host: String) : ServerState()
    data class Error(val message: String) : ServerState()
}

@Singleton
class OpenCodeServer @Inject constructor(
    private val prootRunner: ProotRunner,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "OpenCodeServer"
        const val TARGET_VERSION = BuildConfig.OPENCODE_VERSION
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverProcess: Process? = null
    private var restartAttempts = 0
    private var isUserStopped = false

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(emptyList())
    val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    fun appendLog(line: String) {
        val current = _terminalLogs.value.toMutableList()
        current.add(line)
        if (current.size > 500) {
            current.removeAt(0)
        }
        _terminalLogs.value = current
    }

    suspend fun installOpenCode(onLog: (String) -> Unit): Boolean {
        onLog("=== Updating Ubuntu Package Index ===")
        val updateCmd = "apt-get update -y && apt-get install -y --no-install-recommends curl git ca-certificates unzip nodejs npm"
        val code1 = prootRunner.executeScript(updateCmd) {
            onLog(it)
            appendLog(it)
        }
        if (code1 != 0) {
            onLog("Warning: apt install returned $code1, continuing to check npm...")
        }

        onLog("=== Installing OpenCode v$TARGET_VERSION via NPM ===")
        val installCmd = "npm install -g @opencode/cli@$TARGET_VERSION || npm install -g opencode-ai"
        val code2 = prootRunner.executeScript(installCmd) {
            onLog(it)
            appendLog(it)
        }

        onLog("=== Verifying OpenCode Version ===")
        val verifyCmd = "opencode --version || opencode -v"
        var verified = false
        prootRunner.executeScript(verifyCmd) { line ->
            onLog(line)
            appendLog(line)
            if (line.contains("2.") || line.contains("opencode") || line.contains("1.")) {
                verified = true
            }
        }
        return verified || code2 == 0
    }

    fun startServer() {
        isUserStopped = false
        scope.launch {
            if (_serverState.value is ServerState.Running) return@launch
            _serverState.value = ServerState.Starting

            val port = preferencesManager.serverPort.firstOrNull() ?: 49374
            val host = preferencesManager.serverHost.firstOrNull() ?: "127.0.0.1"
            var password = preferencesManager.serverPassword.firstOrNull().orEmpty()
            if (password.isBlank()) {
                password = generateRandomPassword()
                preferencesManager.setServerCredentials(port, host, password)
            }

            appendLog("Starting OpenCode server on $host:$port...")

            val cmd = listOf(
                "/bin/bash", "-c",
                "export OPENCODE_SERVER_PASSWORD=\"$password\" && opencode serve --hostname $host --port $port"
            )
            val extraEnv = mapOf("OPENCODE_SERVER_PASSWORD" to password)

            try {
                serverProcess = prootRunner.startProcess(cmd, extraEnv)
                _serverState.value = ServerState.Running(port, host)
                restartAttempts = 0

                val reader = BufferedReader(InputStreamReader(serverProcess!!.inputStream))
                var line = reader.readLine()
                while (line != null) {
                    appendLog(line)
                    line = reader.readLine()
                }

                val exitCode = serverProcess?.waitFor() ?: -1
                appendLog("OpenCode server exited with code $exitCode")

                if (!isUserStopped) {
                    _serverState.value = ServerState.Error("Server process terminated ($exitCode)")
                    handleAutoRestart()
                } else {
                    _serverState.value = ServerState.Stopped
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server", e)
                appendLog("Error: ${e.message}")
                _serverState.value = ServerState.Error(e.message ?: "Unknown start error")
                if (!isUserStopped) handleAutoRestart()
            }
        }
    }

    fun stopServer() {
        isUserStopped = true
        serverProcess?.destroy()
        serverProcess = null
        _serverState.value = ServerState.Stopped
        appendLog("Server stopped by user.")
    }

    private fun handleAutoRestart() {
        if (restartAttempts < 5) {
            val backoffMs = (1000L * (1 shl restartAttempts)).coerceAtMost(30000L)
            restartAttempts++
            appendLog("Auto-restarting server in ${backoffMs / 1000}s (attempt $restartAttempts/5)...")
            scope.launch {
                delay(backoffMs)
                if (!isUserStopped) {
                    startServer()
                }
            }
        } else {
            appendLog("Maximum restart attempts reached. Please check logs.")
        }
    }

    private fun generateRandomPassword(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        return (1..24).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }
}
