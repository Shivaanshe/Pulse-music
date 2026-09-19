package com.example.song.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.cacheDataStore: DataStore<Preferences> by preferencesDataStore(name = "cache_preferences")

class CachePreferences(private val context: Context) {

    companion object {
        private val KEY_SONG_CACHE_SIZE_MB = intPreferencesKey("song_cache_size_mb")
        const val DEFAULT_SONG_CACHE_MB = 300
        const val MIN_SONG_CACHE_MB = 100
        const val SAFETY_BUFFER_MB = 1024 // 1 GB safety buffer
    }

    val songCacheSizeMb: Flow<Int> = context.cacheDataStore.data.map { preferences ->
        preferences[KEY_SONG_CACHE_SIZE_MB] ?: DEFAULT_SONG_CACHE_MB
    }

    suspend fun setSongCacheSizeMb(sizeMb: Int) {
        val validated = sizeMb.coerceAtLeast(MIN_SONG_CACHE_MB)
        context.cacheDataStore.edit { preferences ->
            preferences[KEY_SONG_CACHE_SIZE_MB] = validated
        }
    }
}
