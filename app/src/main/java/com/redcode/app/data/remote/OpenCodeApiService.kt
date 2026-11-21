package com.redcode.app.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenCodeApiService {

    @GET("/api/info")
    suspend fun getServerInfo(): Response<ApiServerInfo>

    @GET("/global/health")
    suspend fun getGlobalHealth(): Response<Map<String, Any>>

    @GET("/api/project")
    suspend fun getProjects(): Response<List<ApiProjectItem>>

    @GET("/api/session")
    suspend fun getSessions(): Response<ApiSessionListResponse>

    @POST("/api/session")
    suspend fun createSession(@Body body: ApiCreateSessionRequest): Response<ApiSessionDetailResponse>

    @GET("/api/session/{id}")
    suspend fun getSessionDetail(@Path("id") sessionId: String): Response<ApiSessionDetailResponse>

    @DELETE("/api/session/{id}")
    suspend fun deleteSession(@Path("id") sessionId: String): Response<Unit>

    @PATCH("/api/session/{id}")
    suspend fun renameSession(@Path("id") sessionId: String, @Body body: ApiRenameSessionRequest): Response<Unit>

    @POST("/api/session/{id}/fork")
    suspend fun forkSession(@Path("id") sessionId: String): Response<ApiSessionDetailResponse>

    @POST("/api/session/{id}/compact")
    suspend fun compactSession(@Path("id") sessionId: String): Response<Unit>

    @GET("/api/experimental/session/{id}/export")
    suspend fun exportSession(@Path("id") sessionId: String): Response<Map<String, Any>>

    @GET("/api/session/{id}/message")
    suspend fun getSessionMessages(@Path("id") sessionId: String): Response<ApiMessageListResponse>

    @POST("/api/session/{id}/prompt")
    suspend fun sendPrompt(@Path("id") sessionId: String, @Body body: ApiPromptRequest): Response<ApiPromptResponse>

    @GET("/api/model")
    suspend fun getModels(): Response<ApiModelListResponse>

    @GET("/api/provider")
    suspend fun getProviders(): Response<ApiProviderListResponse>

    @GET("/api/command")
    suspend fun getCommands(): Response<ApiCommandListResponse>

    @GET("/api/fs/find")
    suspend fun findFiles(@Query("query") query: String): Response<ApiFileListResponse>
}

// ── Models ──────────────────────────────────────────────────────────────────

data class ApiServerInfo(
    @SerializedName("version") val version: String,
    @SerializedName("pid") val pid: Int,
    @SerializedName("hostname") val hostname: String? = null,
    @SerializedName("urls") val urls: List<String> = emptyList(),
    @SerializedName("paths") val paths: Map<String, String> = emptyMap()
)

data class ApiProjectItem(
    @SerializedName("id") val id: String,
    @SerializedName("canonical") val canonical: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("vcs") val vcs: String? = null,
    @SerializedName("time") val time: ApiTime? = null
)

data class ApiTime(
    @SerializedName("created") val created: Long = 0L,
    @SerializedName("updated") val updated: Long = 0L,
    @SerializedName("active") val active: Long = 0L,
    @SerializedName("archived") val archived: Long? = null
)

data class ApiSessionListResponse(
    @SerializedName("data") val data: List<ApiSessionSummary> = emptyList(),
    @SerializedName("cursor") val cursor: Map<String, Any>? = null
)

data class ApiSessionSummary(
    @SerializedName("id") val id: String,
    @SerializedName("projectID") val projectID: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("time") val time: ApiTime? = null,
    @SerializedName("agent") val agent: String? = null,
    @SerializedName("model") val model: ApiSessionModel? = null
)

data class ApiSessionModel(
    @SerializedName("id") val id: String? = null,
    @SerializedName("providerID") val providerID: String? = null,
    @SerializedName("variant") val variant: String? = null
)

data class ApiSessionDetailResponse(
    @SerializedName("data") val data: ApiSessionSummary
)

data class ApiCreateSessionRequest(
    @SerializedName("title") val title: String,
    @SerializedName("projectID") val projectID: String? = null
)

data class ApiRenameSessionRequest(
    @SerializedName("title") val title: String
)

data class ApiPromptRequest(
    @SerializedName("text") val text: String,
    @SerializedName("files") val files: List<Map<String, Any>>? = null
)

data class ApiPromptResponse(
    @SerializedName("data") val data: Map<String, Any>? = null
)

data class ApiMessageListResponse(
    @SerializedName("data") val data: List<ApiMessageEntry> = emptyList(),
    @SerializedName("cursor") val cursor: Map<String, Any>? = null
)

data class ApiMessageEntry(
    @SerializedName("id") val id: String,
    @SerializedName("type") val type: String, // "user", "assistant", "system", etc.
    @SerializedName("text") val text: String? = null, // for type "user"
    @SerializedName("content") val content: List<ApiContentPart>? = null, // for type "assistant"
    @SerializedName("time") val time: ApiMessageTime? = null,
    @SerializedName("agent") val agent: String? = null,
    @SerializedName("model") val model: ApiSessionModel? = null
)

data class ApiMessageTime(
    @SerializedName("created") val created: Long = 0L,
    @SerializedName("completed") val completed: Long? = null
)

data class ApiContentPart(
    @SerializedName("type") val type: String, // "text", "tool", "source"
    @SerializedName("text") val text: String? = null,
    @SerializedName("tool") val tool: String? = null,
    @SerializedName("callID") val callID: String? = null,
    @SerializedName("input") val input: Map<String, Any>? = null,
    @SerializedName("output") val output: String? = null,
    @SerializedName("error") val error: String? = null
)

data class ApiModelListResponse(
    @SerializedName("data") val data: List<ApiModelItem> = emptyList()
)

data class ApiModelItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("providerID") val providerID: String,
    @SerializedName("variants") val variants: List<ApiModelVariant>? = null
)

data class ApiModelVariant(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null
)

data class ApiProviderListResponse(
    @SerializedName("data") val data: List<ApiProviderItem> = emptyList()
)

data class ApiProviderItem(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String? = null
)

data class ApiCommandListResponse(
    @SerializedName("data") val data: List<ApiCommandItem> = emptyList()
)

data class ApiCommandItem(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String? = null
)

data class ApiFileListResponse(
    @SerializedName("data") val data: List<ApiFileItem> = emptyList()
)

data class ApiFileItem(
    @SerializedName("path") val path: String,
    @SerializedName("type") val type: String // "file", "directory"
)
