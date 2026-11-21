package com.redcode.app.data.repository

import com.google.gson.Gson
import com.redcode.app.data.local.PreferencesManager
import com.redcode.app.data.local.dao.MessageDao
import com.redcode.app.data.local.dao.SessionDao
import com.redcode.app.data.local.entity.MessageEntity
import com.redcode.app.data.local.entity.SessionEntity
import com.redcode.app.data.model.MentionItem
import com.redcode.app.data.model.MessageItem
import com.redcode.app.data.model.MessageRole
import com.redcode.app.data.model.ModelItem
import com.redcode.app.data.model.ProjectFolder
import com.redcode.app.data.model.SessionSummary
import com.redcode.app.data.model.SlashCommand
import com.redcode.app.data.model.ToolInvocation
import com.redcode.app.data.remote.ApiCreateSessionRequest
import com.redcode.app.data.remote.ApiPromptRequest
import com.redcode.app.data.remote.ApiRenameSessionRequest
import com.redcode.app.data.remote.OpenCodeApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenCodeRepository @Inject constructor(
    private val apiService: OpenCodeApiService,
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val preferencesManager: PreferencesManager,
    private val gson: Gson
) {

    val sessionsFlow: Flow<List<SessionSummary>> = sessionDao.getAllSessions().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getMessagesForSession(sessionId: String): Flow<List<MessageItem>> =
        messageDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { it.toDomain(gson) }
        }

    fun isSessionMuted(sessionId: String): Flow<Boolean> =
        preferencesManager.mutedSessions.map { it.contains(sessionId) }

    suspend fun toggleSessionMute(sessionId: String): Boolean =
        preferencesManager.toggleSessionMute(sessionId)

    suspend fun refreshProjects(): Result<List<ProjectFolder>> {
        return try {
            val response = apiService.getProjects()
            if (response.isSuccessful && response.body() != null) {
                val selectedId = preferencesManager.selectedProjectId.firstOrNull().orEmpty()
                val projects = response.body()!!.map { apiProject ->
                    val displayName = apiProject.name?.ifBlank { null }
                        ?: File(apiProject.canonical).name.ifBlank { apiProject.canonical }
                    ProjectFolder(
                        id = apiProject.id,
                        name = displayName,
                        path = apiProject.canonical,
                        isSelected = apiProject.id == selectedId || (selectedId.isEmpty() && response.body()!!.firstOrNull()?.id == apiProject.id)
                    )
                }
                if (selectedId.isEmpty() && projects.isNotEmpty()) {
                    preferencesManager.setSelectedProject(projects.first().id, projects.first().name)
                }
                Result.success(projects)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshSessions(): Result<List<SessionSummary>> {
        return try {
            val response = apiService.getSessions()
            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.data.map { apiSession ->
                    val timeMillis = apiSession.time?.updated ?: apiSession.time?.created ?: 0L
                    val timeAgoStr = formatTimeAgo(timeMillis)
                    SessionEntity(
                        id = apiSession.id,
                        title = apiSession.title?.ifBlank { "Untitled Session" } ?: "Untitled Session",
                        project = apiSession.projectID ?: "",
                        snippet = "",
                        timeAgo = timeAgoStr,
                        isUnread = false,
                        updatedAt = timeMillis
                    )
                }
                sessionDao.insertSessions(items)
                Result.success(items.map { it.toDomain() })
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshMessages(sessionId: String): Result<List<MessageItem>> {
        return try {
            val response = apiService.getSessionMessages(sessionId)
            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.data.map { apiMsg ->
                    val textContent = when (apiMsg.type) {
                        "user" -> apiMsg.text.orEmpty()
                        "assistant" -> apiMsg.content?.filter { it.type == "text" }?.joinToString("\n\n") { it.text.orEmpty() }.orEmpty()
                        else -> ""
                    }
                    val tools = apiMsg.content?.filter { it.type == "tool" }?.map { part ->
                        val inputMap = part.input ?: emptyMap()
                        val filePath = inputMap["path"]?.toString()
                            ?: inputMap["file"]?.toString()
                            ?: inputMap["filePath"]?.toString()
                        ToolInvocation(
                            id = part.callID ?: part.tool ?: "tool_${System.currentTimeMillis()}",
                            name = part.tool ?: "tool",
                            targetFile = filePath?.let { File(it).name } ?: "",
                            oldString = inputMap["oldString"]?.toString(),
                            newString = inputMap["newString"]?.toString(),
                            path = filePath,
                            result = part.output,
                            isFailed = !part.error.isNullOrBlank()
                        )
                    } ?: emptyList()

                    MessageEntity(
                        id = apiMsg.id,
                        sessionId = sessionId,
                        role = apiMsg.type.uppercase(),
                        content = textContent,
                        toolsJson = if (tools.isNotEmpty()) gson.toJson(tools) else "",
                        sourcesJson = "",
                        timestamp = apiMsg.time?.created ?: System.currentTimeMillis()
                    )
                }
                messageDao.insertMessages(entities)
                Result.success(entities.map { it.toDomain(gson) })
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createNewSession(title: String, projectId: String?): Result<SessionSummary> {
        return try {
            val response = apiService.createSession(ApiCreateSessionRequest(title, projectId))
            if (response.isSuccessful && response.body() != null) {
                val apiSession = response.body()!!.data
                val entity = SessionEntity(
                    id = apiSession.id,
                    title = apiSession.title ?: title,
                    project = apiSession.projectID ?: projectId.orEmpty(),
                    snippet = "",
                    timeAgo = "just now",
                    isUnread = false,
                    updatedAt = System.currentTimeMillis()
                )
                sessionDao.insertSession(entity)
                Result.success(entity.toDomain())
            } else {
                val localId = "ses_local_${System.currentTimeMillis()}"
                val localEntity = SessionEntity(
                    id = localId,
                    title = title,
                    project = projectId.orEmpty(),
                    snippet = "",
                    timeAgo = "just now",
                    isUnread = false,
                    updatedAt = System.currentTimeMillis()
                )
                sessionDao.insertSession(localEntity)
                Result.success(localEntity.toDomain())
            }
        } catch (e: Exception) {
            val localId = "ses_local_${System.currentTimeMillis()}"
            val localEntity = SessionEntity(
                id = localId,
                title = title,
                project = projectId.orEmpty(),
                snippet = "",
                timeAgo = "just now",
                isUnread = false,
                updatedAt = System.currentTimeMillis()
            )
            sessionDao.insertSession(localEntity)
            Result.success(localEntity.toDomain())
        }
    }

    suspend fun sendMessage(sessionId: String, text: String): Result<Unit> {
        val userMsgId = "msg_user_${System.currentTimeMillis()}"
        val userMsg = MessageEntity(
            id = userMsgId,
            sessionId = sessionId,
            role = "USER",
            content = text,
            timestamp = System.currentTimeMillis()
        )
        messageDao.insertMessage(userMsg)

        return try {
            val response = apiService.sendPrompt(sessionId, ApiPromptRequest(text = text))
            if (response.isSuccessful) {
                refreshMessages(sessionId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send message: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameSession(sessionId: String, newTitle: String): Result<Unit> {
        sessionDao.updateTitle(sessionId, newTitle)
        return try {
            apiService.renameSession(sessionId, ApiRenameSessionRequest(newTitle))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    suspend fun togglePinSession(sessionId: String, currentPin: Boolean): Result<Unit> {
        sessionDao.updatePinned(sessionId, !currentPin)
        return Result.success(Unit)
    }

    suspend fun markSessionAsUnread(sessionId: String, unread: Boolean): Result<Unit> {
        sessionDao.updateUnread(sessionId, unread)
        return Result.success(Unit)
    }

    suspend fun deleteSession(sessionId: String): Result<Unit> {
        sessionDao.deleteSession(sessionId)
        messageDao.deleteMessagesForSession(sessionId)
        try {
            apiService.deleteSession(sessionId)
        } catch (_: Exception) { }
        return Result.success(Unit)
    }

    suspend fun compactSession(sessionId: String): Result<Unit> {
        return try {
            val res = apiService.compactSession(sessionId)
            if (res.isSuccessful) {
                refreshMessages(sessionId)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Compact request failed: ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forkSession(sessionId: String): Result<SessionSummary> {
        return try {
            val response = apiService.forkSession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                val apiSession = response.body()!!.data
                val forked = SessionEntity(
                    id = apiSession.id,
                    title = apiSession.title ?: "Forked Session",
                    project = apiSession.projectID ?: "",
                    snippet = "",
                    timeAgo = "just now",
                    isUnread = false,
                    updatedAt = System.currentTimeMillis()
                )
                sessionDao.insertSession(forked)
                Result.success(forked.toDomain())
            } else {
                Result.failure(Exception("Failed to fork session: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportSession(sessionId: String): Result<String> {
        return try {
            val response = apiService.exportSession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                val jsonString = gson.toJson(response.body())
                Result.success(jsonString)
            } else {
                // Local export fallback from Room DB
                val session = sessionDao.getSessionById(sessionId)
                val messages = messageDao.getMessagesForSessionList(sessionId)
                val exportMap = mapOf(
                    "session" to session,
                    "messages" to messages
                )
                Result.success(gson.toJson(exportMap))
            }
        } catch (e: Exception) {
            val session = sessionDao.getSessionById(sessionId)
            val messages = messageDao.getMessagesForSessionList(sessionId)
            val exportMap = mapOf(
                "session" to session,
                "messages" to messages
            )
            Result.success(gson.toJson(exportMap))
        }
    }

    suspend fun getAvailableModels(): Result<List<ModelItem>> {
        return try {
            val response = apiService.getModels()
            if (response.isSuccessful && response.body() != null) {
                val favs = preferencesManager.favoriteModels.firstOrNull() ?: emptySet()
                val recents = preferencesManager.recentModels.firstOrNull() ?: emptyList()
                val selected = preferencesManager.selectedModel.firstOrNull().orEmpty()

                val models = response.body()!!.data.map { apiModel ->
                    val variant = apiModel.variants?.firstOrNull()?.id ?: "Default"
                    ModelItem(
                        id = apiModel.id,
                        name = apiModel.name ?: apiModel.id,
                        provider = apiModel.providerID,
                        variant = variant,
                        isFavorite = favs.contains(apiModel.id),
                        isRecent = recents.contains(apiModel.id),
                        isSelected = apiModel.id == selected || (selected.isEmpty() && response.body()!!.data.firstOrNull()?.id == apiModel.id)
                    )
                }
                if (selected.isEmpty() && models.isNotEmpty()) {
                    preferencesManager.setSelectedModel(models.first().name)
                }
                Result.success(models)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSlashCommands(): Result<List<SlashCommand>> {
        return try {
            val response = apiService.getCommands()
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.data.map { cmd ->
                    val cmdName = if (cmd.name.startsWith("/")) cmd.name else "/${cmd.name}"
                    SlashCommand(
                        command = cmdName,
                        description = cmd.description ?: ""
                    )
                }
                Result.success(list)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchFiles(query: String): Result<List<MentionItem>> {
        return try {
            val response = apiService.findFiles(query)
            if (response.isSuccessful && response.body() != null) {
                val items = response.body()!!.data.map { fileEntry ->
                    val file = File(fileEntry.path)
                    val parent = file.parentFile?.name ?: ""
                    MentionItem(
                        id = fileEntry.path,
                        title = file.name,
                        subtitle = parent,
                        isFile = fileEntry.type == "file"
                    )
                }
                Result.success(items)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatTimeAgo(timeMillis: Long): String {
        if (timeMillis <= 0L) return ""
        val diff = System.currentTimeMillis() - timeMillis
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "just now"
        }
    }

    private fun SessionEntity.toDomain() = SessionSummary(
        id = id,
        title = title,
        project = project,
        snippet = snippet,
        timeAgo = timeAgo,
        isUnread = isUnread,
        isPinned = isPinned,
        isArchived = isArchived,
        updatedAt = updatedAt
    )

    private fun MessageEntity.toDomain(gson: Gson): MessageItem {
        val toolList = if (toolsJson.isNotBlank()) {
            try {
                gson.fromJson(toolsJson, Array<ToolInvocation>::class.java).toList()
            } catch (_: Exception) { emptyList() }
        } else emptyList()

        return MessageItem(
            id = id,
            sessionId = sessionId,
            role = when (role.uppercase()) {
                "USER" -> MessageRole.USER
                "SYSTEM" -> MessageRole.SYSTEM
                else -> MessageRole.ASSISTANT
            },
            content = content,
            timestamp = timestamp,
            tools = toolList,
            sources = emptyList()
        )
    }
}
