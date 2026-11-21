package com.redcode.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.redcode.app.data.local.PreferencesManager
import com.redcode.app.data.model.ChangelogRelease
import com.redcode.app.data.model.ExperimentalSettings
import com.redcode.app.data.model.ServerHealthState
import com.redcode.app.runtime.HealthMonitor
import com.redcode.app.runtime.OpenCodeServer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val health: ServerHealthState = ServerHealthState(),
    val serverHost: String = "127.0.0.1",
    val serverPort: Int = 49374,
    val selectedLanguage: String = "System Default",
    val experimentalSettings: ExperimentalSettings = ExperimentalSettings(),
    val changelogReleases: List<ChangelogRelease> = emptyList(),
    val storagePath: String = "",
    val isRestarting: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val healthMonitor: HealthMonitor,
    private val openCodeServer: OpenCodeServer,
    private val gson: Gson
) : ViewModel() {

    private val _isRestarting = MutableStateFlow(false)
    private val _changelogReleases = MutableStateFlow<List<ChangelogRelease>>(emptyList())

    val uiState: StateFlow<SettingsUiState> = combine(
        healthMonitor.healthState,
        preferencesManager.serverHost,
        preferencesManager.serverPort,
        preferencesManager.selectedLanguage,
        preferencesManager.experimentalSettings,
        _changelogReleases,
        _isRestarting
    ) { health, host, port, language, experimental, changelog, restarting ->
        SettingsUiState(
            health = health,
            serverHost = host,
            serverPort = port,
            selectedLanguage = language,
            experimentalSettings = experimental,
            changelogReleases = changelog,
            storagePath = "${context.filesDir.absolutePath}/ubuntu",
            isRestarting = restarting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(
            storagePath = "${context.filesDir.absolutePath}/ubuntu"
        )
    )

    init {
        loadChangelog()
    }

    private fun loadChangelog() {
        viewModelScope.launch {
            try {
                val json = context.assets.open("changelog.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<ChangelogRelease>>() {}.type
                val releases: List<ChangelogRelease> = gson.fromJson(json, type)
                _changelogReleases.value = releases
            } catch (_: Exception) {
                _changelogReleases.value = emptyList()
            }
        }
    }

    fun updateExperimentalSettings(settings: ExperimentalSettings) {
        viewModelScope.launch {
            preferencesManager.setExperimentalSettings(settings)
        }
    }

    fun toggleAllExperiments(enabled: Boolean) {
        viewModelScope.launch {
            val updated = ExperimentalSettings(
                allExperiments = enabled,
                hideUnusedModels = enabled,
                backgroundCallShortcut = enabled,
                sessionDelegator = enabled,
                shareScreenWithAgents = enabled,
                inboxSidebar = enabled,
                sessionLiveUpdates = enabled,
                soundEffects = enabled,
                openLinksByServerAddress = enabled,
                volumePercent = if (enabled) 100 else 50,
                sendSound = enabled,
                inAppBrowser = enabled
            )
            preferencesManager.setExperimentalSettings(updated)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferencesManager.setSelectedLanguage(language)
        }
    }

    fun restartServer() {
        viewModelScope.launch {
            _isRestarting.value = true
            openCodeServer.stop()
            openCodeServer.start()
            _isRestarting.value = false
        }
    }

    fun resetSetup(onResetDone: () -> Unit) {
        viewModelScope.launch {
            openCodeServer.stop()
            preferencesManager.setSetupCompleted(false)
            onResetDone()
        }
    }
}
