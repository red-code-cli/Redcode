package com.redcode.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcode.app.data.local.PreferencesManager
import com.redcode.app.data.model.MentionItem
import com.redcode.app.data.model.ModelItem
import com.redcode.app.data.model.ProjectFolder
import com.redcode.app.data.model.SlashCommand
import com.redcode.app.data.repository.OpenCodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: OpenCodeRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _projects = MutableStateFlow<List<ProjectFolder>>(emptyList())
    val projects: StateFlow<List<ProjectFolder>> = _projects.asStateFlow()

    private val _isLoadingProjects = MutableStateFlow(false)
    val isLoadingProjects: StateFlow<Boolean> = _isLoadingProjects.asStateFlow()

    val selectedProjectName: StateFlow<String> = preferencesManager.selectedProjectName.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val selectedModel: StateFlow<String> = preferencesManager.selectedModel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), ""
    )

    val selectedMode: StateFlow<String> = preferencesManager.selectedMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "Build"
    )

    val hasUnreadSessions: StateFlow<Boolean> = repository.sessionsFlow.map { list ->
        list.any { it.isUnread }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _models = MutableStateFlow<List<ModelItem>>(emptyList())
    val models: StateFlow<List<ModelItem>> = _models.asStateFlow()

    private val _slashCommands = MutableStateFlow<List<SlashCommand>>(emptyList())
    val slashCommands: StateFlow<List<SlashCommand>> = _slashCommands.asStateFlow()

    private val _mentionFiles = MutableStateFlow<List<MentionItem>>(emptyList())
    val mentionFiles: StateFlow<List<MentionItem>> = _mentionFiles.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoadingProjects.value = true
            repository.refreshProjects().onSuccess {
                _projects.value = it
            }
            _isLoadingProjects.value = false

            repository.getAvailableModels().onSuccess {
                _models.value = it
            }

            repository.getSlashCommands().onSuccess {
                _slashCommands.value = it
            }
        }
    }

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
        if (newText.contains("@")) {
            val query = newText.substringAfterLast("@")
            searchFiles(query)
        } else {
            _mentionFiles.value = emptyList()
        }
    }

    fun selectProject(project: ProjectFolder) {
        viewModelScope.launch {
            preferencesManager.setSelectedProject(project.id, project.name)
            _projects.value = _projects.value.map {
                it.copy(isSelected = it.id == project.id)
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

    fun searchFiles(query: String) {
        viewModelScope.launch {
            repository.searchFiles(query).onSuccess {
                _mentionFiles.value = it
            }
        }
    }

    fun startSession(onSuccess: (String) -> Unit) {
        val text = _inputText.value
        if (text.isBlank()) return

        viewModelScope.launch {
            val title = if (text.length > 30) "${text.take(30)}..." else text
            val projectId = preferencesManager.selectedProjectId.stateIn(viewModelScope).value
            repository.createNewSession(title, projectId).onSuccess { session ->
                repository.sendMessage(session.id, text)
                _inputText.value = ""
                onSuccess(session.id)
            }
        }
    }
}
