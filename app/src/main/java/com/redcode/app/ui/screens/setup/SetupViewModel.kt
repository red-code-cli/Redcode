package com.redcode.app.ui.screens.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcode.app.data.local.PreferencesManager
import com.redcode.app.data.model.SetupStepItem
import com.redcode.app.data.model.SetupStepStatus
import com.redcode.app.runtime.InstallProgress
import com.redcode.app.runtime.OpenCodeServer
import com.redcode.app.runtime.ProotRunner
import com.redcode.app.runtime.UbuntuInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val ubuntuInstaller: UbuntuInstaller,
    private val openCodeServer: OpenCodeServer,
    private val preferencesManager: PreferencesManager,
    private val prootRunner: ProotRunner
) : ViewModel() {

    private val _steps = MutableStateFlow(
        listOf(
            SetupStepItem(0, "Downloading Ubuntu RootFS", SetupStepStatus.PENDING),
            SetupStepItem(1, "Extracting Runtime", SetupStepStatus.PENDING),
            SetupStepItem(2, "Installing OpenCode Engine", SetupStepStatus.PENDING),
            SetupStepItem(3, "Starting Server", SetupStepStatus.PENDING)
        )
    )
    val steps: StateFlow<List<SetupStepItem>> = _steps.asStateFlow()

    private val _currentSpeed = MutableStateFlow("")
    val currentSpeed: StateFlow<String> = _currentSpeed.asStateFlow()

    private val _currentEta = MutableStateFlow("")
    val currentEta: StateFlow<String> = _currentEta.asStateFlow()

    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress: StateFlow<Float> = _overallProgress.asStateFlow()

    val terminalLogs: StateFlow<List<String>> = openCodeServer.terminalLogs

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        startSetup()
    }

    fun startSetup() {
        viewModelScope.launch {
            _errorMessage.value = null

            // Verify PRoot runtime self-check before starting
            openCodeServer.appendLog("=== Verifying PRoot runtime binary ===")
            val prootCheck = prootRunner.verifyProot()
            if (prootCheck.isFailure) {
                val err = "PRoot self-check failed: ${prootCheck.exceptionOrNull()?.message}"
                openCodeServer.appendLog("ERROR: $err")
                _errorMessage.value = err
                updateStep(0, SetupStepStatus.ERROR)
                return@launch
            }
            openCodeServer.appendLog("PRoot self-check passed: ${prootCheck.getOrNull()?.lines()?.firstOrNull() ?: "OK"}")

            // If already installed, skip directly to server start
            if (ubuntuInstaller.isRootfsInstalled) {
                updateStep(0, SetupStepStatus.COMPLETED)
                updateStep(1, SetupStepStatus.COMPLETED)
                updateStep(2, SetupStepStatus.COMPLETED)
                startServerStep()
                return@launch
            }

            // Step 1 & 2: Install Ubuntu rootfs
            updateStep(0, SetupStepStatus.IN_PROGRESS)
            _overallProgress.value = 0.1f

            ubuntuInstaller.installUbuntu().collect { progress ->
                when (progress) {
                    is InstallProgress.Downloading -> {
                        updateStep(0, SetupStepStatus.IN_PROGRESS, progress.percent)
                        _currentSpeed.value = progress.speedText
                        _currentEta.value = progress.etaText
                        _overallProgress.value = (progress.percent * 0.4f) / 100f
                    }
                    is InstallProgress.Extracting -> {
                        updateStep(0, SetupStepStatus.COMPLETED)
                        updateStep(1, SetupStepStatus.IN_PROGRESS, progress.percent)
                        _overallProgress.value = 0.4f + (progress.percent * 0.3f) / 100f
                    }
                    is InstallProgress.InstallingDependencies -> {
                        updateStep(1, SetupStepStatus.COMPLETED)
                        updateStep(2, SetupStepStatus.IN_PROGRESS)
                        _overallProgress.value = 0.75f
                    }
                    is InstallProgress.StartingServer -> {
                        updateStep(2, SetupStepStatus.COMPLETED)
                        updateStep(3, SetupStepStatus.IN_PROGRESS)
                        _overallProgress.value = 0.9f
                    }
                    is InstallProgress.Success -> {
                        updateStep(0, SetupStepStatus.COMPLETED)
                        updateStep(1, SetupStepStatus.COMPLETED)

                        // Inspect RootFS structure and verify bash
                        openCodeServer.appendLog("=== Inspecting RootFS Structure ===")
                        ubuntuInstaller.inspectRootfs().forEach { line ->
                            openCodeServer.appendLog("[RootFS] $line")
                        }

                        // Run PRoot bash self-test
                        openCodeServer.appendLog("=== Verifying Ubuntu PRoot Shell ===")
                        var bashVerified = false
                        val testExit = prootRunner.executeScript("echo 'bash-ok'") { line ->
                            openCodeServer.appendLog("[proot] $line")
                            if (line.contains("bash-ok")) {
                                bashVerified = true
                            }
                        }

                        if (testExit == 0 && bashVerified) {
                            openCodeServer.appendLog("Ubuntu PRoot bash verification passed!")
                            installOpenCodeStep()
                        } else {
                            val err = "Ubuntu bash test failed (exit code $testExit). Incomplete rootfs."
                            openCodeServer.appendLog("ERROR: $err")
                            File(ubuntuInstaller.ubuntuDir, UbuntuInstaller.MARKER_FILE_NAME).delete()
                            _errorMessage.value = err
                            updateStep(1, SetupStepStatus.ERROR)
                        }
                    }
                    is InstallProgress.Error -> {
                        _errorMessage.value = progress.error
                        updateStep(0, SetupStepStatus.ERROR)
                    }
                }
            }
        }
    }

    private fun installOpenCodeStep() {
        viewModelScope.launch {
            updateStep(2, SetupStepStatus.IN_PROGRESS)
            _overallProgress.value = 0.75f

            val success = openCodeServer.installOpenCode { log ->
                openCodeServer.appendLog(log)
            }

            if (success) {
                updateStep(2, SetupStepStatus.COMPLETED)
                startServerStep()
            } else {
                updateStep(2, SetupStepStatus.ERROR)
                _errorMessage.value = "Failed to install OpenCode engine"
            }
        }
    }

    private fun startServerStep() {
        viewModelScope.launch {
            updateStep(3, SetupStepStatus.IN_PROGRESS)
            _overallProgress.value = 0.9f

            openCodeServer.startServer()
            updateStep(3, SetupStepStatus.COMPLETED)
            _overallProgress.value = 1f
            preferencesManager.setSetupCompleted(true)
            _isComplete.value = true
        }
    }

    private fun updateStep(id: Int, status: SetupStepStatus, progress: Int = 0) {
        val current = _steps.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(
                status = status,
                progressPercent = progress
            )
            _steps.value = current
        }
    }
}
