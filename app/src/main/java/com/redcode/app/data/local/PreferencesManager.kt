package com.redcode.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.redcode.app.data.model.ExperimentalSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "redcode_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    companion object {
        val KEY_SETUP_COMPLETED = booleanPreferencesKey("setup_completed")
        val KEY_SERVER_PORT = intPreferencesKey("server_port")
        val KEY_SERVER_HOST = stringPreferencesKey("server_host")
        val KEY_SERVER_PASSWORD = stringPreferencesKey("server_password")
        val KEY_SELECTED_PROJECT_ID = stringPreferencesKey("selected_project_id")
        val KEY_SELECTED_PROJECT_NAME = stringPreferencesKey("selected_project_name")
        val KEY_SELECTED_MODEL = stringPreferencesKey("selected_model")
        val KEY_SELECTED_MODE = stringPreferencesKey("selected_mode")
        val KEY_FAVORITE_MODELS = stringPreferencesKey("favorite_models")
        val KEY_RECENT_MODELS = stringPreferencesKey("recent_models")
        val KEY_EXPERIMENTAL_SETTINGS = stringPreferencesKey("experimental_settings")
        val KEY_SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    }

    val isSetupCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SETUP_COMPLETED] ?: false
    }

    val serverPort: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_PORT] ?: 49374
    }

    val serverHost: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_HOST] ?: "127.0.0.1"
    }

    val serverPassword: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_PASSWORD] ?: ""
    }

    val selectedProjectId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_PROJECT_ID] ?: ""
    }

    val selectedProjectName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_PROJECT_NAME] ?: ""
    }

    val selectedModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_MODEL] ?: ""
    }

    val selectedMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_MODE] ?: "Build"
    }

    val favoriteModels: Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_FAVORITE_MODELS]
        if (!raw.isNullOrBlank()) {
            try {
                gson.fromJson(raw, Array<String>::class.java).toSet()
            } catch (_: Exception) { emptySet() }
        } else emptySet()
    }

    val recentModels: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_RECENT_MODELS]
        if (!raw.isNullOrBlank()) {
            try {
                gson.fromJson(raw, Array<String>::class.java).toList()
            } catch (_: Exception) { emptyList() }
        } else emptyList()
    }

    val experimentalSettings: Flow<ExperimentalSettings> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_EXPERIMENTAL_SETTINGS]
        if (!json.isNullOrBlank()) {
            try {
                gson.fromJson(json, ExperimentalSettings::class.java)
            } catch (e: Exception) {
                ExperimentalSettings()
            }
        } else {
            ExperimentalSettings()
        }
    }

    val selectedLanguage: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_LANGUAGE] ?: "System Default"
    }

    suspend fun setSelectedLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_LANGUAGE] = language
        }
    }

    suspend fun setSetupCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SETUP_COMPLETED] = completed
        }
    }

    suspend fun setServerCredentials(port: Int, host: String, password: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_PORT] = port
            prefs[KEY_SERVER_HOST] = host
            prefs[KEY_SERVER_PASSWORD] = password
        }
    }

    suspend fun setSelectedProject(id: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_PROJECT_ID] = id
            prefs[KEY_SELECTED_PROJECT_NAME] = name
        }
    }

    suspend fun setSelectedModel(model: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODEL] = model
        }
    }

    suspend fun setSelectedMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_MODE] = mode
        }
    }

    suspend fun toggleFavoriteModel(modelId: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_FAVORITE_MODELS]
            val set = if (!raw.isNullOrBlank()) {
                try {
                    gson.fromJson(raw, Array<String>::class.java).toMutableSet()
                } catch (_: Exception) { mutableSetOf() }
            } else mutableSetOf()

            if (set.contains(modelId)) set.remove(modelId) else set.add(modelId)
            prefs[KEY_FAVORITE_MODELS] = gson.toJson(set)
        }
    }

    suspend fun addRecentModel(modelId: String) {
        context.dataStore.edit { prefs ->
            val raw = prefs[KEY_RECENT_MODELS]
            val list = if (!raw.isNullOrBlank()) {
                try {
                    gson.fromJson(raw, Array<String>::class.java).toMutableList()
                } catch (_: Exception) { mutableListOf() }
            } else mutableListOf()

            list.remove(modelId)
            list.add(0, modelId)
            val trimmed = list.take(10)
            prefs[KEY_RECENT_MODELS] = gson.toJson(trimmed)
        }
    }

    suspend fun setExperimentalSettings(settings: ExperimentalSettings) {
        context.dataStore.edit { prefs ->
            prefs[KEY_EXPERIMENTAL_SETTINGS] = gson.toJson(settings)
        }
    }
}
