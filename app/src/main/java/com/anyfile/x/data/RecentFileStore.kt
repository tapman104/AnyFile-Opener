package com.anyfile.x.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.recentFilesDataStore by preferencesDataStore(name = "recent_files")

object RecentFileStore {
    private val RECENT_FILES_KEY = stringPreferencesKey("recent_files_list")

    fun getRecentFiles(context: Context): Flow<List<RecentFile>> {
        return context.recentFilesDataStore.data.map { preferences ->
            val json = preferences[RECENT_FILES_KEY] ?: "[]"
            try {
                Json.decodeFromString<List<RecentFile>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun addRecentFile(context: Context, file: RecentFile) {
        context.recentFilesDataStore.edit { preferences ->
            val currentJson = preferences[RECENT_FILES_KEY] ?: "[]"
            val currentList = try {
                Json.decodeFromString<List<RecentFile>>(currentJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            // Remove if already exists (to bump to top)
            currentList.removeAll { it.uri == file.uri }
            currentList.add(0, file)

            // Limit to 10
            val limitedList = currentList.take(10)
            preferences[RECENT_FILES_KEY] = Json.encodeToString(limitedList)
        }
    }
}
