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
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
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
        if (current.size > 800) {
            current.removeAt(0)
        }
        _terminalLogs.value = current
    }

    suspend fun isOpenCodeInstalled(): Boolean = withContext(Dispatchers.IO) {
        var output = ""
        val code = prootRunner.executeScript("command -v opencode >/dev/null 2>&1 && opencode --version") {
            output += "$it\n"
        }
        val isInstalled = code == 0 && (output.contains("opencode") || output.contains("1.") || output.contains("2."))
        Log.d(TAG, "isOpenCodeInstalled: $isInstalled (code=$code, output=$output)")
        isInstalled
    }

    suspend fun installDependencies(onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        onLog("=== [1/4] Installing System Dependencies (apt) ===")
        val script = """
            export DEBIAN_FRONTEND=noninteractive
            SUCCESS=0
            for i in 1 2 3; do
                echo "[apt] Attempt ${'$'}i of 3: updating package index and installing tools..."
                if apt-get update -y && apt-get install -y --no-install-recommends \
                    curl ca-certificates git unzip xz-utils tar bzip2; then
                    SUCCESS=1
                    break
                else
                    echo "[apt] Attempt ${'$'}i failed, retrying in 3 seconds..."
                    sleep 3
                fi
            done
            update-ca-certificates || true
            exit ${'$'}( [ ${'$'}SUCCESS -eq 1 ] && echo 0 || echo 1 )
        """.trimIndent()

        val exitCode = prootRunner.executeScript(script) {
            onLog(it)
            appendLog(it)
        }
        exitCode == 0
    }

    suspend fun installNode(onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        onLog("=== [2/4] Installing Official Node.js LTS (v20 arm64) ===")

        val script = """
            # Check existing Node.js
            if command -v node >/dev/null 2>&1; then
                CURRENT_NODE=${'$'}(node -v 2>/dev/null || echo "")
                echo "Existing node detected: ${'$'}CURRENT_NODE"
                if [[ "${'$'}CURRENT_NODE" =~ ^v(18|20|22|24)\. ]]; then
                    echo "Node.js ${'$'}CURRENT_NODE is adequate, skipping re-install."
                    node -v
                    npm -v
                    exit 0
                fi
            fi

            echo "Downloading official Node.js v20.18.0 linux-arm64 tarball..."
            NODE_URL="https://nodejs.org/dist/v20.18.0/node-v20.18.0-linux-arm64.tar.xz"
            if curl -fsSL "${'$'}NODE_URL" | tar -xJ -C /usr/local --strip-components=1; then
                echo "Node.js v20 tarball installed to /usr/local successfully!"
            else
                echo "Tarball install failed, falling back to NodeSource repository..."
                curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
                DEBIAN_FRONTEND=noninteractive apt-get install -y nodejs
            fi

            echo "Verifying Node.js and NPM..."
            if command -v node >/dev/null 2>&1 && command -v npm >/dev/null 2>&1; then
                echo "Node: ${'$'}(node -v)"
                echo "NPM: ${'$'}(npm -v)"
                exit 0
            else
                echo "ERROR: Failed to install Node.js and NPM"
                exit 1
            fi
        """.trimIndent()

        val exitCode = prootRunner.executeScript(script) {
            onLog(it)
            appendLog(it)
        }
        exitCode == 0
    }

    suspend fun installOpenCodeEngine(onLog: (String) -> Unit): Boolean = withContext(Dispatchers.IO) {
        onLog("=== [3/4] Installing OpenCode Engine (Multi-Method Fallback) ===")

        val script = """
            # Configure NPM resilience
            export HOME=/root
            npm config set prefix /usr/local
            npm config set fetch-retries 5
            npm config set fetch-retry-mintimeout 20000
            npm config set fetch-retry-maxtimeout 120000

            INSTALLED=0

            # METHOD 1: npm install -g opencode-ai
            echo "--- Method 1: npm install -g opencode-ai ---"
            if npm install -g opencode-ai --registry=https://registry.npmjs.org/; then
                if command -v opencode >/dev/null 2>&1 && opencode --version >/dev/null 2>&1; then
                    echo "Method 1 succeeded: ${'$'}(opencode --version)"
                    INSTALLED=1
                fi
            fi

            # METHOD 2: Official installer script (v2/install or install)
            if [ ${'$'}INSTALLED -eq 0 ]; then
                echo "--- Method 2: Official install script (v2/install) ---"
                if curl -fsSL https://opencode.ai/v2/install | bash || curl -fsSL https://opencode.ai/install | bash; then
                    if [ -f /root/.opencode/bin/opencode ]; then
                        ln -sf /root/.opencode/bin/opencode /usr/local/bin/opencode
                    fi
                    if command -v opencode >/dev/null 2>&1 && opencode --version >/dev/null 2>&1; then
                        echo "Method 2 succeeded: ${'$'}(opencode --version)"
                        INSTALLED=1
                    fi
                fi
            fi

            # METHOD 3: Direct prebuilt binary from npm package or GitHub releases
            if [ ${'$'}INSTALLED -eq 0 ]; then
                echo "--- Method 3: Direct prebuilt binary download ---"
                DL_DIR="/tmp/opencode_dl_${'$'}${'$'}"
                mkdir -p "${'$'}DL_DIR" && cd "${'$'}DL_DIR"

                # Try downloading @opencode/cli-linux-arm64
                if curl -fsSL "https://registry.npmjs.org/@opencode/cli-linux-arm64/-/cli-linux-arm64-2.3.2.tgz" | tar -xz; then
                    if [ -f package/bin/opencode ]; then
                        mv package/bin/opencode /usr/local/bin/opencode
                        chmod 755 /usr/local/bin/opencode
                        if command -v opencode >/dev/null 2>&1 && opencode --version >/dev/null 2>&1; then
                            echo "Method 3 succeeded via npm tarball: ${'$'}(opencode --version)"
                            INSTALLED=1
                        fi
                    fi
                fi

                # Try GitHub release binary if still not installed
                if [ ${'$'}INSTALLED -eq 0 ]; then
                    GH_URL="https://github.com/anomalyco/opencode/releases/download/v1.19.6/opencode-linux-arm64.tar.gz"
                    if curl -fsSL "${'$'}GH_URL" | tar -xz -C /usr/local/bin/; then
                        chmod 755 /usr/local/bin/opencode || true
                        if command -v opencode >/dev/null 2>&1 && opencode --version >/dev/null 2>&1; then
                            echo "Method 3 succeeded via GitHub release: ${'$'}(opencode --version)"
                            INSTALLED=1
                        fi
                    fi
                fi
                rm -rf "${'$'}DL_DIR"
            fi

            # Final verify & fallback to JS wrapper if binary throws SIGSYS/illegal instruction
            if [ ${'$'}INSTALLED -eq 1 ] || ( command -v opencode >/dev/null 2>&1 && opencode --version >/dev/null 2>&1 ); then
                echo "OpenCode engine installed successfully!"
                exit 0
            else
                # Check if Node.js JS module exists
                JS_BIN="/usr/local/lib/node_modules/opencode-ai/bin/opencode.js"
                if [ -f "${'$'}JS_BIN" ]; then
                    echo "Binary execution failed, creating Node.js JS wrapper..."
                    cat << 'EOF' > /usr/local/bin/opencode
#!/bin/sh
exec node /usr/local/lib/node_modules/opencode-ai/bin/opencode.js "$@"
EOF
                    chmod 755 /usr/local/bin/opencode
                    if opencode --version; then
                        echo "Node.js JS wrapper succeeded: ${'$'}(opencode --version)"
                        exit 0
                    fi
                fi

                echo "ERROR: All OpenCode installation methods failed."
                exit 1
            fi
        """.trimIndent()

        val exitCode = prootRunner.executeScript(script) {
            onLog(it)
            appendLog(it)
        }
        exitCode == 0
    }

    suspend fun verifyOpenCode(onLog: (String) -> Unit): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        onLog("=== [4/4] Verifying OpenCode Version ===")
        var verOutput = ""
        val exitCode = prootRunner.executeScript("opencode --version || opencode -v") { line ->
            onLog(line)
            appendLog(line)
            verOutput += "$line\n"
        }
        val isSuccess = exitCode == 0 && (verOutput.contains("opencode") || verOutput.contains("1.") || verOutput.contains("2."))
        Pair(isSuccess, verOutput.trim())
    }

    suspend fun cleanOpenCode(): Boolean = withContext(Dispatchers.IO) {
        appendLog("=== Cleaning OpenCode Installation ===")
        val script = "rm -rf /usr/local/bin/opencode /root/.opencode /usr/local/lib/node_modules/opencode-ai /usr/local/lib/node_modules/@opencode"
        prootRunner.executeScript(script) { appendLog(it) }
        true
    }

    suspend fun startServerAndWaitHealth(timeoutSeconds: Int = 15): Boolean = withContext(Dispatchers.IO) {
        isUserStopped = false
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
            "/bin/bash", "-lc",
            "export OPENCODE_SERVER_PASSWORD=\"$password\" && export OPENCODE_PASSWORD=\"$password\" && opencode serve --hostname $host --port $port"
        )
        val extraEnv = mapOf(
            "OPENCODE_SERVER_PASSWORD" to password,
            "OPENCODE_PASSWORD" to password
        )

        try {
            serverProcess = prootRunner.startProcess(cmd, extraEnv)

            // Start log consumer in background
            scope.launch {
                try {
                    val reader = BufferedReader(InputStreamReader(serverProcess!!.inputStream))
                    var line = reader.readLine()
                    while (line != null) {
                        appendLog(line)
                        line = reader.readLine()
                    }
                    val exitCode = serverProcess?.waitFor() ?: -1
                    appendLog("OpenCode server exited with code $exitCode")
                    if (!isUserStopped) {
                        _serverState.value = ServerState.Error("Server terminated ($exitCode)")
                        handleAutoRestart()
                    } else {
                        _serverState.value = ServerState.Stopped
                    }
                } catch (e: Exception) {
                    appendLog("Server stream error: ${e.message}")
                }
            }

            // Health poll loop
            val startTime = System.currentTimeMillis()
            var healthy = false
            while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000L) {
                delay(1000L)
                if (checkHttpHealth(host, port)) {
                    healthy = true
                    break
                }
            }

            if (healthy) {
                appendLog("OpenCode server health check passed on $host:$port!")
                _serverState.value = ServerState.Running(port, host)
                restartAttempts = 0
                true
            } else {
                appendLog("OpenCode server health check timed out after ${timeoutSeconds}s.")
                _serverState.value = ServerState.Error("Server failed to respond on $host:$port")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            appendLog("Error: ${e.message}")
            _serverState.value = ServerState.Error(e.message ?: "Unknown start error")
            false
        }
    }

    fun startServer() {
        scope.launch {
            if (_serverState.value is ServerState.Running) return@launch
            startServerAndWaitHealth()
        }
    }

    fun stopServer() {
        isUserStopped = true
        serverProcess?.destroy()
        serverProcess = null
        _serverState.value = ServerState.Stopped
        appendLog("Server stopped by user.")
    }

    private suspend fun checkHttpHealth(host: String, port: Int): Boolean {
        return try {
            val password = preferencesManager.serverPassword.firstOrNull().orEmpty()
            val url = URL("http://$host:$port/api/info")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.requestMethod = "GET"
            if (password.isNotBlank()) {
                val credentials = "opencode:$password"
                val basicAuth = "Basic " + android.util.Base64.encodeToString(credentials.toByteArray(), android.util.Base64.NO_WRAP)
                conn.setRequestProperty("Authorization", basicAuth)
            }
            val code = conn.responseCode
            conn.disconnect()
            code in 200..499 // Any HTTP response (including 401 Unauthorized) proves server daemon is alive!
        } catch (_: Exception) {
            false
        }
    }

    private fun handleAutoRestart() {
        if (restartAttempts < 5) {
            val backoffMs = (1000L * (1 shl restartAttempts)).coerceAtMost(30000L)
            restartAttempts++
            appendLog("Auto-restarting server in ${backoffMs / 1000}s (attempt $restartAttempts/5)...")
            scope.launch {
                delay(backoffMs)
                if (!isUserStopped) {
                    startServerAndWaitHealth()
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
