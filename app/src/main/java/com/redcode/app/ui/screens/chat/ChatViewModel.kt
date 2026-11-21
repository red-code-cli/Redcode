package com.redcode.app.ui.screens.chat

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcode.app.data.local.PreferencesManager
import com.redcode.app.data.model.MentionItem
import com.redcode.app.data.model.MessageItem
import com.redcode.app.data.model.MessageRole
import com.redcode.app.data.model.ModelItem
import com.redcode.app.data.model.SessionSummary
import com.redcode.app.data.model.SlashCommand
import com.redcode.app.data.remote.OpenCodeSseClient
import com.redcode.app.data.remote.SseEvent
import com.redcode.app.data.repository.OpenCodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
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

    private var sseJob: Job? = null
    private var pollingJob: Job? = null

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
        sseJob?.cancel()
        sseJob = viewModelScope.launch {
            var backoffMs = 1000L
            while (isActive) {
                val port = preferencesManager.serverPort.firstOrNull() ?: 49374
                val host = preferencesManager.serverHost.firstOrNull() ?: "127.0.0.1"
                val password = preferencesManager.serverPassword.firstOrNull().orEmpty()
                val url = "http://$host:$port/api/event"

                try {
                    Log.d("ChatViewModel", "Connecting to SSE: $url")
                    sseClient.observeEventStream(url, password).collect { event ->
                        when (event) {
                            is SseEvent.Open -> {
                                Log.d("ChatViewModel", "SSE Connected successfully")
                                backoffMs = 1000L
                                repository.refreshMessages(sessionId)
                            }
                            is SseEvent.Message -> {
                                handleSseMessage(event.type, event.data)
                            }
                            is SseEvent.Failure -> {
                                Log.w("ChatViewModel", "SSE Failure: ${event.throwable?.message}")
                            }
                            is SseEvent.Closed -> {
                                Log.d("ChatViewModel", "SSE Closed, will retry")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "SSE connection loop error: ${e.message}")
                }

                delay(backoffMs)
                backoffMs = (backoffMs * 2).coerceAtMost(15000L)
            }
        }
    }

    private fun handleSseMessage(type: String?, rawData: String) {
        if (type.isNullOrBlank()) return
        val json = try { JSONObject(rawData) } catch (_: Exception) { return }
        val dataObj = json.optJSONObject("data")

        val eventSessionId = dataObj?.optString("sessionID")
        if (!eventSessionId.isNullOrBlank() && eventSessionId != sessionId) {
            return
        }

        when (type) {
            "server.connected" -> {
                Log.d("ChatViewModel", "Server confirmed SSE stream connection")
            }
            "session.text.started", "session.execution.started", "session.step.started" -> {
                _isAgentBusy.value = true
            }
            "session.text.delta" -> {
                _isAgentBusy.value = true
                val delta = dataObj?.optString("delta").orEmpty()
                val msgId = dataObj?.optString("assistantMessageID").orEmpty()
                if (delta.isNotEmpty() && msgId.isNotEmpty()) {
                    viewModelScope.launch {
                        repository.appendStreamingDelta(sessionId, msgId, delta)
                    }
                }
            }
            "session.text.ended" -> {
                val finalText = dataObj?.optString("text").orEmpty()
                val msgId = dataObj?.optString("assistantMessageID").orEmpty()
                if (msgId.isNotEmpty()) {
                    viewModelScope.launch {
                        repository.finalizeAssistantMessage(sessionId, msgId, finalText)
                    }
                }
            }
            "session.tool.called", "session.tool.progress", "session.tool.success", "session.tool.failed" -> {
                viewModelScope.launch {
                    repository.refreshMessages(sessionId)
                }
            }
            "session.idle", "session.execution.succeeded" -> {
                _isAgentBusy.value = false
                viewModelScope.launch {
                    repository.refreshMessages(sessionId)
                }
            }
            "session.execution.failed" -> {
                _isAgentBusy.value = false
                val errObj = dataObj?.optJSONObject("error")
                val errMsg = errObj?.optString("message") ?: "Execution failed"
                viewModelScope.launch {
                    repository.recordErrorMessage(sessionId, errMsg)
                    repository.refreshMessages(sessionId)
                }
            }
            else -> {
                if (type.contains("message") || type.contains("session")) {
                    viewModelScope.launch {
                        repository.refreshMessages(sessionId)
                    }
                }
            }
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
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        _inputText.value = ""
        _isAgentBusy.value = true

        viewModelScope.launch {
            // 1. Sync session model and agent before sending prompt
            val selectedModelName = preferencesManager.selectedModel.firstOrNull().orEmpty()
            val selectedModeName = preferencesManager.selectedMode.firstOrNull() ?: "Build"
            val modelItem = _models.value.find { it.name == selectedModelName || it.id == selectedModelName }
                ?: _models.value.firstOrNull()

            if (modelItem != null) {
                repository.setSessionModel(sessionId, modelItem.id, modelItem.provider)
            }
            repository.setSessionAgent(sessionId, selectedModeName.lowercase())

            // 2. Send the message
            val sendResult = repository.sendMessage(sessionId, text)
            if (sendResult.isFailure) {
                _isAgentBusy.value = false
                val err = sendResult.exceptionOrNull()?.message ?: "Failed to send message"
                repository.recordErrorMessage(sessionId, err)
                return@launch
            }

            // 3. Fallback: 1-second polling loop to ensure assistant reply is always captured
            pollingJob?.cancel()
            pollingJob = launch {
                var attempts = 0
                while (attempts < 60 && _isAgentBusy.value) {
                    delay(1000L)
                    attempts++
                    val res = repository.refreshMessages(sessionId)
                    val msgs = res.getOrNull()
                    val lastMsg = msgs?.lastOrNull()
                    if (lastMsg != null && lastMsg.role != MessageRole.USER) {
                        if (lastMsg.content.isNotBlank() || lastMsg.tools.isNotEmpty() || lastMsg.role == MessageRole.ERROR) {
                            _isAgentBusy.value = false
                            break
                        }
                    }
                }
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
