package com.redcode.app.data.model

import com.google.gson.annotations.SerializedName

data class ProjectFolder(
    val id: String,
    val name: String,
    val path: String,
    val isSelected: Boolean = false
)

data class SessionSummary(
    val id: String,
    val title: String,
    val project: String,
    val snippet: String,
    val timeAgo: String,
    val isUnread: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val updatedAt: Long = 0L
)

data class MessageItem(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = 0L,
    val tools: List<ToolInvocation> = emptyList(),
    val sources: List<SourceItem> = emptyList(),
    val isStreaming: Boolean = false
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ToolInvocation(
    val id: String,
    val name: String,
    val targetFile: String = "",
    val oldString: String? = null,
    val newString: String? = null,
    val path: String? = null,
    val result: String? = null,
    val isFailed: Boolean = false,
    val isExpanded: Boolean = false
)

data class SourceItem(
    val title: String,
    val domain: String,
    val url: String
)

data class ModelItem(
    val id: String,
    val name: String,
    val provider: String,
    val variant: String = "Default",
    val isFavorite: Boolean = false,
    val isRecent: Boolean = false,
    val isSelected: Boolean = false
)

data class SlashCommand(
    val command: String,
    val description: String
)

data class MentionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isFile: Boolean
)

data class ServerHealthState(
    val isConnected: Boolean = false,
    val latencyMs: Long = 0L,
    val address: String = "",
    val statusText: String = "Server Offline"
)

data class ExperimentalSettings(
    val allExperiments: Boolean = false,
    val hideUnusedModels: Boolean = false,
    val backgroundCallShortcut: Boolean = false,
    val sessionDelegator: Boolean = false,
    val shareScreenWithAgents: Boolean = false,
    val inboxSidebar: Boolean = false,
    val sessionLiveUpdates: Boolean = true,
    val soundEffects: Boolean = true,
    val openLinksByServerAddress: Boolean = false,
    val volumePercent: Int = 100,
    val sendSound: Boolean = false,
    val inAppBrowser: Boolean = true
)

enum class SetupStepStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    ERROR
}

data class SetupStepItem(
    val id: Int,
    val title: String,
    val status: SetupStepStatus = SetupStepStatus.PENDING,
    val progressPercent: Int = 0,
    val speedText: String = "",
    val etaText: String = "",
    val detail: String = ""
)

data class ChangelogRelease(
    val date: String,
    val version: String,
    val items: List<ChangelogItem>
)

data class ChangelogItem(
    val title: String,
    val description: String,
    val color: String
)
