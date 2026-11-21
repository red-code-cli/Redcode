package com.redcode.app.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcode.app.data.local.PreferencesManager
import com.redcode.app.data.model.MentionItem
import com.redcode.app.data.model.MessageItem
import com.redcode.app.data.model.ModelItem
import com.redcode.app.data.model.SessionSummary
import com.redcode.app.data.model.SlashCommand
import com.redcode.app.data.remote.OpenCodeSseClient
import com.redcode.app.data.remote.SseEvent
import com.redcode.app.data.repository.OpenCodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: OpenCodeRepository,
    private val preferencesManager: PreferencesManager,
    private val sseClient: OpenCodeSseClient,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    val messages: StateFlow<List<MessageItem>> = repository.getMessagesForSession(sessionId).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _session = MutableStateFlow<SessionSummary?>(null)
    val session: StateFlow<SessionSummary?> = _session.asStateFlow()

    val isSessionMuted: StateFlow<Boolean> = repository.isSessionMuted(sessionId).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    private val _isAgentBusy = MutableStateFlow(false)
    val isAgentBusy: StateFlow<Boolean> = _isAgentBusy.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    val selectedModel: StateFlow<String> = preferencesManager.selectedModel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val selectedMode: StateFlow<String> = preferencesManager.selectedMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Build"
    )

    private val _models = MutableStateFlow<List<ModelItem>>(emptyList())
    val models: StateFlow<List<ModelItem>> = _models.asStateFlow()

    private val _slashCommands = MutableStateFlow<List<SlashCommand>>(emptyList())
    val slashCommands: StateFlow<List<SlashCommand>> = _slashCommands.asStateFlow()

    private val _mentionFiles = MutableStateFlow<List<MentionItem>>(emptyList())
    val mentionFiles: StateFlow<List<MentionItem>> = _mentionFiles.asStateFlow()

    init {
        loadSessionData()
        subscribeToSse()
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            repository.sessionsFlow.collect { list ->
                _session.value = list.firstOrNull { it.id == sessionId }
            }
        }
        viewModelScope.launch {
            repository.refreshMessages(sessionId)
            repository.getAvailableModels().onSuccess { _models.value = it }
            repository.getSlashCommands().onSuccess { _slashCommands.value = it }
        }
    }

    private fun subscribeToSse() {
        viewModelScope.launch {
            val port = preferencesManager.serverPort.firstOrNull() ?: 49374
            val host = preferencesManager.serverHost.firstOrNull() ?: "127.0.0.1"
            val password = preferencesManager.serverPassword.firstOrNull().orEmpty()
            val url = "http://$host:$port/api/event"

            try {
                sseClient.observeEventStream(url, password).collect { event ->
                    when (event) {
                        is SseEvent.Message -> {
                            if (event.type?.contains("message") == true || event.type?.contains("session") == true) {
                                repository.refreshMessages(sessionId)
                            }
                            if (event.type?.contains("busy") == true || event.type?.contains("start") == true) {
                                _isAgentBusy.value = true
                            } else if (event.type?.contains("idle") == true || event.type?.contains("completed") == true) {
                                _isAgentBusy.value = false
                            }
                        }
                        is SseEvent.Failure -> {
                            _isAgentBusy.value = false
                        }
                        else -> {}
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
        if (text.contains("@")) {
            val query = text.substringAfterLast("@")
            searchFiles(query)
        } else {
            _mentionFiles.value = emptyList()
        }
    }

    fun searchFiles(query: String) {
        viewModelScope.launch {
            repository.searchFiles(query).onSuccess { _mentionFiles.value = it }
        }
    }

    fun sendMessage() {
        val text = _inputText.value
        if (text.isBlank()) return
        _inputText.value = ""
        _isAgentBusy.value = true

        viewModelScope.launch {
            repository.sendMessage(sessionId, text).onFailure {
                _isAgentBusy.value = false
            }
        }
    }

    fun selectModel(model: ModelItem) {
        viewModelScope.launch {
            preferencesManager.setSelectedModel(model.name)
            preferencesManager.addRecentModel(model.id)
        }
    }

    fun selectMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.setSelectedMode(mode)
        }
    }

    fun togglePin() {
        val s = _session.value ?: return
        viewModelScope.launch {
            repository.togglePinSession(s.id, s.isPinned)
        }
    }

    fun renameSession(newTitle: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.renameSession(sessionId, newTitle)
            onComplete()
        }
    }

    fun markAsUnread(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.markSessionAsUnread(sessionId, true)
            onComplete()
        }
    }

    fun toggleMute(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isMuted = repository.toggleSessionMute(sessionId)
            onResult(isMuted)
        }
    }

    fun compactSession(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.compactSession(sessionId).fold(
                onSuccess = { onResult(true, "Session compacted successfully") },
                onFailure = { onResult(false, it.message ?: "Failed to compact session") }
            )
        }
    }

    fun forkSession(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            repository.forkSession(sessionId).fold(
                onSuccess = { onResult(it.id) },
                onFailure = { onResult(null) }
            )
        }
    }

    fun exportSession(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            repository.exportSession(sessionId).fold(
                onSuccess = { onResult(it) },
                onFailure = { onResult(null) }
            )
        }
    }

    fun deleteSession(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            onComplete()
        }
    }
}
