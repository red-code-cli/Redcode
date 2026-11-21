package com.redcode.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.redcode.app.data.local.PreferencesManager
import com.redcode.app.data.model.SessionSummary
import com.redcode.app.data.repository.OpenCodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val repository: OpenCodeRepository
) : ViewModel() {

    val isSetupCompleted: StateFlow<Boolean> = preferencesManager.isSetupCompleted.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    val sessions: StateFlow<List<SessionSummary>> = repository.sessionsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        refreshSessions()
    }

    fun refreshSessions() {
        viewModelScope.launch {
            repository.syncSessions()
        }
    }
}
